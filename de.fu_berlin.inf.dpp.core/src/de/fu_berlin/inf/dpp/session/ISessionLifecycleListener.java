package de.fu_berlin.inf.dpp.session;

import de.fu_berlin.inf.dpp.monitoring.IProgressMonitor;

/**
 * A listener for {@link ISarosSession} life-cycle related events.
 *
 * @author rdjemili
 */
public interface ISessionLifecycleListener {
  /*
   * TODO: remove this method as soon as external components like the
   * whiteboard are maintained in another way (i.e. a component interface)
   */

  /**
   * Is fired after invitation complete but for every peer the host invited. At this state, the
   * session is fully established and confirmed but the outgoing session negotiation job is still
   * running.
   *
   * <p>Can be used by session components to plug their synchronization process in the session
   * negotiation.
   *
   * <p>Implementations must not block for too long, because this blocks the whole invitation
   * process.
   *
   * <p>TODO: remove this method as soon as external components like the whiteboard are maintained
   * in another way (i.e. a component interface)
   *
   * @param session The corresponding session
   * @param monitor the invitation process's monitor to track process and cancellation
   */
  public void postOutgoingInvitationCompleted(
      ISarosSession session, User user, IProgressMonitor monitor);

  /**
   * Is fired when a new session is about to start.
   *
   * @param session the session that is about to start
   */
  public void sessionStarting(ISarosSession session);

  /**
   * Is fired when a new session started.
   *
   * @param session the session that has been started
   */
  public void sessionStarted(ISarosSession session);

  /**
   * Is fired when a session is about to be ended.
   *
   * @param session the session that is about to end <code>null</code>.
   */
  public void sessionEnding(ISarosSession session);

  /**
   * Is fired when a session ended.
   *
   * @param session the session that has been ended
   * @param reason the reason why the session ended
   */
  public void sessionEnded(ISarosSession session, SessionEndReason reason);
}
