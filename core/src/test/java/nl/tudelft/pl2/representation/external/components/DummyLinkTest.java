package nl.tudelft.pl2.representation.external.components;

import nl.tudelft.pl2.representation.external.Edge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

class DummyLinkTest {
    private DummyLink dummyLink;
    private Edge origin;
    @BeforeEach
    void before() {
        origin = new Edge(0, 1);
        dummyLink = new DummyLink(0, 1, origin);
    }

    @Test
    void getOrigin() {
        assertThat(dummyLink.origin()).isEqualTo(origin);
    }

    @Test
    void isDummyTrueTest() {
        assertThat(dummyLink.isDummy()).isTrue();
    }

    @Test
    void isDummyFalseTest() {
        assertThat(origin.isDummy()).isFalse();
    }
}