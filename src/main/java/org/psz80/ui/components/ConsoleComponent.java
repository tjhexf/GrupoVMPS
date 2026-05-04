package org.psz80.ui.components;

import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class ConsoleComponent {

    private final TextArea consoleArea = new TextArea();

    public ConsoleComponent() {
        consoleArea.setEditable(false);
    }

    public Node getRoot() {
        VBox container = new VBox();
        container.setFillWidth(true);
        VBox.setVgrow(consoleArea, Priority.ALWAYS);

        VBox wrapper = new VBox();
        wrapper.getChildren().addAll(new javafx.scene.control.Label("Console I/O"), consoleArea);
        return wrapper;
    }

    public TextArea getConsoleArea() {
        return consoleArea;
    }

    public void appendText(String text) {
        consoleArea.appendText(text);
    }

    public void clear() {
        consoleArea.clear();
    }
}