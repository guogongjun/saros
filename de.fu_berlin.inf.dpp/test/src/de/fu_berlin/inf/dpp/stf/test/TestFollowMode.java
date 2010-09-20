package de.fu_berlin.inf.dpp.stf.test;

import static org.junit.Assert.assertTrue;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.BotConfiguration;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.Musician;
import de.fu_berlin.inf.dpp.stf.sarosswtbot.SarosConstant;

public class TestFollowMode {
    private static final String CLS1 = BotConfiguration.CLASSNAME;
    private static final String CLS2 = BotConfiguration.CLASSNAME2;
    private static final String CLS3 = BotConfiguration.CLASSNAME3;
    private static final String PKG = BotConfiguration.PACKAGENAME;
    private static final String PROJECT = BotConfiguration.PROJECTNAME;

    // bots
    protected static Musician alice;
    protected static Musician bob;

    @BeforeClass
    public static void configureInvitee() throws RemoteException,
        NotBoundException {

        alice = new Musician(new JID(BotConfiguration.JID_ALICE),
            BotConfiguration.PASSWORD_ALICE, BotConfiguration.HOST_ALICE,
            BotConfiguration.PORT_ALICE);
        alice.initBot();
        alice.newProjectWithClass(PROJECT, PKG, CLS1);

        bob = new Musician(new JID(BotConfiguration.JID_BOB),
            BotConfiguration.PASSWORD_BOB, BotConfiguration.HOST_BOB,
            BotConfiguration.PORT_BOB);
        bob.initBot();

        alice.buildSession(bob, PROJECT,
            SarosConstant.CONTEXT_MENU_SHARE_PROJECT,
            SarosConstant.CREATE_NEW_PROJECT);

    }

    @AfterClass
    public static void cleanupBob() throws RemoteException {
        bob.xmppDisconnect();
        bob.deleteProject(PROJECT);
    }

    @AfterClass
    public static void cleanupAlice() throws RemoteException {
        alice.xmppDisconnect();
        alice.deleteProject(PROJECT);
    }

    @After
    public void StopFollowMode() throws RemoteException {
        if (bob.isInFollowMode(alice))
            bob.clickCMStopfollowingThisUserInSPSView(alice);
        if (alice.isInFollowMode(bob))
            alice.clickCMStopfollowingThisUserInSPSView(bob);

    }

    @Test
    public void testBobFollowAlice() throws RemoteException {
        alice.setTextInJavaEditor(BotConfiguration.CONTENTPATH, PROJECT, PKG,
            CLS1);
        bob.followUser(alice);
        bob.waitUntilJavaEditorActive(CLS1);
        assertTrue(bob.isInFollowMode(alice));
        assertTrue(bob.isJavaEditorActive(CLS1));

        String textFromInviter = alice.getTextOfJavaEditor(PROJECT, PKG, CLS1);
        bob.waitUntilFileEqualWithFile(PROJECT, PKG, CLS1, textFromInviter);
        String textFormInvitee = bob.getTextOfJavaEditor(PROJECT, PKG, CLS1);

        assertTrue(textFromInviter.equals(textFormInvitee));

        alice.newJavaClassInProject(PROJECT, PKG, CLS2);
        bob.waitUntilJavaEditorActive(CLS2);
        assertTrue(bob.isJavaEditorActive(CLS2));

        // TODO make tests independent of each other
        // }
        //
        // @Test
        // public void testAliceFollowBob() throws RemoteException {
        alice.followUser(bob);
        bob.activateJavaEditor(CLS1);
        alice.waitUntilJavaEditorActive(CLS1);
        assertTrue(alice.isInFollowMode(bob));
        assertTrue(alice.isJavaEditorActive(CLS1));

        // TODO make tests independent of each other
        // }
        //
        // @Test
        // public void testBobFollowAliceWithDebugging() throws RemoteException
        // {

        bob.followUser(alice);
        alice.newJavaClassInProject(PROJECT, PKG, CLS3);
        alice.waitUntilJavaEditorActive(CLS3);
        alice.setTextInJavaEditor(BotConfiguration.CONTENTPATH3, PROJECT, PKG,
            CLS3);
        alice.setBreakPoint(13, PROJECT, PKG, CLS3);
        // alice.debugJavaFile(BotConfiguration.PROJECTNAME,
        // BotConfiguration.PACKAGENAME, BotConfiguration.CLASSNAME3);
        // bob.waitUntilJavaEditorActive(BotConfiguration.CLASSNAME3);
        // assertFalse(bob.isDebugPerspectiveActive());
        // alice.openJavaPerspective();
        // bob.sleep(1000);
        // int lineFromAlice = alice.getJavaCursorLinePosition(
        // BotConfiguration.PROJECTNAME, BotConfiguration.PACKAGENAME,
        // BotConfiguration.CLASSNAME3);
        // int lineFromBob = bob.getJavaCursorLinePosition(
        // BotConfiguration.PROJECTNAME, BotConfiguration.PACKAGENAME,
        // BotConfiguration.CLASSNAME3);
        // assertEquals(lineFromAlice, lineFromBob);
        // alice.waitUntilShellActive("Confirm Perspective Switch");
        // assertTrue(alice.isShellActive("Confirm Perspective Switch"));
    }
}
