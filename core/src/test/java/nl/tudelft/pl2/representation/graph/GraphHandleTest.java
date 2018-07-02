package nl.tudelft.pl2.representation.graph;

import nl.tudelft.pl2.data.gff.TraitMap;
import nl.tudelft.pl2.representation.GraphPosition;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.ui.graph.GraphCoordinateSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public abstract class GraphHandleTest {
    protected GraphHandle graphHandle;
    protected GraphLoader graphBuilder;

    private final String[] expected = {"1", "2", "3"};

    @Before
    public void before() throws InterruptedException, ExecutionException, URISyntaxException {
        List<Future> futures = graphBuilder.getLoadFutures();
        for (Future future : futures) {
            future.get();
        }
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
    public void testValidCentreNode() {
        assertThat(
                graphHandle.getSegmentsFromLayer(0)
        ).containsOnlyOnce((Node) graphHandle.centre());
    }

    @Test
    public void testValidMoveRight() throws ExecutionException, InterruptedException {
        Node oldCentre = graphHandle.centre();
        graphHandle.move(1, MoveDirection.RIGHT);
        List<Future> futureList = graphBuilder.getLoadFutures();
        for (Future future : futureList) {
            future.get();
        }
        assertThat(oldCentre).isNotEqualTo(graphHandle.centre());
        assertThat(
                graphHandle.getSegmentsFromLayer(1)
        ).containsOnlyOnce((Node) graphHandle.centre());
    }

    @Test
    public void testInvalidMoveRight() throws ExecutionException, InterruptedException {
        Node oldCentre = graphHandle.centre();
        graphHandle.move(5, MoveDirection.RIGHT);
        assertThat(oldCentre).isNotEqualTo(graphHandle.centre());
        assertThat(
                graphHandle.getSegmentsFromLayer(2)
        ).containsOnlyOnce((Node) graphHandle.centre());
    }

    @Test
    public void testValidMoveLeft() {
        Node oldCentre = graphHandle.centre();
        GraphPosition oldPosition = graphHandle.position();
        graphHandle.move(1, MoveDirection.RIGHT);
        graphHandle.move(1, MoveDirection.LEFT);
        assertThat(oldCentre).isEqualTo(graphHandle.centre());
        assertThat(oldPosition.layer())
                .isEqualTo(graphHandle.getCentreLayer());
    }

    @Test
    public void testInvalidMoveLeft() {
        Node oldCentre = graphHandle.centre();
        GraphPosition oldPosition = graphHandle.position();
        graphHandle.move(1, MoveDirection.LEFT);
        assertThat(oldCentre).isEqualTo(graphHandle.centre());
        assertThat(oldPosition.layer())
                .isEqualTo(graphHandle.position().layer());
    }

    @Test
    public void testInvalidMoveLeft2() {
        GraphPosition oldPosition = graphHandle.position();
        graphHandle.move(1, MoveDirection.LEFT);
        assertThat(oldPosition.layer())
                .isEqualTo(graphHandle.position().layer());
    }

    @Test
    public void testSetCentre() {
        int currentLayer = graphHandle.getCentreLayer();
        Set<Node> set = graphHandle.getSegmentsFromLayer(currentLayer + 1);
        int moved = graphHandle.setCentre(set.iterator().next()).getValue();
        assertThat(graphHandle.getCentreLayer()).isEqualTo(moved + currentLayer);
    }

    @Test
    public void testVerticalIndex() {
        Node node = graphHandle.getSegmentsFromLayer(0).iterator().next();
        assertThat(graphHandle.getVerticalPosition(node)).isEqualTo(0);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidVerticalIndex() {
        Node node = Mockito.mock(Node.class);
        Mockito.when(node.id()).thenReturn(-1);
        graphHandle.getVerticalPosition(node);
    }

    @Test
    public void testObserver() {
        Observer observer = Mockito.mock(Observer.class);
        graphHandle.registerObserver(observer);
        graphHandle.signalLoaded(LoadingState.FULLY_LOADED);
        Mockito.verify(observer).update((Observable) graphHandle,
                LoadingState.FULLY_LOADED);
    }

    @Test
    public void testLayerSet() {
        Set<Integer> keySet = graphHandle.getLayerSet();
        Set<Integer> expected = new HashSet<>();
        expected.add(0);
        expected.add(1);
        expected.add(2);
        assertThat(keySet).containsAll(expected);
    }

    @Test
    public void genomeCount1Test() {
        Map<String, String> headers = new HashMap<>();
        headers.put("ORI", "1");
        graphHandle.updateHeaders(headers);
        assertThat(graphHandle.genomeCount()).isEqualTo(1);
    }

    @Test
    public void genomeCount0Test() {
        Map<String, String> headers = new HashMap<>();
        headers.put("OR", "1");
        graphHandle.updateHeaders(headers);
        assertThat(graphHandle.genomeCount()).isEqualTo(0);
    }

    @Test
    public void updateBoundsNoChangeTest() {
        GraphCoordinateSystem system =
                Mockito.mock(GraphCoordinateSystem.class);
        graphHandle.updateBounds(system.upperBound(), system.lowerBound());
    }

    @Test
    public void updateBoundsChangeTest() {
        GraphCoordinateSystem system =
                Mockito.mock(GraphCoordinateSystem.class);
        int currentLayer = graphHandle.getCentreLayer();
        Mockito.when(system.upperBound())
                .thenReturn(currentLayer + 1);
        graphHandle.updateBounds(system.upperBound(), system.lowerBound());
    }

    @Test
    public void testValidHeaderUpdate() {
        final String tag = "ORI";
        final String value = "1;2;3";
        HashMap<String, String> headerMap = new HashMap<>();
        headerMap.put(tag, value);
        Observer observer = Mockito.mock(Observer.class);
        graphHandle.registerObserver(observer);
        graphHandle.updateHeaders(headerMap);
        Mockito.verify(observer).update((Observable) graphHandle, expected);
    }

    @Test
    public void validGenomeTest() {
        testValidHeaderUpdate();
        assertThat(graphHandle.getGenomes()).isEqualTo(expected);
    }

    @Test
    public void semanticZoomSameTest() {
        assertThat(graphHandle.semanticZoom(0, ZoomDirection.IN)).isEqualTo(0);
    }

    @Test
    public void semanticZoomDifferentTest() {
        assertThat(graphHandle.semanticZoom(1, ZoomDirection.OUT)).isEqualTo(1);
    }

    @Test
    public void traitMapTest() {
        TraitMap traitMap = Mockito.mock(TraitMap.class);
        graphHandle.setTraitMap(traitMap);
        assertThat(graphHandle.getTraitMap()).isEqualTo(traitMap);
    }

    @Test
    public void getSegmentLayerTest() {
        int max = graphHandle.getMaxLayer();
        Set<Node> nodes = new HashSet<>();
        for (int i = 0; i <= max; i++) {
            nodes.addAll(graphHandle.getSegmentsFromLayer(i));
        }
        for (Node node : nodes) {
            assertThat(graphHandle.getSegmentLayer(node.id()))
                    .isEqualTo(node.layer());
        }
    }

    @Test
    public void getNodesByGenomeTest() {
        Set<Node> nodes = graphHandle.getNodesByGenome("");
        assertThat(nodes).isEmpty();
    }

    @Test
    public void validGraphNameTest() {
        String name = graphHandle.getGraphName();
        assertThat(name).isNotEqualToIgnoringCase("test3.gfa");
    }

    @After
    public void after() {
        graphHandle.unload();
        graphHandle = null;
    }
}
