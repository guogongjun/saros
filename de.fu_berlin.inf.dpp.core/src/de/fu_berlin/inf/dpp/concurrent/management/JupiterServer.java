package de.fu_berlin.inf.dpp.concurrent.management;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.fu_berlin.inf.dpp.activities.ChecksumActivity;
import de.fu_berlin.inf.dpp.activities.JupiterActivity;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.concurrent.jupiter.TransformationException;
import de.fu_berlin.inf.dpp.concurrent.jupiter.internal.JupiterDocumentServer;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A JupiterServer manages Jupiter server instances for a number of users AND
 * number of paths.
 * 
 * (in contrast to a JupiterDocumentServer which only handles a single path)
 */
public class JupiterServer {

    /**
     * Jupiter server instance documents
     * 
     * @host
     */
    private final HashMap<SPath, JupiterDocumentServer> concurrentDocuments = new HashMap<SPath, JupiterDocumentServer>();

    private final Set<User> currentClients = new HashSet<User>();
    private final HashMap<User, Set<SPath>> userIsMissingRessource = new HashMap<User, Set<SPath>>();

    private final ISarosSession sarosSession;

    public JupiterServer(final ISarosSession sarosSession) {
        this.sarosSession = sarosSession;
    }

    public synchronized void removePath(final SPath path) {
        concurrentDocuments.remove(path);
    }

    public synchronized void addUser(final User user) {
        currentClients.add(user);

        for (final JupiterDocumentServer server : concurrentDocuments.values())
            server.addProxyClient(user);
    }

    public synchronized void removeUser(final User user) {
        currentClients.remove(user);

        for (final JupiterDocumentServer server : concurrentDocuments.values()) {
            server.removeProxyClient(user);
        }
    }

    /**
     * @see ConcurrentDocumentServer#addUserInNegotiation(User, Set)
     */
    public synchronized void addUserInNegotiation(final User user,
        Set<SPath> resMissing) {
        if (!userIsMissingRessource.containsKey(user))
            userIsMissingRessource.put(user, new HashSet<SPath>(resMissing));
        else
            throw new IllegalArgumentException(
                "Only one Negotiation per User simultaneously allowed!");

        for (Map.Entry<SPath, JupiterDocumentServer> entry : concurrentDocuments
            .entrySet()) {
            if (!resMissing.contains(entry.getKey()))
                entry.getValue().addProxyClient(user);
        }
    }

    /**
     * @see ConcurrentDocumentServer#userInNegotiationReceives(User, SPath)
     */
    public synchronized void userInNegotiationReceives(final User user,
        SPath resource) {
        if (!userIsMissingRessource.containsKey(user))
            throw new IllegalArgumentException("User isn't in Negotiation!");

        userIsMissingRessource.get(user).remove(resource);

        if (concurrentDocuments.containsKey(resource))
            concurrentDocuments.get(resource).addProxyClient(user);

        if (userIsMissingRessource.get(user).isEmpty())
            userIsMissingRessource.remove(user);
    }

    /**
     * Retrieves the JupiterDocumentServer for a given path. If no
     * JupiterDocumentServer exists for this path, a new one is created and
     * returned afterwards.
     * 
     * @host
     */
    /*
     * TODO This solution currently just works for partial sharing by
     * coincidence. To really fix the issue we have to expand the
     * SarosSessionMapper to also track the resources and not just the projects
     * that are already shared for every user individually.
     */
    private synchronized JupiterDocumentServer getServer(final SPath path) {

        JupiterDocumentServer docServer = concurrentDocuments.get(path);

        if (docServer == null) {

            docServer = new JupiterDocumentServer(path);

            for (final User client : currentClients) {
                /*
                 * Make sure that we only add clients that already have the
                 * resources in question. Other clients that haven't accepted
                 * the Project yet will be added later.
                 */
                if (userIsMissingRessource.containsKey(client)
                    && userIsMissingRessource.get(client).contains(path))
                    continue;

                if (sarosSession.userHasProject(client, path.getProject())) {
                    docServer.addProxyClient(client);
                }
            }

            docServer.addProxyClient(sarosSession.getHost());

            concurrentDocuments.put(path, docServer);
        }
        return docServer;
    }

    public synchronized void reset(final SPath path, final User user) {
        getServer(path).reset(user);
    }

    public synchronized Map<User, JupiterActivity> transform(
        final JupiterActivity activity) throws TransformationException {

        final JupiterDocumentServer docServer = getServer(activity.getPath());

        return docServer.transformJupiterActivity(activity);
    }

    public synchronized Map<User, ChecksumActivity> withTimestamp(
        final ChecksumActivity activity) throws TransformationException {

        final JupiterDocumentServer docServer = getServer(activity.getPath());

        return docServer.withTimestamp(activity);
    }

}
