package nl.tudelft.pl2.representation.external.components;

import nl.tudelft.pl2.representation.external.Node;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class DummyNodeTest {
    @Test
    void widthChangedTest() {
        DummyNode dummyNode = new DummyNode(0, 0);
        dummyNode.setWidth(2.0);
        assertThat(dummyNode.getWidth()).isEqualTo(2.0);
    }

    @Test
    void widthInitTest() {
        DummyNode dummyNode = new DummyNode(0, 0);
        assertThat(dummyNode.getWidth()).isEqualTo(1.0);
    }

    @Test
    void isDummyTrueTest() {
        DummyNode dummyNode = new DummyNode(0, 0);
        assertThat(dummyNode.isDummy()).isTrue();
    }

    @Test
    void isDummyFalseTest() {
        Node node = new Node(0, "", 0, "", null, null, null, null);
        assertThat(node.isDummy()).isFalse();
    }
}