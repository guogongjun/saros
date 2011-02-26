package de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.invitation;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.STFTest;

public class TestEditDuringInvitation extends STFTest {

    /**
     * Preconditions:
     * <ol>
     * <li>alice (Host, Write Access), alice share a java project with bob and
     * carl.</li>
     * <li>bob (Read-Only Access)</li>
     * <li>carl (Read-Only Access)</li>
     * </ol>
     * 
     * @throws RemoteException
     * 
     */
    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(TypeOfTester.ALICE, TypeOfTester.BOB, TypeOfTester.CARL);
        setUpWorkbench();
        setUpSaros();
        alice.sarosBot().file().newJavaProjectWithClasses(PROJECT1, PKG1, CLS1);
    }

    /**
     * 
     * Steps:
     * <ol>
     * <li>Alice invites Bob.</li>
     * <li>Bob accepts the invitation</li>
     * <li>Alice invites Carl</li>
     * <li>Bob changes data during the running invtiation of Carl.</li>
     * </ol>
     * 
     * 
     * Expected Results:
     * <ol>
     * <li>All changes that Bob has done should be on Carl's side. There should
     * not be an inconsistency.</li>.
     * </ol>
     * 
     * @throws RemoteException
     */
    @Test
    public void testEditDuringInvitation() throws RemoteException {
        buildSessionSequentially(VIEW_PACKAGE_EXPLORER, PROJECT1,
            TypeOfShareProject.SHARE_PROJECT, TypeOfCreateProject.NEW_PROJECT,
            alice, bob);

        assertTrue(bob.sarosBot().sessionView().hasWriteAccessNoGUI());

        alice.sarosBot().sessionView()
            .openInvitationInterface(carl.getBaseJid());
        carl.sarosBot().packageExplorerView().saros()
            .confirmShellSessionnInvitation();

        bob.sarosBot().packageExplorerView().selectClass(PROJECT1, PKG1, CLS1)
            .contextMenu(CM_OPEN).click();
        bob.bot().editor(CLS1_SUFFIX).setTexWithSave(CP1);
        String texByBob = bob.bot().editor(CLS1_SUFFIX).getText();
        // System.out.println(texByBob);

        carl.sarosBot().packageExplorerView().saros()
            .confirmShellAddProjectWithNewProject(PROJECT1);
        carl.sarosBot().packageExplorerView().selectClass(PROJECT1, PKG1, CLS1)
            .contextMenu(CM_OPEN).click();

        alice.bot().editor(CLS1_SUFFIX).waitUntilIsTextSame(texByBob);
        String textByAlice = alice.bot().editor(CLS1_SUFFIX).getText();

        // There are bugs here, carl get completely different content as bob.
        carl.bot().editor(CLS1_SUFFIX).waitUntilIsTextSame(texByBob);
        String textByCarl = carl.bot().editor(CLS1_SUFFIX).getText();
        System.out.println(textByCarl);

        assertTrue(textByCarl.equals(texByBob));
        assertTrue(textByAlice.equals(texByBob));
    }
}
