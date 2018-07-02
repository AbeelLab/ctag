package nl.tudelft.pl2.representation.ui.InfoSidePanel;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import nl.tudelft.pl2.representation.ui.ControllerManager;
import nl.tudelft.pl2.representation.ui.graph.GraphController;
import nl.tudelft.pl2.representation.ui.graph.GraphKeyEventHandler;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class to wrap the side-panel {@link ScrollPane} and to allow
 * for specific actions to be performed easily on this panel.
 */
public class SidePanelWrapper {

    /**
     * The scroll panel that is wrapped with functionality
     * in this SidePanelWrapper.
     */
    private ScrollPane pane;

    /**
     * Constructs a new {@link SidePanelWrapper} from the
     * {@link ScrollPane} it wraps.
     *
     * @param paneIn The {@link ScrollPane} to wrap.
     */
    SidePanelWrapper(final ScrollPane paneIn) {
        this.pane = paneIn;
    }

    /**
     * Requests focus of the graph panel.
     */
    private void focusOnGraphPanel() {
        ControllerManager.get(GraphController.class).focus();
    }

    /**
     * Traverses all children of the given node and executes
     * the given consumer for each child and each parent.
     *
     * @param node   The parent node from which to traverse down
     *               the children.
     * @param action The action to take for each node.
     */
    private void traverseChildren(final Node node,
                                  final Consumer<Node> action) {
        action.accept(node);
        if (node instanceof Parent) {
            ((Parent) node).getChildrenUnmodifiable()
                    .forEach(n -> traverseChildren(n, action));
        }
    }

    /**
     * Sets the given {@link Node} and all its children to be
     * non-focusable. Instead of focusing on this panel, we
     * transfer focus to the graph panel immediately.
     *
     * @param node The node to set non-focusable.
     */
    private void setNotFocusable(final Node node) {
        traverseChildren(node, n -> {
            if (!(n instanceof Control) || n instanceof ScrollPane) {
                n.focusedProperty().addListener(
                        (obj, o, ne) -> focusOnGraphPanel());
            }
        });
    }

    /**
     * Sets this panel and all its children to be non-focusable.
     * Instead of focusing on this panel, we transfer focus to
     * the graph panel immediately.
     */
    final void setNotFocusable() {
        setNotFocusable(pane);
    }

    /**
     * Prioritizes the given event handler but keeps the original
     * event handler if the prioritized handler does not use an
     * event.
     *
     * @param node    Node to prioritize all children on.
     * @param handler The handler represented as a function returning
     *                a boolean representing whether it fired on the
     *                given event.
     */
    private void prioritizeEventHandler(
            final Node node,
            final Function<KeyEvent, Boolean> handler) {
        traverseChildren(node, n -> {
            if (!(n instanceof Control) || n instanceof ScrollPane) {
                EventHandler<? super KeyEvent> current = n.getOnKeyPressed();
                n.setOnKeyPressed(event -> {
                    if (!handler.apply(event) && current != null) {
                        current.handle(event);
                    }
                });
            }
        });
    }

    /**
     * Prioritizes the graph key event handler for all children
     * of this panel.
     */
    final void prioritizeGraphKeyEventHandler() {
        prioritizeEventHandler(pane, GraphKeyEventHandler.HANDLER);
    }
}
