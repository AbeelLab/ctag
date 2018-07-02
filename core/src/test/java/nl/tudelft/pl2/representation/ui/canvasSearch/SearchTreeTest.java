package nl.tudelft.pl2.representation.ui.canvasSearch;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * A class which tests the implementation in the
 * search tree.
 */
public class SearchTreeTest {

    private SearchTree<String> tree;

    /**
     * Creates a new search tree to test with
     * and adds a node to the tree.
     */
    @Before
    public void before(){
        tree = new SearchTree<>();
        tree.add("node1", 0, 0, 2, 2);
    }

    /**
     * This tests that the tree can successfully
     * find the node.
     */
    @Test
    public void searchForKnownNode() {
        final String[] strings = new String[1];
        tree.search(1, 1, (str) -> {
            if(str instanceof String) {
                strings[0] = (String) str;
            }}, e -> {}
        );

    assertThat(strings[0]).isEqualTo("node1");
    }

    @Test
    public void searchForUnKnownNode() {
        final String[] strings = new String[1];
        tree.search(3, 3, e -> {}, (str) -> {
            if(str instanceof String) {
                strings[0] = (String) str;
            }

        });
        assertThat(strings[0]).isEqualTo("");
    }

}
