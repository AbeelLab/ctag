package nl.tudelft.pl2.representation.graph.loaders;

import nl.tudelft.pl2.representation.graph.GraphLoaderTest;
import nl.tudelft.pl2.representation.ui.DefaultThreadCompleter;
import org.junit.Before;
import org.junit.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ChunkedGraphLoaderTest extends GraphLoaderTest {
    @Before
    public void before() throws InterruptedException, ExecutionException, URISyntaxException {
        URL u = Thread.currentThread().getContextClassLoader().getResource("test3.gfa");
        loader = new ChunkedGraphLoader();
        assert u != null;
        handle = loader.load(Paths.get(u.toURI()), new DefaultThreadCompleter());
        super.before();
    }

    @Test
    public void testUniqueId() {
        ChunkedGraphLoader chunkedGraphLoader = (ChunkedGraphLoader) loader;
        assertThat(chunkedGraphLoader.createNewId())
                .isNotEqualTo(chunkedGraphLoader.createNewId());
    }
}
