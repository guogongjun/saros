package de.fu_berlin.inf.dpp.negotiation;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;

import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.communication.extensions.StartActivityQueuingResponse;
import de.fu_berlin.inf.dpp.exceptions.LocalCancellationException;
import de.fu_berlin.inf.dpp.exceptions.SarosCancellationException;
import de.fu_berlin.inf.dpp.filesystem.IChecksumCache;
import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;
import de.fu_berlin.inf.dpp.negotiation.NegotiationTools.CancelOption;
import de.fu_berlin.inf.dpp.negotiation.stream.IncomingStreamProtocol;
import de.fu_berlin.inf.dpp.net.IReceiver;
import de.fu_berlin.inf.dpp.net.ITransmitter;
import de.fu_berlin.inf.dpp.net.xmpp.JID;
import de.fu_berlin.inf.dpp.net.xmpp.XMPPConnectionService;
import de.fu_berlin.inf.dpp.observables.FileReplacementInProgressObservable;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.internal.ResourceActivityQueuer;
import de.fu_berlin.inf.dpp.synchronize.StartHandle;

/**
 * Receive shared Projects and display them instant using a stream based
 * solution.
 */
public class InstantIncomingProjectNegotiation extends
    AbstractIncomingProjectNegotiation {

    private static final Logger log = Logger
        .getLogger(InstantIncomingProjectNegotiation.class);

    private ResourceActivityQueuer activityQueuer;
    private StartHandle stoppedLocalEditor;

    public InstantIncomingProjectNegotiation(
        final JID peer, //
        final String negotiationID, //
        final List<ProjectNegotiationData> projectNegotiationData, //
        final ISarosSessionManager sessionManager, //
        final ISarosSession session, //
        final FileReplacementInProgressObservable fileReplacementInProgressObservable, //
        final IWorkspace workspace, //
        final IChecksumCache checksumCache, //
        final XMPPConnectionService connectionService, //
        final ITransmitter transmitter, //
        final IReceiver receiver //
    ) {
        super(peer, TransferType.INSTANT, negotiationID,
            projectNegotiationData, sessionManager, session,
            fileReplacementInProgressObservable, workspace, checksumCache,
            connectionService, transmitter, receiver);
    }

    @Override
    protected void transfer(IProgressMonitor monitor,
        Map<String, IProject> projectMapping, List<FileList> missingFiles)
        throws IOException, SarosCancellationException {

        try {
            stoppedLocalEditor = session.getStopManager().stop(
                session.getLocalUser(), "Read-only while negotiation!");
        } catch (InterruptedException e) {
            log.error("while blocking local editor", e);
            Thread.currentThread().interrupt();
        }

        /*
         * the user who sends this ProjectNegotiation is now responsible for the
         * resources of the contained projects
         */
        for (Entry<String, IProject> entry : projectMapping.entrySet()) {
            final String projectID = entry.getKey();
            final IProject project = entry.getValue();

            session.addProjectMapping(projectID, project);
        }

        /* generate file lists */
        int filesMissing = 0;
        for (FileList list : missingFiles)
            filesMissing += list.getPaths().size();

        Set<SPath> files = new HashSet<SPath>(filesMissing * 2);
        for (final FileList list : missingFiles) {
            IProject project = session.getProject(list.getProjectID());
            for (String file : list.getPaths()) {
                files.add(new SPath(project.getFile(file)));
            }
        }

        /* register resource based queuing */
        awaitActivityQueueingActivation(monitor);
        activityQueuer = new ResourceActivityQueuer(files);
        session.registerQueuingHandler(activityQueuer);

        /* notify host about queuing */
        transmitter.send(ISarosSession.SESSION_CONNECTION_ID, getPeer(), //
            StartActivityQueuingResponse.PROVIDER //
                .create( //
                new StartActivityQueuingResponse(getSessionID(), getID())));

        checkCancellation(CancelOption.NOTIFY_PEER);

        if (filesMissing > 0)
            receiveStream(monitor, filesMissing);
    }

    @Override
    protected void cleanup(IProgressMonitor monitor,
        Map<String, IProject> projectMapping) {

        stoppedLocalEditor.start();

        super.cleanup(monitor, projectMapping);
    }

    private void receiveStream(IProgressMonitor monitor, int fileCount)
        throws SarosCancellationException, IOException {

        String message = "Receiving files from " + getPeer().getName() + "...";
        monitor.beginTask(message, fileCount);
        monitor.subTask("Waiting for Host to start...");

        awaitTransferRequest();

        monitor.subTask("Host is starting to send...");
        log.debug(this + ": Host is starting to send...");

        IncomingFileTransfer transfer = transferListener.getRequest().accept();
        InputStream in = null;
        try {
            in = transfer.recieveFile();

            IncomingStreamProtocol isp;
            isp = new IncomingStreamProtocol(in, session, monitor);
            isp.receiveStream(activityQueuer);
        } catch (XMPPException e) {
            throw new LocalCancellationException(e.getMessage(),
                CancelOption.NOTIFY_PEER);
        } finally {
            IOUtils.closeQuietly(in);
        }

        log.debug(this + ": stream transmission done");
        monitor.done();
    }

}
