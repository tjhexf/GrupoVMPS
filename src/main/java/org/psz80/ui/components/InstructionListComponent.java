package org.psz80.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class InstructionListComponent {

    private final TableView<InstructionRow> instructionTable = new TableView<>();
    private final ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();

    public InstructionListComponent() {
        setupTable();
    }

    private void setupTable() {
        TableColumn<InstructionRow, String> idxCol = new TableColumn<>("#");
        idxCol.setCellValueFactory(c -> c.getValue().indexProperty());
        idxCol.setPrefWidth(50);

        TableColumn<InstructionRow, String> txtCol = new TableColumn<>("Instrução");
        txtCol.setCellValueFactory(c -> c.getValue().textProperty());
        txtCol.prefWidthProperty().bind(instructionTable.widthProperty().subtract(idxCol.widthProperty()).subtract(2));

        instructionTable.getColumns().addAll(idxCol, txtCol);
        instructionTable.setItems(instructionRows);
    }

    public Node getRoot() {
        VBox container = new VBox();
        container.setFillWidth(true);
        VBox.setVgrow(instructionTable, Priority.ALWAYS);

        VBox wrapper = new VBox();
        wrapper.getChildren().addAll(new Label("Lista de Instruções"), instructionTable);
        return wrapper;
    }

    public TableView<InstructionRow> getInstructionTable() {
        return instructionTable;
    }

    public ObservableList<InstructionRow> getInstructionRows() {
        return instructionRows;
    }

    public void clearInstructions() {
        instructionRows.clear();
    }

    public void addInstruction(int idx, String text) {
        instructionRows.add(new InstructionRow(idx, text));
    }

    public static class InstructionRow {
        private final javafx.beans.property.SimpleStringProperty index;
        private final javafx.beans.property.SimpleStringProperty text;

        public InstructionRow(int idx, String txt) {
            index = new javafx.beans.property.SimpleStringProperty(String.format("%02d", idx));
            text = new javafx.beans.property.SimpleStringProperty(txt);
        }

        public javafx.beans.property.StringProperty indexProperty() { return index; }
        public javafx.beans.property.StringProperty textProperty() { return text; }
    }
}