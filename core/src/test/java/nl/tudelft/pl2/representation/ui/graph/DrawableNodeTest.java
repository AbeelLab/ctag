package nl.tudelft.pl2.representation.ui.graph;


import nl.tudelft.pl2.representation.external.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * This classes test the none ui functionality of the
 * {@link Node} class.
 */
class DrawableNodeTest {
    private Node chunk;
    private DrawableNode node;
    private GraphCoordinateSystem coordinates =
            Mockito.mock(GraphCoordinateSystem.class);

    /**
     * This method is used to setup a new mocked chunk
     * as well as creating a new node to test on.
     */
    @BeforeEach
    void before() {
        chunk = Mockito.mock(Node.class);
        when(chunk.layer()).thenReturn(2);
        NodeData data = new NodeData(chunk, 1, 3,
                coordinates, new HashMap<>());
        node = new DrawableNode(data, Mockito.mock(GenomePainter.class), null);
    }

    /**
     * Make sure that the two nodes are equal when their parameters are equal.
     */
    @Test
    void testNodeCreation() {
        NodeData data = new NodeData(chunk, 1, 3,
                coordinates, new HashMap<>());
        assertThat(node).isEqualTo(
                new DrawableNode(data, Mockito.mock(GenomePainter.class),  null));
    }
}
