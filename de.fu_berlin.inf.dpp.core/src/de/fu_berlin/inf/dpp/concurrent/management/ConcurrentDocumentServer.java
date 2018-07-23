package de.fu_berlin.inf.dpp.concurrent.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.picocontainer.Startable;

import de.fu_berlin.inf.dpp.activities.AbstractActivityReceiver;
import de.fu_berlin.inf.dpp.activities.ChecksumActivity;
import de.fu_berlin.inf.dpp.activities.FileActivity;
import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.IActivityReceiver;
import de.fu_berlin.inf.dpp.activities.JupiterActivity;
import de.fu_berlin.inf.dpp.activities.QueueItem;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.concurrent.jupiter.TransformationException;
import de.fu_berlin.inf.dpp.session.AbstractSessionListener;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISessionListener;
import de.fu_berlin.inf.dpp.session.User;

/**
 * The ConcurrentDocumentServer is responsible for coordinating all
 * JupiterActivities.
 * 
 * All clients (including the host!) will send their JupiterActivities to the
 * ConcurrentDocumentServer on the host, which transforms them (using Jupiter)
 * and then sends them to everybody else.
 * 
 * A ConcurrentDocumentServer exists only on the host!
 */
public class ConcurrentDocumentServer implements Startable {

    private static Logger LOG = Logger
        .getLogger(ConcurrentDocumentServer.class);

    private final ISarosSession sarosSession;

    private final JupiterServer server;

    /**
     * {@link ISessionListener} for updating Jupiter documents on the host.
     */
    private final ISessionListener sessionListener = new AbstractSessionListener() {

        @Override
        public void userStartedQueuing(final User user) {
            server.addUser(user);
        }

        @Override
        public void userLeft(final User user) {
            server.removeUser(user);
        }
    };

    public ConcurrentDocumentServer(final ISarosSession sarosSession) {
        this.sarosSession = sarosSession;
        this.server = new JupiterServer(sarosSession);
    }

    @Override
    public void start() {
        sarosSession.addListener(sessionListener);
    }

    @Override
    public void stop() {
        sarosSession.removeListener(sessionListener);
    }

    /**
     * Dispatched the activity to the internal ActivityReceiver. The
     * ActivityReceiver will remove FileDocuments when the file has been
     * deleted.
     * 
     * @param activity
     *            Activity to be dispatched
     */
    public void checkFileDeleted(final IActivity activity) {
        activity.dispatch(hostReceiver);
    }

    private final IActivityReceiver hostReceiver = new AbstractActivityReceiver() {
        @Override
        public void receive(final FileActivity activity) {
            if (activity.getType() == FileActivity.Type.REMOVED) {
                server.removePath(activity.getPath());
            }
        }
    };

    /**
     * Transforms the given activities on the server side and returns a list of
     * QueueItems containing the transformed activities and there receivers.
     * 
     * @host
     * 
     * @sarosThread Must be executed in the Saros dispatch thread.
     * 
     * @notGUI This method may not be called from SWT, otherwise a deadlock
     *         might occur!!
     * 
     * @param activity
     *            Activity to be transformed
     * 
     * @return A list of QueueItems containing the activities and receivers
     */
    public List<QueueItem> transformIncoming(final IActivity activity) {

        assert sarosSession.isHost() : "CDS.transformIncoming must not be called on the client";

        // assert !isGUI() :
        // "CDS.transformIncoming must not be called from SWT";

        final List<QueueItem> result = new ArrayList<QueueItem>();

        try {
            activity.dispatch(hostReceiver);

            if (activity instanceof JupiterActivity) {
                result.addAll(receive((JupiterActivity) activity));

            } else if (activity instanceof ChecksumActivity) {
                result.addAll(withTimestamp((ChecksumActivity) activity));
            }
        } catch (Exception e) {
            LOG.error("failed to transform jupiter activity: " + activity, e);
        }

        return result;
    }

    /**
     * Does the actual work of transforming a clients JupiterActivity into
     * specific JupiterActivities for every client.
     */
    private List<QueueItem> receive(final JupiterActivity activity) {

        final List<QueueItem> result = new ArrayList<QueueItem>();

        // Sync jupiterActivity with jupiter document server
        final Map<User, JupiterActivity> outgoing;

        try {
            outgoing = server.transform(activity);
        } catch (TransformationException e) {
            LOG.error("failed to transform jupiter activity: " + activity, e);
            // TODO this should trigger a consistency check
            return result;
        }

        for (final Entry<User, JupiterActivity> entry : outgoing.entrySet()) {
            final User user = entry.getKey();
            final JupiterActivity transformed = entry.getValue();

            result.add(new QueueItem(user, transformed));
        }
        return result;
    }

    /**
     * Resets the JupiterServer for the given combination and path and user.
     * 
     * When this is called on the host, a call to
     * {@link ConcurrentDocumentClient#reset(SPath)} should be executed at the
     * same time on the side of the given user.
     * 
     * @host
     */
    public synchronized void reset(final User user, final SPath path) {

        assert sarosSession.isHost();

        LOG.debug("resetting jupiter server for user: " + user + ", path: "
            + path);

        server.reset(path, user);
    }

    /**
     * Prohibits adding a {@link User} to Jupiter Documents, for a Set of
     * {@code SPath} and thereby prohibits to be part of Jupiter Activities in
     * these documents. This prevents inconsistencies for files the user is yet
     * unaware of.
     *
     * @param user
     * @param resorcesMissing
     */
    public synchronized void addUserInNegotiation(final User user,
        Set<SPath> resorcesMissing) {
        server.addUserInNegotiation(user, resorcesMissing);
    }

    /**
     * Allows the {@link User} to become part of an existing or future Jupiter
     * Document. This method should be called right after opening a unmodifiable
     * Filestream for sending to this User, to minimize inconsistency
     * possibilities.
     *
     * @param user
     * @param resource
     */
    public synchronized void userInNegotiationReceives(final User user,
        SPath resource) {
        server.userInNegotiationReceives(user, resource);
    }

    /**
     * Does the actual work of transforming a ChecksumActivity.
     */
    private List<QueueItem> withTimestamp(final ChecksumActivity activity) {

        final List<QueueItem> result = new ArrayList<QueueItem>();

        // Timestamp checksumActivity with jupiter document server
        final Map<User, ChecksumActivity> outgoing;

        try {
            outgoing = server.withTimestamp(activity);
        } catch (TransformationException e) {
            LOG.error("failed to transform checksum activity: " + activity, e);
            // TODO this should trigger a consistency check
            return result;
        }

        for (Entry<User, ChecksumActivity> entry : outgoing.entrySet()) {
            final User user = entry.getKey();
            final ChecksumActivity transformed = entry.getValue();

            result.add(new QueueItem(user, transformed));
        }
        return result;
    }
}
