package nl.tudelft.pl2.representation.graph.handles;

import nl.tudelft.pl2.data.storage.HeatMap;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphHandleTest;
import nl.tudelft.pl2.representation.graph.MoveDirection;
import nl.tudelft.pl2.representation.graph.loaders.ChunkedGraphLoader;
import nl.tudelft.pl2.representation.ui.DefaultThreadCompleter;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Class for testing the chunkedGraph.
 */
public class ChunkedGraphTest extends GraphHandleTest {
    private ChunkedGraph chunkedGraph;
    private Map<Integer, Integer> verticalMap;
    private Map<Integer, HashSet<Node>> layerMap;

    @Before
    public void before() throws ExecutionException, InterruptedException, URISyntaxException {
        URL u = Thread.currentThread().getContextClassLoader().getResource("test1.gfa");
        assert u != null;
        graphBuilder = new ChunkedGraphLoader();
        graphHandle = graphBuilder.load(Paths.get(u.toURI()), new DefaultThreadCompleter());
        verticalMap = new HashMap<>();
        layerMap = new HashMap<>();
        ChunkedGraphLoader loader = new ChunkedGraphLoader();
        chunkedGraph = new ChunkedGraph(loader, verticalMap, layerMap, Paths.get(u.toURI()));
        super.before();
    }

    @Test
    public void testInvalidMoveRight() throws ExecutionException, InterruptedException {
        Node oldCentre = graphHandle.centre();
        graphHandle.move(5, MoveDirection.RIGHT);
        List<Future> futureList = graphBuilder.getLoadFutures();
        for (Future future : futureList) {
            future.get();
        }
        AssertionsForClassTypes.assertThat(oldCentre)
                .isNotEqualTo(graphHandle.centre());
        AssertionsForInterfaceTypes.assertThat(
                graphHandle.getSegmentsFromLayer(2)
        ).containsOnlyOnce((Node) graphHandle.centre());
    }

    @Test
    public void testMaxLayer() {
        assertThat(graphHandle.getMaxLayer()).isEqualTo(2);
    }

    @Test
    public void testValidLayerMap() {
        int maxLayer = graphHandle.getMaxLayer();
        Integer[] expectedSize = {1, 1, 1};
        for (int i = 0; i <= maxLayer; i++) {
            assertThat(
                    graphHandle.getSegmentsFromLayer(i).size()
            ).isEqualTo(expectedSize[i]);
        }
    }

    @Test
    public void validHeatMapTest() {
        HeatMap heatMap = graphHandle.getHeatMap();
        AssertionsForClassTypes.assertThat(heatMap.maxLayer())
                .isEqualTo(graphHandle.getMaxLayer());
    }

    @Test
    public void testInvalidHeaderUpdate() {
        final String tag = "OR";
        final String value = "1;2;3";
        final String[] expected = new String[0];
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put(tag, value);
        Observer observer = Mockito.mock(Observer.class);
        chunkedGraph.registerObserver(observer);
        chunkedGraph.updateHeaders(headerMap);
        Mockito.verify(observer).update(chunkedGraph, expected);
    }
}
