package nl.tudelft.pl2.representation.ui;

import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CommandLineToolTest {

    @Test
    public void testCommandLineTool() throws URISyntaxException {
        Path path = Paths.get(Thread
                .currentThread()
                .getContextClassLoader()
                .getResource("test1.gfa").toURI());

        CommandLineTool.main(new String[]{path.toString()});
        assertThat(CommandLineTool.isLoaded()).isTrue();
    }
}
