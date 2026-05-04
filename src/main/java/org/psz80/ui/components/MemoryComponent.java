package org.psz80.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class MemoryComponent {

    private final TableView<MemoryRow> memoryTable = new TableView<>();
    private final ObservableList<MemoryRow> memoryRows = FXCollections.observableArrayList();
    private final ComboBox<String> memorySelector = new ComboBox<>();
    private final byte[] memory = new byte[65536];

    public MemoryComponent() {
        setupTable();
        setupSelector();
    }

    private void setupTable() {
        TableColumn<MemoryRow, String> addrCol = new TableColumn<>("Endereço");
        addrCol.setCellValueFactory(cell -> cell.getValue().addressProperty());
        memoryTable.getColumns().add(addrCol);

        for (int i = 0; i < 16; i++) {
            final int col = i;
            TableColumn<MemoryRow, String> c = new TableColumn<>(String.format("%X", i));
            c.setCellValueFactory(cell -> cell.getValue().byteProperty(col));
            memoryTable.getColumns().add(c);
        }

        int totalCols = 17;
        addrCol.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
        for (TableColumn<MemoryRow, ?> col : memoryTable.getColumns()) {
            col.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
        }

        memoryTable.setItems(memoryRows);
    }

    private void setupSelector() {
        memorySelector.getItems().addAll("código (.text)", "dados (.data)", "stack");
        memorySelector.getSelectionModel().select(0);
        memorySelector.setPrefWidth(120);
        memorySelector.setOnAction(e -> scrollToRegion());
    }

    private int getStartAddress() {
        String sel = memorySelector.getSelectionModel().getSelectedItem();
        if (sel != null && sel.startsWith("código")) return 0x0000;
        if (sel != null && sel.startsWith("dados")) return 0x4000;
        return 0xD000;
    }

    private void scrollToRegion() {
        int start = getStartAddress();
        int row = start / 16;
        memoryTable.scrollTo(row);
    }

    private void populateAll() {
        memoryRows.clear();
        for (int r = 0; r < 4096; r++) {
            int baseAddr = r * 16;
            MemoryRow mr = new MemoryRow(baseAddr);
            for (int b = 0; b < 16; b++) {
                mr.setByte(b, String.format("%02X", memory[baseAddr + b] & 0xFF));
            }
            memoryRows.add(mr);
        }
    }

    public Node getRoot() {
        VBox container = new VBox();
        container.setFillWidth(true);
        VBox.setVgrow(memoryTable, Priority.ALWAYS);

        HBox header = new HBox();
        header.getChildren().addAll(new Label("Memória "), memorySelector);

        VBox wrapper = new VBox();
        wrapper.setMinHeight(400);
        wrapper.getChildren().addAll(header, memoryTable);
        return wrapper;
    }

    public ComboBox<String> getMemorySelector() {
        return memorySelector;
    }

    public TableView<MemoryRow> getMemoryTable() {
        return memoryTable;
    }

    public void populate(int rows) {
        for (int i = 0; i < 65536; i++) {
            memory[i] = (byte) (i & 0xFF);
        }
        populateAll();
        scrollToRegion();
    }

    public void updateMemory(int address, byte value) {
        memory[address] = value;
        int row = address / 16;
        int col = address % 16;
        if (row < memoryRows.size()) {
            memoryRows.get(row).setByte(col, String.format("%02X", value & 0xFF));
        }
    }

    public void refresh() {
        for (int row = 0; row < memoryRows.size(); row++) {
            MemoryRow mr = memoryRows.get(row);
            int baseAddr = row * 16;
            for (int col = 0; col < 16; col++) {
                mr.setByte(col, String.format("%02X", memory[baseAddr + col] & 0xFF));
            }
        }
    }

    public static class MemoryRow {
        private final StringProperty address = new SimpleStringProperty();
        private final StringProperty[] bytes = new StringProperty[16];

        public MemoryRow(int baseAddress) {
            address.set(String.format("%04X", baseAddress & 0xFFFF));
            for (int i = 0; i < 16; i++) {
                bytes[i] = new SimpleStringProperty("00");
            }
        }

        public StringProperty addressProperty() { return address; }
        public StringProperty byteProperty(int idx) { return bytes[idx]; }
        public void setByte(int idx, String value) { bytes[idx].set(value); }
    }
}