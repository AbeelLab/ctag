package nl.tudelft.pl2.representation.ui;

import javafx.geometry.VerticalDirection;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import nl.tudelft.pl2.representation.ui.menu.BaseTestFx;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class SidePanelTestFx extends BaseTestFx {
    @Test
    void openSidePanel() {
        clickOn("#leftToggleButton");
        ScrollPane pane = find("#leftPane");
        assertThat(pane.isVisible()).isTrue();

        clickOn("#leftToggleButton");
        assertThat(pane.isVisible()).isFalse();

        press(KeyCode.CONTROL);
        press(KeyCode.SHIFT);
        press(KeyCode.O).release(KeyCode.CONTROL).release(KeyCode.CONTROL);
        release(KeyCode.SHIFT);

        sleep(1000);

        press(KeyCode.LEFT).release(KeyCode.LEFT);
        press(KeyCode.RIGHT).release(KeyCode.RIGHT);
        press(KeyCode.HOME).release(KeyCode.HOME);
        press(KeyCode.END).release(KeyCode.END);

        scroll(VerticalDirection.DOWN);
        scroll(VerticalDirection.UP);
    }
}
