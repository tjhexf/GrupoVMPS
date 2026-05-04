package org.psz80.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class RegistersComponent {

    private final VBox regsContainer = new VBox();

    public RegistersComponent() {
        regsContainer.setFillWidth(true);
        regsContainer.setSpacing(10);
        VBox.setVgrow(regsContainer, Priority.ALWAYS);
    }

    public Node getRoot() {
        regsContainer.getChildren().clear();

        VBox titleWrapper = new VBox();
        titleWrapper.getChildren().add(new Label("Registradores"));
        regsContainer.getChildren().add(titleWrapper);

        // registradores principais (8 bits)
        VBox main8BitsSection = createSection("Registradores Principais (8 bits)",
            new String[]{"A", "B", "C", "D", "E", "H", "L"},
            new String[]{"00", "00", "00", "00", "00", "00", "00"});
        regsContainer.getChildren().add(main8BitsSection);

        // flags
        VBox flagSection = createFlagTable();
        regsContainer.getChildren().add(flagSection);

        // registradores de 16 bits
        VBox main16BitsSection = createSection("Registradores de 16 bits (pares)",
            new String[]{"AF", "BC", "DE", "HL"},
            new String[]{"0000", "0000", "0000", "0000"});
        regsContainer.getChildren().add(main16BitsSection);

        // registradores especiais
        VBox specialRegsSection = createSection("Registradores Especiais",
            new String[]{"PC", "SP", "IX", "IY"},
            new String[]{"0000", "0000", "0000", "0000"});
        regsContainer.getChildren().add(specialRegsSection);

        return regsContainer;
    }

    private VBox createSection(String title, String[] names, String[] values) {
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

        ObservableList<RegisterRow> rows = FXCollections.observableArrayList();
        for (int i = 0; i < names.length; i++) {
            rows.add(new RegisterRow(names[i], values[i]));
        }
        table.setItems(rows);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(100);

        section.getChildren().add(table);
        return section;
    }

    private VBox createFlagTable() {
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

        ObservableList<FlagRow> rows = FXCollections.observableArrayList(
            new FlagRow("7", "S", "0"),
            new FlagRow("6", "Z", "0"),
            new FlagRow("5", "5", "0"),
            new FlagRow("4", "H", "0"),
            new FlagRow("3", "3", "0"),
            new FlagRow("2", "P/V", "0"),
            new FlagRow("1", "N", "0"),
            new FlagRow("0", "C", "0")
        );
        table.setItems(rows);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(100);

        section.getChildren().add(table);
        return section;
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