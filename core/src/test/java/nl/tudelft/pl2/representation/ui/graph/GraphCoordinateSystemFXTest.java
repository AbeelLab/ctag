package nl.tudelft.pl2.representation.ui.graph;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphCoordinateSystemFXTest extends ApplicationTest {
    private static final int SHOWN_LAYERS = 5;

    private static final double WIDTH = 535.0;
    private static final double HEIGHT = 450.0;
    private static final double TRANS_X = -200.0;
    private static final double TRANS_Y = -100.0;

    private Canvas canvas;
    private GraphicsContext graphics;

    private GraphCoordinateSystem underTest;

    @Override
    public void start(Stage stage) throws Exception {
        super.start(stage);

        canvas = new Canvas(WIDTH, HEIGHT);
        graphics = canvas.getGraphicsContext2D();
    }

    @Before
    public void before() {
        canvas.setWidth(WIDTH);
        canvas.setHeight(HEIGHT);
        graphics.setTransform(new Affine(
                0.0, 0.0, TRANS_X, 0.0, 0.0, TRANS_Y));

        underTest = new GraphCoordinateSystem(graphics, SHOWN_LAYERS);
    }

    @Test
    public void initialObjectShouldContainGivenValues() {
        assertThat(underTest.shownLayers()).isEqualTo(SHOWN_LAYERS);
        assertThat(underTest.graphics()).isEqualTo(graphics);

        assertThat(underTest.screenWidth())
                .isCloseTo(WIDTH, Offset.offset(0.1));
        assertThat(underTest.screenHeight())
                .isCloseTo(HEIGHT, Offset.offset(0.1));
    }

    @Test
    public void initializationRecalculatesNodeWidthAndPadding() {
        int expectedTotalWidth = (int) Math.ceil(WIDTH / SHOWN_LAYERS);
        int expectedNodeWidth = Math.max(1, (int)
                (expectedTotalWidth * GraphCoordinateSystem.NODE_PART()));
        int expectedPadding = expectedTotalWidth - expectedNodeWidth;

        assertThat(underTest.totalWidth()).isEqualTo(expectedTotalWidth);
        assertThat(underTest.nodeWidth()).isEqualTo(expectedNodeWidth);
        assertThat(underTest.padding()).isEqualTo(expectedPadding);
    }

    @Test
    public void lowerBoundAndUpperBoundShouldShowAtLeastShownLayers() {
        assertThat(underTest.lowerBound()).isLessThanOrEqualTo(
                (int) underTest.minLayer());
        assertThat(underTest.upperBound()).isGreaterThanOrEqualTo(
                (int) (underTest.minLayer() + SHOWN_LAYERS));
    }

    @Test
    public void layerAtWorks() {
        graphics.setTransform(0, 0, 0, 0, 0, 0);

        assertThat(underTest.layerAt(0))
                .isCloseTo(0.0, Offset.offset(0.1));
        assertThat(underTest.layerAt(2.5 * underTest.totalWidth()))
                .isCloseTo(2.5, Offset.offset(0.1));
    }

    @Test
    public void rowAtWorks() {
        graphics.setTransform(0, 0, 0, 0, 0, 0);

        assertThat(underTest.rowAt(0))
                .isCloseTo(0.0, Offset.offset(0.1));
        assertThat(underTest.rowAt(2.5 * underTest.totalHeight()))
                .isCloseTo(2.5, Offset.offset(0.1));
    }

    @Test
    public void recalculatingLayersChangesLeftLayerAndShownLayers() {
        canvas.setWidth(100.0);
        canvas.setHeight(100.0);
        graphics.setTransform(0, 0, 0, 0, 0, 0);

        underTest.recalculateLayers(10);

        assertThat(underTest.shownLayers())
                .isEqualTo(10);
        assertThat(underTest.minLayer())
                .isCloseTo(0.0, Offset.offset(0.1));
    }
}
