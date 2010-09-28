package de.fu_berlin.inf.dpp.stf.swtbot.helpers;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

import de.fu_berlin.inf.dpp.stf.swtbot.RmiSWTWorkbenchBot;
import de.fu_berlin.inf.dpp.stf.swtbot.SarosSWTWorkbenchBot;

public class TreeObject {
    private static final transient Logger log = Logger
        .getLogger(TreeObject.class);
    private RmiSWTWorkbenchBot rmiBot;
    private WaitUntilObject wUntil;
    private static SarosSWTWorkbenchBot bot = new SarosSWTWorkbenchBot();

    public TreeObject(RmiSWTWorkbenchBot rmiBot) {
        this.rmiBot = rmiBot;
        this.wUntil = rmiBot.wUntilObject;
    }

    /**
     * This method is a helper method and should not be exported by rmi. it is
     * very helpful, when you want to click a context menu of a tree node in a
     * view.e.g.
     * 
     * in Package Explorer View: click context menu "open" of the class file
     * "MyClass", in this case, you should pass the
     * parameters("Package Explorer", "open", "Foo_Saros","src", "my.pkg",
     * "MyClass.java".
     * 
     * in Roster View: click context menu "rename.." of a user
     * "lin@saros-con.imp.fu-berlin.de" in buddies. In this case, you shuld pass
     * the parameter ("Roster", "rename...", "Buddies",
     * "lin@saros-con.imp.fu-berlin.de").
     * 
     * 1. select the tree node that context menu you want to click.
     * 
     * 2. then click the context menu.
     * 
     * @param viewName
     *            e.g. Package Explorer view or Resource Explorer view
     * @param nodes
     *            node path to expand. Attempts to expand all nodes along the
     *            path specified by the node array parameter.
     * @throws RemoteException
     */
    public void clickContextMenuOfTreeInView(String viewName, String context,
        String... nodes) throws RemoteException {
        SWTBotTreeItem treeItem = selectTreeWithLabelsInView(viewName, nodes);
        if (treeItem == null) {
            log.error("Tree item not found " + nodes.toString());
            return;
        }
        final SWTBotMenu contextMenu = treeItem.contextMenu(context);
        if (contextMenu == null) {
            log.error("Context menu \"" + context + "\" not found");
            return;
        }
        contextMenu.click();
    }

    /**
     * @param viewName
     *            the title of the specified view
     * @param labels
     *            all labels on the widget
     * @return a {@link SWTBotTreeItem} with the specified <code>label</code>.
     */

    public SWTBotTreeItem selectTreeWithLabelsInView(String viewName,
        String... labels) throws RemoteException {
        // SWTBotView view =
        // getViewByTitle(SarosConstant.VIEW_TITLE_PACKAGE_EXPLORER);
        //
        // Composite composite = (Composite) view.getWidget();
        // Tree swtTree = delegate.widget(WidgetMatcherFactory
        // .widgetOfType(Tree.class), composite);
        //
        // SWTBotTree tree = new SWTBotTree(swtTree);
        return selectTreeWithLabels(getTreeInView(viewName), labels);
    }

    public SWTBotTree getTreeInView(String viewName) {
        return getViewWithText(viewName).bot().tree();
    }

    /**
     * test, if a tree node exist. This method ist very helpful, if you are not
     * sure, how exactly is the tree node's name.
     */
    public boolean isTreeItemWithMatchTextExist(SWTBotTree tree,
        String... regexs) {
        SWTBotTreeItem item = null;
        for (String regex : regexs) {
            boolean exist = false;
            if (item == null) {
                for (int i = 0; i < tree.getAllItems().length; i++) {
                    log.info("treeItem'name: "
                        + tree.getAllItems()[i].getText());
                    if (tree.getAllItems()[i].getText().matches(regex)) {
                        item = tree.getAllItems()[i].expand();
                        exist = true;
                    }
                }
            } else {
                for (String nodeName : item.getNodes()) {
                    log.info("node'name: " + nodeName);
                    if (nodeName.matches(regex)) {
                        item = item.getNode(nodeName).expand();
                        exist = true;
                    }
                }
            }
            if (!exist) {
                return false;
            }
        }
        return item != null;
    }

    public SWTBotView getViewWithText(String title) {
        SWTBotView view = bot.viewByTitle(title);
        view.show();
        return view;
    }

    public SWTBotTreeItem getTreeItemWithMatchText(SWTBotTree tree,
        String... regexs) {
        try {
            SWTBotTreeItem item = null;
            for (String regex : regexs) {
                if (item == null) {
                    for (int i = 0; i < tree.getAllItems().length; i++) {
                        log.info("treeItem'name: "
                            + tree.getAllItems()[i].getText());
                        if (tree.getAllItems()[i].getText().matches(regex)) {
                            item = tree.getAllItems()[i].expand();
                        }
                    }
                } else {
                    for (String nodeName : item.getNodes()) {
                        log.info("node'name: " + nodeName);
                        if (nodeName.matches(regex)) {
                            item = item.getNode(nodeName).expand();
                        }
                    }
                }
            }
            return item;
        } catch (WidgetNotFoundException e) {
            log.error("gematched Context menu can't be found!", e);
            return null;
        }
    }

    public SWTBotTreeItem selectTreeWithLabels(SWTBotTree tree,
        String... labels) {
        SWTBotTreeItem selectedTreeItem = null;
        for (String label : labels) {
            try {
                if (selectedTreeItem == null) {
                    wUntil.waitUntilTreeExisted(tree, label);
                    selectedTreeItem = tree.expandNode(label);
                    // log.info("treeItem name: " + selectedTreeItem.getText());
                } else {
                    wUntil.waitUntilTreeItemExisted(selectedTreeItem, label);
                    selectedTreeItem = selectedTreeItem.expandNode(label);
                    // log.info("treeItem name: " + selectedTreeItem.getText());
                }
            } catch (WidgetNotFoundException e) {
                // log.error("treeitem \"" + label + "\" not found");
            }
        }
        if (selectedTreeItem != null) {
            selectedTreeItem.select();
            return selectedTreeItem;
        }
        return null;
    }
}
