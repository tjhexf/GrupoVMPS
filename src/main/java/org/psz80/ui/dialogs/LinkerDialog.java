package org.psz80.ui.dialogs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.psz80.assembler.Assembler;
import org.psz80.linker.*;

import java.io.File;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LinkerDialog {

    private final Assembler assembler;
    private final BiConsumer<LinkedProgram, List<String>> onLinked;
    private final Consumer<List<File>> onOpenFiles;
    private final ObservableList<ModuleEntry> modules = FXCollections.observableArrayList();
    private final ListView<ModuleEntry> moduleList = new ListView<>(modules);
    private final ComboBox<LinkerMode> modeCombo = new ComboBox<>();
    private final TextField addressField = new TextField("0x0000");
    private final TextArea logArea = new TextArea();
    private final CheckBox openFilesCheck = new CheckBox("Abrir módulos no editor após linkar");

    private LinkedProgram linkedProgram;
    private boolean linkedOk = false;

    public LinkerDialog(Assembler assembler, BiConsumer<LinkedProgram, List<String>> onLinked, Consumer<List<File>> onOpenFiles) {
        this.assembler = assembler;
        this.onLinked = onLinked;
        this.onOpenFiles = onOpenFiles;
    }

    public void showDialog(Window owner) {
        Stage stage = new Stage();
        stage.setTitle("Linkar módulos");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(true);

        Button linkButton = new Button("Linkar e carregar");
        Button closeButton = new Button("Fechar");
        openFilesCheck.setSelected(false);
        HBox actionButtons = new HBox(10, linkButton, closeButton, openFilesCheck);
        actionButtons.setPadding(new Insets(5, 0, 0, 0));
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        VBox.setVgrow(logArea, Priority.ALWAYS);

        moduleList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ModuleEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name);
            }
        });
        moduleList.setPrefHeight(120);

        HBox buttons = new HBox(8);
        Button addButton = new Button("Adicionar .asm");
        Button removeButton = new Button("Remover");
        Button upButton = new Button("↑");
        Button downButton = new Button("↓");
        buttons.getChildren().addAll(addButton, removeButton, upButton, downButton);

        modeCombo.getItems().addAll(LinkerMode.LINK_ONLY, LinkerMode.LINK_AND_RELOCATE);
        modeCombo.getSelectionModel().select(LinkerMode.LINK_AND_RELOCATE);

        HBox options = new HBox(10);
        options.getChildren().addAll(new Label("Modo:"), modeCombo, new Label("Endereço de carga:"), addressField);
        HBox.setHgrow(addressField, Priority.ALWAYS);

        logArea.setEditable(false);
        logArea.setPrefHeight(100);
        logArea.setWrapText(true);
        logArea.setText("Adicione os módulos e clique em 'Linkar e carregar'.\n");

        content.getChildren().addAll(
            new Label("Módulos (ordem importa):"),
            moduleList,
            buttons,
            options,
            new Label("Log:"),
            logArea,
            actionButtons
        );

        Scene scene = new Scene(content, 600, 480);
        stage.setScene(scene);

        addButton.setOnAction(e -> handleAddModule(stage));
        removeButton.setOnAction(e -> handleRemoveModule());
        upButton.setOnAction(e -> moveModule(-1));
        downButton.setOnAction(e -> moveModule(1));

        linkButton.setOnAction(e -> performLinkAndLoad());
        closeButton.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private void handleAddModule(Window owner) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s"));
        fc.setTitle("Adicionar módulo .asm");
        List<File> files = fc.showOpenMultipleDialog(owner);
        if (files == null) return;

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                String name = file.getName();
                modules.add(new ModuleEntry(name, file, content));
            } catch (Exception ex) {
                log("Erro ao ler " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void handleRemoveModule() {
        int idx = moduleList.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            modules.remove(idx);
        }
    }

    private void moveModule(int delta) {
        int idx = moduleList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        int newIdx = idx + delta;
        if (newIdx < 0 || newIdx >= modules.size()) return;
        ModuleEntry item = modules.remove(idx);
        modules.add(newIdx, item);
        moduleList.getSelectionModel().select(newIdx);
    }

    private void performLinkAndLoad() {
        linkedProgram = null;
        linkedOk = false;
        logArea.clear();

        if (modules.isEmpty()) {
            log("Nenhum módulo adicionado.");
            return;
        }

        LinkerMode mode = modeCombo.getSelectionModel().getSelectedItem();
        int loadAddress;
        try {
            loadAddress = parseAddress(addressField.getText());
        } catch (NumberFormatException ex) {
            log("Endereço de carga inválido: " + addressField.getText());
            return;
        }

        List<ObjectModule> objectModules = new ArrayList<>();
        try {
            for (ModuleEntry entry : modules) {
                log("Montando objeto: " + entry.name);
                ObjectModule module = assembler.assembleObject(entry.name, entry.content);
                objectModules.add(module);
                log("  → " + module.size() + " bytes, " + module.getSymbols().size() + " símbolos, "
                    + module.getRelocations().size() + " relocações");
            }

            Linker linker = new Linker();
            linkedProgram = linker.link(objectModules, mode, loadAddress);
            log("Linkagem OK: " + linkedProgram.getBytes().length + " bytes");

            if (mode == LinkerMode.LINK_ONLY) {
                log("Modo LINK_ONLY: relocações pendentes = " + linkedProgram.getLoaderRelocations().size());
            } else {
                log("Modo LINK_AND_RELOCATE: endereço base 0x" + String.format("%04X", loadAddress & 0xFFFF));
            }

            List<String> contents = new ArrayList<>();
            for (ModuleEntry entry : modules) {
                contents.add(entry.content);
            }
            onLinked.accept(linkedProgram, contents);
            linkedOk = true;

            if (openFilesCheck.isSelected()) {
                List<File> files = new ArrayList<>();
                for (ModuleEntry entry : modules) {
                    files.add(entry.file);
                }
                onOpenFiles.accept(files);
                log("✓ Programa carregado e módulos abertos no editor.");
            } else {
                log("✓ Programa carregado no emulador. Clique em 'Fechar' para voltar.");
            }
        } catch (Exception ex) {
            log("Erro na linkagem: " + ex.getMessage());
        }
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    private static int parseAddress(String text) {
        String t = text.trim().toLowerCase();
        if (t.startsWith("0x")) {
            return Integer.parseInt(t.substring(2), 16);
        }
        if (t.endsWith("h")) {
            return Integer.parseInt(t.substring(0, t.length() - 1), 16);
        }
        return Integer.parseInt(t);
    }

    private static class ModuleEntry {
        final String name;
        final File file;
        final String content;

        ModuleEntry(String name, File file, String content) {
            this.name = name;
            this.file = file;
            this.content = content;
        }
    }
}
