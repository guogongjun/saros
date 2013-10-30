package de.fu_berlin.inf.dpp.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.concurrent.watchdog.ConsistencyWatchdogClient;
import de.fu_berlin.inf.dpp.concurrent.watchdog.IsInconsistentObservable;
import de.fu_berlin.inf.dpp.editor.internal.EditorAPI;
import de.fu_berlin.inf.dpp.project.AbstractSarosSessionListener;
import de.fu_berlin.inf.dpp.project.ISarosSession;
import de.fu_berlin.inf.dpp.project.ISarosSessionManager;
import de.fu_berlin.inf.dpp.ui.ImageManager;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.util.SWTUtils;
import de.fu_berlin.inf.dpp.ui.views.SarosView;
import de.fu_berlin.inf.dpp.util.Utils;
import de.fu_berlin.inf.dpp.util.ValueChangeListener;

@Component(module = "action")
public class ConsistencyAction extends Action {

    private static final ImageDescriptor IN_SYNC = ImageManager
        .getImageDescriptor("icons/etool16/in_sync.png"); //$NON-NLS-1$;

    private static final ImageDescriptor OUT_SYNC = ImageManager
        .getImageDescriptor("icons/etool16/out_sync.png"); //$NON-NLS-1$;

    private static final Logger log = Logger.getLogger(ConsistencyAction.class);

    @Inject
    protected ISarosSessionManager sessionManager;

    @Inject
    protected ConsistencyWatchdogClient watchdogClient;

    @Inject
    protected IsInconsistentObservable inconsistentObservable;

    public ConsistencyAction() {

        setImageDescriptor(IN_SYNC);

        setToolTipText(Messages.ConsistencyAction_tooltip_no_inconsistency);

        SarosPluginContext.initComponent(this);

        sessionManager
            .addSarosSessionListener(new AbstractSarosSessionListener() {
                @Override
                public void sessionStarted(ISarosSession newSarosSession) {
                    setSharedProject(newSarosSession);
                }

                @Override
                public void sessionEnded(ISarosSession oldSarosSession) {
                    setSharedProject(null);
                }
            });

        setSharedProject(sessionManager.getSarosSession());
    }

    protected ISarosSession sarosSession;

    private void setSharedProject(ISarosSession newSharedProject) {

        // Unregister from previous project
        if (sarosSession != null) {
            inconsistentObservable.remove(isConsistencyListener);
        }

        sarosSession = newSharedProject;

        if (sarosSession != null)
            setDisabledImageDescriptor(IN_SYNC);
        else
            setDisabledImageDescriptor(null);

        // Register to new project
        if (sarosSession != null) {
            inconsistentObservable.addAndNotify(isConsistencyListener);
        } else {
            setEnabled(false);
        }
    }

    ValueChangeListener<Boolean> isConsistencyListener = new ValueChangeListener<Boolean>() {

        @Override
        public void setValue(Boolean newValue) {

            if (sarosSession.isHost() && newValue == true) {
                log.warn("No inconsistency should ever be reported" //$NON-NLS-1$
                    + " to the host"); //$NON-NLS-1$
                return;
            }
            log.debug("Inconsistency indicator goes: " //$NON-NLS-1$
                + (newValue ? "on" : "off")); //$NON-NLS-1$ //$NON-NLS-2$

            setEnabled(newValue);

            if (!newValue) {
                setToolTipText(Messages.ConsistencyAction_tooltip_no_inconsistency);
                return;
            }

            setImageDescriptor(OUT_SYNC);

            final Set<SPath> paths = new HashSet<SPath>(
                watchdogClient.getPathsWithWrongChecksums());

            SWTUtils.runSafeSWTAsync(log, new Runnable() {
                @Override
                public void run() {

                    String files = Utils.toOSString(paths);

                    // set tooltip
                    setToolTipText(MessageFormat
                        .format(
                            Messages.ConsistencyAction_tooltip_inconsistency_detected,
                            files));

                    // TODO Balloon is too aggressive at the moment, when
                    // the host is slow in sending changes (for instance
                    // when refactoring)

                    // show balloon notification
                    SarosView
                        .showNotification(
                            Messages.ConsistencyAction_title_inconsistency_deteced,
                            MessageFormat
                                .format(
                                    Messages.ConsistencyAction_message_inconsistency_detected,
                                    files));
                }
            });
        }

    };

    @Override
    public void run() {
        SWTUtils.runSafeSWTAsync(log, new Runnable() {

            @Override
            public void run() {
                log.debug("user activated CW recovery."); //$NON-NLS-1$

                Shell dialogShell = EditorAPI.getShell();
                if (dialogShell == null)
                    dialogShell = new Shell();

                ProgressMonitorDialog dialog = new ProgressMonitorDialog(
                    dialogShell);
                try {
                    dialog.run(true, true, new IRunnableWithProgress() {
                        @Override
                        public void run(IProgressMonitor monitor)
                            throws InterruptedException {

                            SubMonitor progress = SubMonitor.convert(monitor);
                            progress
                                .beginTask(
                                    Messages.ConsistencyAction_progress_perform_recovery,
                                    100);
                            watchdogClient.runRecovery(progress.newChild(100));
                            monitor.done();
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error("Exception not expected here.", e); //$NON-NLS-1$
                } catch (InterruptedException e) {
                    log.error("Exception not expected here.", e); //$NON-NLS-1$
                }
            }
        });
    }
}
