/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.accountManagement.XMPPAccount;
import de.fu_berlin.inf.dpp.accountManagement.XMPPAccountStore;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.preferences.PreferenceConstants;
import de.fu_berlin.inf.dpp.ui.wizards.ChangeWizard;
import de.fu_berlin.inf.dpp.ui.wizards.ConfigurationWizard;

/**
 * Contains the basic preferences for Saros.
 * 
 * @author Sebastian Schlaak
 */
@Component(module = "prefs")
public class GeneralPreferencePage extends FieldEditorPreferencePage implements
    IWorkbenchPreferencePage {

    // labels
    public static final String GROUP_ACTIVE_LABEL = "activate";
    public static final String GROUP_DEACTIVE_LABEL = "deactivate";
    public static final int COLUMNS_IN_ACCOUNTGROUP = 2;
    public static final String ACCOUNT_GROUP_TITLE = "XMPP-Accounts";
    public static final String ACTIVATE_BTN_TEXT = "Activate Account";
    public static final String CHANGE_BTN_TEXT = "Change Account";
    public static final String ADD_BTN_TEXT = "Add Account";
    public static final String DELETE_BTN_TEXT = "Delete Account";
    public static final String DELETE_ACTIVE_TEXT = "You cannot delete the active account.";
    public static final String NO_ENTRY_SELECTED_TEXT = "Please select account in list.";
    public static final String STARTUP_CONNECT_TEXT = "Automatically connect on startup";
    public static final String FOLLOW_MODE_TEXT = "Start in Follow Mode.";
    public static final String ROLE_CHANGE_TEXT = "On role changes follow exclusive driver automatically.";
    public static final String MULTI_DRIVER_SUPPORT_TEXT = "Enable multi driver support";
    public static final String CONCURRENT_UNDO_TEXT = "Enable concurrent undo (only local changes are undone, session restart necessary).";
    public static final String DISABLE_VERSION_CONTROL_TEXT = "Disable Version Control support";
    public static final String DISABLE_VERSION_CONTROL_TOOLTIP = "Saros tries to share VCS operations"
        + " like checkout during the invitation, or switch or update during a session. (Currently, only SVN "
        + "is supported.) You can disable VCS support in case you have problems with the repository.\n"
        + "Disabling VCS support during a running session is possible, but enabling VCS support won't have"
        + " any effect until you rejoin the session (restart if you're the host).";

    // icons
    public static final Image ADD_IMAGE = SarosUI
        .getImage("icons/btn/addaccount.png");
    public static final Image ACTIVATE_IMAGE = SarosUI
        .getImage("icons/btn/activateaccount.png");
    public static final Image DELETE_IMAGE = SarosUI
        .getImage("icons/btn/deleteaccount.png");
    public static final Image CHANGE_IMAGE = SarosUI
        .getImage("icons/btn/changeaccount.png");

    @Inject
    Saros saros;
    @Inject
    XMPPAccountStore accountStore;
    Composite parent;
    IPreferenceStore preferenceStore;
    String selectedEntry;
    Label infoLabel;
    Group accountGroup;
    List accountList;
    int selectedEntryId;

    public GeneralPreferencePage() {
        super(FieldEditorPreferencePage.GRID);
        Saros.injectDependenciesOnly(this);
        setPreferenceStore(saros.getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        this.parent = new Composite(getFieldEditorParent(), SWT.NONE);
        layoutParent();
        createAccountsGroup();
        createAutomaticConnectField(this.parent);
        createVersionControlPreferences(this.parent);
        createFollowModePreferences();
        createMultiDriverPreferences();
    }

    /*
     * Creates a grid-layout with one column.
     */
    protected void layoutParent() {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        parent.setLayout(new GridLayout(1, false));
    }

    /*
     * Creates the account-list, buttons and the 'activeUserLabel'.
     */
    protected void createAccountsGroup() {
        accountGroup = createGroupWithGridLayout(COLUMNS_IN_ACCOUNTGROUP,
            ACCOUNT_GROUP_TITLE);
        createAccountList(accountGroup);
        createAccountListControls(accountGroup);
        createInfoLabel(accountGroup);
    }

    protected Group createGroupWithGridLayout(int numColumns, String title) {
        Group accountGroup = new Group(parent, SWT.NONE);
        accountGroup.setLayout(new GridLayout(numColumns, false));
        accountGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
            false));
        accountGroup.setText(title);
        return accountGroup;
    }

    /*
     * Creates a list-component filled with the accounts.
     */
    protected void createAccountList(Composite composite) {

        accountList = new List(composite, SWT.BORDER | SWT.H_SCROLL
            | SWT.V_SCROLL);
        GridData data = new GridData(GridData.FILL_BOTH);
        accountList.setLayoutData(data);
        for (XMPPAccount account : accountStore.getAllAccounts()) {
            accountList.add(account.toString());
        }
        accountList.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                handleEvent();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                handleEvent();
            }

            private void handleEvent() {
                // String entry = "";
                // String[] selection = accountList.getSelection();
                selectedEntryId = accountList.getSelectionIndex();
                /*
                 * for (int i = 0; i < selection.length; i++) entry +=
                 * selection[i]; selectedEntry = entry;
                 */
            }
        });
    }

    /*
     * Creates the buttons: activate, add, change and delete.
     */
    protected void createAccountListControls(Composite parent) {
        Composite controlComposite = createAccountListComposite(parent);
        createActivateBtn(controlComposite);
        createAddAccountBtn(controlComposite);
        createDeleteBtn(controlComposite);
        createChangeBtn(controlComposite);
    }

    protected Composite createAccountListComposite(Composite parent) {
        Composite buildControlComposite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, true);
        buildControlComposite.setLayout(gridLayout);
        buildControlComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
            true, false));
        return buildControlComposite;
    }

    protected void createInfoLabel(Composite composite) {
        infoLabel = new Label(composite, SWT.SINGLE);
        infoLabel.setForeground(infoLabel.getDisplay().getSystemColor(
            SWT.COLOR_RED));
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        infoLabel.setLayoutData(data);
        updateInfoLabel();
    }

    protected void createActivateBtn(Composite composite) {
        createAccountGroupButton(composite, ACTIVATE_IMAGE, ACTIVATE_BTN_TEXT,
            new ActiveAccountChangeListener());
    }

    protected void createAccountGroupButton(Composite composite, Image icon,
        String text, Listener listener) {
        Button activateAccountBtn = new Button(composite, SWT.PUSH);
        activateAccountBtn.setImage(icon);
        activateAccountBtn.setText(text);
        activateAccountBtn.addListener(SWT.Selection, listener);
        activateAccountBtn
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    class ActiveAccountChangeListener implements Listener {
        public void handleEvent(Event event) {
            if (isEntrySelected()) {
                setAccountActive();
            } else {
                warnNothingSelected();
            }
        }
    }

    protected void setAccountActive() {
        int selectedEntryId = getSelectedEntryID();
        accountStore.setAccountActive(accountStore.getAccount(selectedEntryId));
        updateInfoLabel();
        updateList();
    }

    protected void createChangeBtn(Composite composite) {
        createAccountGroupButton(composite, CHANGE_IMAGE, CHANGE_BTN_TEXT,
            new AccountChangeListener());
    }

    class AccountChangeListener implements Listener {
        public void handleEvent(Event event) {
            if (event.type == SWT.Selection) {
                if (isEntrySelected()) {
                    changeAccountData();
                } else {
                    warnNothingSelected();
                }
            }
        }
    }

    protected boolean isEntrySelected() {
        return accountList.getSelectionIndex() != -1;
    }

    protected void changeAccountData() {
        WizardDialog changeWizard = new WizardDialog(parent.getShell(),
            new ChangeWizard(getSelectedAccount()));
        if (Window.OK == changeWizard.open()) {
            updateInfoLabel();
            updateList();
        }
    }

    protected int getSelectedEntryID() {
        XMPPAccount account = accountStore.getAllAccounts().get(
            this.selectedEntryId);
        return account.getId();
    }

    public XMPPAccount getSelectedAccount() {
        int selectedEntryId = getSelectedEntryID();
        XMPPAccount selectedAccount = this.accountStore
            .getAccount(selectedEntryId);
        return selectedAccount;
    }

    protected void warnNothingSelected() {
        MessageDialog.openError(getShell(), "No account selected",
            NO_ENTRY_SELECTED_TEXT);
    }

    public void updateInfoLabel() {
        try {
            if (accountStore.hasActiveAccount()) {
                infoLabel.setText("Active: "
                    + accountStore.getActiveAccount().toString());
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    protected void createAddAccountBtn(Composite composite) {
        createAccountGroupButton(composite, ADD_IMAGE, ADD_BTN_TEXT,
            new AddAccountHandler());
    }

    class AddAccountHandler implements Listener {
        public void handleEvent(Event event) {
            if (event.type == SWT.Selection) {
                openNewAccountWizard();
            }
        }
    }

    protected void openNewAccountWizard() {
        ConfigurationWizard wiz = new ConfigurationWizard(true, false, true);
        WizardDialog wizard = new WizardDialog(parent.getShell(), wiz);
        wizard.setHelpAvailable(false);
        if (Window.OK == wizard.open()) {
            updateInfoLabel();
            updateList();
        }
    }

    protected void createDeleteBtn(Composite composite) {
        createAccountGroupButton(composite, DELETE_IMAGE, DELETE_BTN_TEXT,
            new DeleteHandler());
    }

    class DeleteHandler implements Listener {
        public void handleEvent(Event e) {
            if (isEntrySelected()) {
                deleteBtnPressend();
            } else {
                warnNothingSelected();
            }
        }
    }

    protected void deleteBtnPressend() {
        int selectedEntryId = getSelectedEntryID();
        if (hasActiveAccountDeleted()) {
            handleActiveAccountDeleted();
        } else {
            accountStore
                .deleteAccount(accountStore.getAccount(selectedEntryId));
        }
        updateList();
    }

    protected boolean hasActiveAccountDeleted() {
        int selectedEntryId = getSelectedEntryID();
        return (selectedEntryId == accountStore.getActiveAccount().getId());
    }

    protected void handleActiveAccountDeleted() {
        MessageDialog.openError(getShell(), "Deleting active account",
            DELETE_ACTIVE_TEXT);
    }

    public void updateList() {
        accountList.removeAll();
        for (XMPPAccount account : accountStore.getAllAccounts()) {
            accountList.add(account.toString());
        }
    }

    protected void createAutomaticConnectField(Composite group) {
        addField(new BooleanFieldEditor(PreferenceConstants.AUTO_CONNECT,
            STARTUP_CONNECT_TEXT, group));
    }

    protected void createVersionControlPreferences(Composite group) {
        BooleanFieldEditor editor = new BooleanFieldEditor(
            PreferenceConstants.DISABLE_VERSION_CONTROL,
            DISABLE_VERSION_CONTROL_TEXT, group);
        Control descriptionControl = editor.getDescriptionControl(group);
        descriptionControl.setToolTipText(DISABLE_VERSION_CONTROL_TOOLTIP);
        addField(editor);
    }

    protected void createFollowModePreferences() {
        Composite group = createGroupWithGridLayout(1, "Follow Mode");
        addField(new BooleanFieldEditor(PreferenceConstants.AUTO_FOLLOW_MODE,
            FOLLOW_MODE_TEXT, group));
        addField(new BooleanFieldEditor(
            PreferenceConstants.FOLLOW_EXCLUSIVE_DRIVER, ROLE_CHANGE_TEXT,
            group));
    }

    protected Composite createGroup(String text, Composite parent) {
        return createGroupWithGridLayout(1, text);
    }

    protected void createMultiDriverPreferences() {
        Composite group = createGroupWithGridLayout(1, "Multi Driver");
        addField(new BooleanFieldEditor(PreferenceConstants.MULTI_DRIVER,
            MULTI_DRIVER_SUPPORT_TEXT, group));
        addField(new BooleanFieldEditor(PreferenceConstants.CONCURRENT_UNDO,
            CONCURRENT_UNDO_TEXT, group));
    }

    public void init(IWorkbench workbench) {
        // Nothing to initialize
    }

    @Override
    public boolean performOk() {
        this.accountStore.saveAccounts();
        return super.performOk();
    }

    @Override
    protected void performApply() {
        this.accountStore.saveAccounts();
        super.performApply();
    }

}
