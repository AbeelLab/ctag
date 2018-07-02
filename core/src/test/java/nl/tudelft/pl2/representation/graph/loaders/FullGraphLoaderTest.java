package nl.tudelft.pl2.representation.graph.loaders;

import nl.tudelft.pl2.data.Scheduler;
import nl.tudelft.pl2.representation.external.Node;
import nl.tudelft.pl2.representation.graph.GraphLoaderTest;
import nl.tudelft.pl2.representation.graph.handles.FullGraph;
import nl.tudelft.pl2.representation.ui.DefaultThreadCompleter;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class FullGraphLoaderTest extends GraphLoaderTest {
    private FullGraphLoader builder;

    private void loadGraphHandle(String fileName) throws URISyntaxException {
        URL u = Thread.currentThread().getContextClassLoader().getResource(fileName);
        builder = new FullGraphLoader();
        loader = builder;
        assert u != null;
        handle =  builder.load(Paths.get(u.toURI()), new DefaultThreadCompleter());
    }

    @Before
    public void before() throws URISyntaxException, ExecutionException, InterruptedException {
        loadGraphHandle("test3.gfa");
        super.before();
    }

    @Test
    public void testValidDummyCount() {
        assertThat(handle.getSegmentsFromLayer(1).size()).isEqualTo(2);
    }

    @Test
    public void testValidVerticalLocation() throws URISyntaxException, ExecutionException, InterruptedException {
        loadGraphHandle("test4.gfa");
        List<Future> futureList = builder.getLoadFutures();
        for (Future future : futureList) {
            future.get();
        }
        FullGraph fullGraph = (FullGraph) handle;
        Node node = fullGraph.getSegmentsFromLayer(0).iterator().next();
        assertThat(fullGraph.getVerticalPosition(node)).isEqualTo(0);
    }

    @Test
    public void testAddLoadingFuture() {
        boolean[] shouldFinish = new boolean[]{false};
        Future future = Scheduler.schedule(() -> {
            while (!shouldFinish[0]) ;
        });
        shouldFinish[0] = true;
        loader.addLoadFuture(future);
        assertThat(future.isDone()).isTrue();
    }
}