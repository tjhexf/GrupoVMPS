package org.psz80.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class EditorComponent {

    private final TabPane editorTabs = new TabPane();
    private final Map<Tab, EditorTab> editorTabMap = new HashMap<>();

    public EditorComponent() {
        editorTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    public Node getRoot() {
        VBox container = new VBox();
        container.setFillWidth(true);
        VBox.setVgrow(editorTabs, Priority.ALWAYS);

        VBox editorWrapper = new VBox();
        editorWrapper.getChildren().addAll(new Label("Editor de Texto"), editorTabs);
        return editorWrapper;
    }

    public TabPane getEditorTabs() {
        return editorTabs;
    }

    public void createNewTab(File file, String content) {
        CodeArea area = new CodeArea();
        if (content != null) area.replaceText(content);

        VirtualizedScrollPane<CodeArea> vs = new VirtualizedScrollPane<>(area);
        String title = (file == null) ? "untitled" + (editorTabs.getTabs().size() + 1) : file.getName();

        Tab tab = new Tab(title, vs);
        EditorTab et = new EditorTab(area, file);
        editorTabMap.put(tab, et);
        tab.setUserData(et);
        tab.setOnClosed(ev -> editorTabMap.remove(tab));

        editorTabs.getTabs().add(tab);
        editorTabs.getSelectionModel().select(tab);
    }

    public EditorTab getActiveEditorTab() {
        Tab t = editorTabs.getSelectionModel().getSelectedItem();
        if (t == null) return null;
        Object ud = t.getUserData();
        return (ud instanceof EditorTab) ? (EditorTab) ud : null;
    }

    public TabPane getTabs() {
        return editorTabs;
    }

    public void loadFile(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            createNewTab(file, String.join("\n", lines));
        } catch (Exception e) {
            System.err.println("Erro ao abrir arquivo: " + e.getMessage());
        }
    }

    public void saveFile(EditorTab et) {
        try {
            if (et.file == null) return;
            Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    public static class EditorTab {
        public final CodeArea area;
        public File file;
        public final ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();

        public EditorTab(CodeArea a, File f) {
            area = a;
            file = f;
        }

        public ObservableList<InstructionRow> getInstructionRows() {
            return instructionRows;
        }
    }

    public static class InstructionRow {
        private final StringProperty index = new SimpleStringProperty();
        private final StringProperty text = new SimpleStringProperty();

        public InstructionRow(int idx, String txt) {
            index.set(String.format("%02d", idx));
            text.set(txt);
        }

        public StringProperty indexProperty() { return index; }
        public StringProperty textProperty() { return text; }
    }
}