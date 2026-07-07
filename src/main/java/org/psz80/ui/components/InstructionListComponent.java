package org.psz80.ui.components;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class InstructionListComponent {

    private final TableView<InstructionRow> instructionTable = new TableView<>();
    private final ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList();
    private final SimpleIntegerProperty highlightedIdx = new SimpleIntegerProperty(-1);
    private final SimpleIntegerProperty nextIdx = new SimpleIntegerProperty(-1);

    public InstructionListComponent() {
        setupTable();
    }

    private void setupTable() {
        TableColumn<InstructionRow, String> idxCol = new TableColumn<>("#");
        idxCol.setCellValueFactory(c -> c.getValue().indexProperty());
        idxCol.setPrefWidth(50);

        TableColumn<InstructionRow, String> txtCol = new TableColumn<>("Instrução");
        txtCol.setCellValueFactory(c -> c.getValue().textProperty());
        txtCol.setCellFactory(tc -> new TableCell<InstructionRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                getStyleClass().removeAll("current-instr", "next-instr");
                if (!empty && item != null && getTableRow() != null) {
                    int idx = getTableRow().getIndex();
                    if (idx == highlightedIdx.get()) {
                        getStyleClass().add("current-instr");
                    } else if (idx == nextIdx.get()) {
                        getStyleClass().add("next-instr");
                    }
                }
            }
        });
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

    public void setHighlightedIdx(int idx) {
        this.highlightedIdx.set(idx);
        this.nextIdx.set(-1);
        forceRefresh();
        if (idx >= 0 && idx < instructionRows.size()) {
            instructionTable.scrollTo(idx);
        }
    }

    public void setNextIdx(int idx) {
        this.nextIdx.set(idx);
        forceRefresh();
    }

    private void forceRefresh() {
        for (InstructionRow row : instructionRows) {
            String t = row.text.get();
            row.text.set(null);
            row.text.set(t);
        }
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