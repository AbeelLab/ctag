package nl.tudelft.pl2.representation.graph;

import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class GraphLoaderTest {
    protected GraphLoader loader;
    protected GraphHandle handle;

    @Before
    public void before() throws URISyntaxException, ExecutionException, InterruptedException {
        List<Future> futures = loader.getLoadFutures();
        for (Future future : futures) {
            future.get();
        }
    }

    @Test
    public void testDummyGet() {
        assertThat(loader.getDummyNodeHelper())
                .isEqualTo(loader.getDummyNodeHelper());
    }

    @Test
    public void testVerticalGet() {
        assertThat(loader.getVerticalIndexingHelper())
                .isEqualTo(loader.getVerticalIndexingHelper());
    }

    @Test
    public void testLoadingProperty() {
        assertThat(loader.getLoadingProperty().get())
                .isCloseTo(1.0, Offset.offset(0.2));
    }

    @Test
    public void testLoadingState() {
        assertThat(loader.getLoadingState().get())
                .isEqualTo(LoadingState.CACHES_LOADED);
    }

    @After
    public void after() {
        handle.unload();
    }
}
