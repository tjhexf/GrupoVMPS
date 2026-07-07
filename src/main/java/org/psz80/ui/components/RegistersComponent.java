package org.psz80.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.psz80.emulator.cpu.Registers;

public class RegistersComponent {

    private final VBox regsContainer = new VBox();
    private boolean built = false;

    private ObservableList<RegisterRow> rows8bit;
    private ObservableList<RegisterRow> rows16bit;
    private ObservableList<RegisterRow> rowsEspeciais;
    private ObservableList<FlagRow> rowsFlags;

    public RegistersComponent() {
        regsContainer.setFillWidth(true);
        regsContainer.setSpacing(10);
        VBox.setVgrow(regsContainer, Priority.ALWAYS);
    }

    public Node getRoot() {
        if (built) return regsContainer;

        regsContainer.getChildren().clear();

        VBox titleWrapper = new VBox();
        titleWrapper.getChildren().add(new Label("Registradores"));
        regsContainer.getChildren().add(titleWrapper);

        rows8bit = FXCollections.observableArrayList(
            new RegisterRow("A", "00"), new RegisterRow("B", "00"),
            new RegisterRow("C", "00"), new RegisterRow("D", "00"),
            new RegisterRow("E", "00"), new RegisterRow("H", "00"),
            new RegisterRow("L", "00"));
        regsContainer.getChildren().add(createRegisterTable("Registradores Principais (8 bits)", rows8bit));

        rowsFlags = FXCollections.observableArrayList(
            new FlagRow("7", "S", "0"),
            new FlagRow("6", "Z", "0"),
            new FlagRow("5", "5", "0"),
            new FlagRow("4", "H", "0"),
            new FlagRow("3", "3", "0"),
            new FlagRow("2", "P/V", "0"),
            new FlagRow("1", "N", "0"),
            new FlagRow("0", "C", "0"));
        regsContainer.getChildren().add(createFlagTable(rowsFlags));

        rows16bit = FXCollections.observableArrayList(
            new RegisterRow("AF", "0000"), new RegisterRow("BC", "0000"),
            new RegisterRow("DE", "0000"), new RegisterRow("HL", "0000"));
        regsContainer.getChildren().add(createRegisterTable("Registradores de 16 bits (pares)", rows16bit));

        rowsEspeciais = FXCollections.observableArrayList(
            new RegisterRow("PC", "0000"), new RegisterRow("SP", "0000"),
            new RegisterRow("IX", "0000"), new RegisterRow("IY", "0000"));
        regsContainer.getChildren().add(createRegisterTable("Registradores Especiais", rowsEspeciais));

        built = true;
        return regsContainer;
    }

    private VBox createRegisterTable(String title, ObservableList<RegisterRow> rows) {
        VBox section = new VBox();
        section.setSpacing(2);
        section.getChildren().add(new Label(title));

        TableView<RegisterRow> table = new TableView<>();
        TableColumn<RegisterRow, String> nameCol = new TableColumn<>("Registrador");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(80);

        TableColumn<RegisterRow, String> valCol = new TableColumn<>("Valor");
        valCol.setCellValueFactory(c -> c.getValue().valueProperty());
        valCol.setPrefWidth(60);

        table.getColumns().addAll(nameCol, valCol);
        table.setItems(rows);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(100);

        section.getChildren().add(table);
        return section;
    }

    private VBox createFlagTable(ObservableList<FlagRow> rows) {
        VBox section = new VBox();
        section.setSpacing(2);
        section.getChildren().add(new Label("Registrador de Flags (F)"));

        TableView<FlagRow> table = new TableView<>();
        TableColumn<FlagRow, String> bitCol = new TableColumn<>("Bit");
        bitCol.setCellValueFactory(c -> c.getValue().bitProperty());
        bitCol.setPrefWidth(40);

        TableColumn<FlagRow, String> nameCol = new TableColumn<>("Nome");
        nameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        nameCol.setPrefWidth(80);

        TableColumn<FlagRow, String> valCol = new TableColumn<>("Valor");
        valCol.setCellValueFactory(c -> c.getValue().valueProperty());
        valCol.setPrefWidth(50);

        table.getColumns().addAll(bitCol, nameCol, valCol);
        table.setItems(rows);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(100);

        section.getChildren().add(table);
        return section;
    }

    public void refresh(Registers r) {
        if (!built) return;

        rows8bit.get(0).valueProperty().set(String.format("%02X", r.getAF() >> 8));
        rows8bit.get(1).valueProperty().set(String.format("%02X", r.getB()));
        rows8bit.get(2).valueProperty().set(String.format("%02X", r.getC()));
        rows8bit.get(3).valueProperty().set(String.format("%02X", r.getD()));
        rows8bit.get(4).valueProperty().set(String.format("%02X", r.getE()));
        rows8bit.get(5).valueProperty().set(String.format("%02X", r.getH()));
        rows8bit.get(6).valueProperty().set(String.format("%02X", r.getL()));

        rowsFlags.get(0).valueProperty().set(r.getSFlag() ? "1" : "0");
        rowsFlags.get(1).valueProperty().set(r.getZFlag() ? "1" : "0");
        rowsFlags.get(2).valueProperty().set("0");
        rowsFlags.get(3).valueProperty().set(r.getHFlag() ? "1" : "0");
        rowsFlags.get(4).valueProperty().set("0");
        rowsFlags.get(5).valueProperty().set(r.getPVFlag() ? "1" : "0");
        rowsFlags.get(6).valueProperty().set(r.getNFlag() ? "1" : "0");
        rowsFlags.get(7).valueProperty().set(r.getCFlag() ? "1" : "0");

        rows16bit.get(0).valueProperty().set(String.format("%04X", r.getAF()));
        rows16bit.get(1).valueProperty().set(String.format("%04X", r.getBC()));
        rows16bit.get(2).valueProperty().set(String.format("%04X", r.getDE()));
        rows16bit.get(3).valueProperty().set(String.format("%04X", r.getHL()));

        rowsEspeciais.get(0).valueProperty().set(String.format("%04X", r.getPC()));
        rowsEspeciais.get(1).valueProperty().set(String.format("%04X", r.getSP()));
        rowsEspeciais.get(2).valueProperty().set(String.format("%04X", r.getIX()));
        rowsEspeciais.get(3).valueProperty().set(String.format("%04X", r.getIY()));
    }

    public static class RegisterRow {
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty value;

        public RegisterRow(String n, String v) {
            name = new javafx.beans.property.SimpleStringProperty(n);
            value = new javafx.beans.property.SimpleStringProperty(v);
        }

        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty valueProperty() { return value; }
    }

    public static class FlagRow {
        private final javafx.beans.property.SimpleStringProperty bit;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty value;

        public FlagRow(String b, String n, String v) {
            bit = new javafx.beans.property.SimpleStringProperty(b);
            name = new javafx.beans.property.SimpleStringProperty(n);
            value = new javafx.beans.property.SimpleStringProperty(v);
        }

        public javafx.beans.property.StringProperty bitProperty() { return bit; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty valueProperty() { return value; }
    }
}