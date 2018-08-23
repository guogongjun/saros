package de.fu_berlin.inf.dpp.server.console;

import de.fu_berlin.inf.dpp.filesystem.IProject;
import de.fu_berlin.inf.dpp.filesystem.IWorkspace;
import de.fu_berlin.inf.dpp.server.filesystem.ServerProjectImpl;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.ISarosSessionManager;
import de.fu_berlin.inf.dpp.session.User;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class ShareCommand extends ConsoleCommand {
  private static final Logger LOG = Logger.getLogger(ShareCommand.class);
  private final ISarosSessionManager sessionManager;
  private final IWorkspace workspace;

  public ShareCommand(ISarosSessionManager sessionManager, IWorkspace workspace) {
    this.sessionManager = sessionManager;
    this.workspace = workspace;
  }

  @Override
  public String identifier() {
    return "share";
  }

  @Override
  public String help() {
    return "share <PATH>... - Share projects relative to the workspace with session participants";
  }

  @Override
  public void execute(String command, PrintStream out) {
    ISarosSession session = sessionManager.getSession();

    if (session == null) {
      LOG.error("No Session running, cannot add any resources");
      return;
    }

    try {
      List<IProject> projects =
          Arrays.asList(command.split(" "))
              .stream()
              .skip(1)
              .map(path -> new ServerProjectImpl(this.workspace, path))
              .collect(Collectors.toList());
      for (IProject project : projects) {
        session.addSharedResources(project, project.getName(), null);
      }
      for (User user : session.getRemoteUsers()) {
        sessionManager.startSharingProjects(user.getJID());
      }
    } catch (Exception e) {
      LOG.error("Error sharing resources", e);
    }
  }
}
