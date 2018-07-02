package nl.tudelft.pl2.representation.graph.handles;

import javafx.util.Pair;
import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandleTest;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.loaders.FullGraphLoader;
import nl.tudelft.pl2.representation.ui.DefaultThreadCompleter;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class FullGraphTest extends GraphHandleTest {
    @Before
    public void before() throws URISyntaxException, ExecutionException, InterruptedException {
        URL u = Thread.currentThread().getContextClassLoader().getResource("test1.gfa");
        assert u != null;
        graphBuilder = new FullGraphLoader();
        graphHandle = graphBuilder.load(Paths.get(u.toURI()), new DefaultThreadCompleter());
        super.before();
    }

    @Test
    public void testSetCentreSteps() {
        int currentLayer = graphHandle.getCentreLayer();
        Set<Node> set =
                graphHandle.getSegmentsFromLayer(graphHandle.getMaxLayer());
        Pair<MoveDirection, Integer> moved =
                graphHandle.setCentre(set.iterator().next());
        assertThat(graphHandle.getCentreLayer()).isEqualTo(moved.getValue() + currentLayer);
    }

    @Test
    public void validHeatMapTest() {
        HeatMap heatMap = graphHandle.getHeatMap();
        AssertionsForClassTypes.assertThat(heatMap).isNull();
    }

    @Test
    public void testSetCentreDirection() {
        Set<Node> set =
                graphHandle.getSegmentsFromLayer(graphHandle.getMaxLayer());
        Pair<MoveDirection, Integer> moved =
                graphHandle.setCentre(set.iterator().next());
        assertThat(moved.getKey()).isEqualTo(MoveDirection.RIGHT);
    }
}
