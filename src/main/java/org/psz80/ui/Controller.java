package org.psz80.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.psz80.ui.components.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

public class Controller {

    private final BorderPane root = new BorderPane();

    private final EditorComponent editorComponent = new EditorComponent();
    private final MemoryComponent memoryComponent = new MemoryComponent();
    private final ConsoleComponent consoleComponent = new ConsoleComponent();
    private final InstructionListComponent instructionListComponent = new InstructionListComponent();
    private final RegistersComponent registersComponent = new RegistersComponent();

    private final Button btnNew = new Button("Criar novo");
    private final Button btnOpen = new Button("Abrir arquivo");
    private final Button btnSave = new Button("Salvar arquivo");
    private final Button btnAssemble = new Button("Montar");
    private final Button btnRun = new Button("Executar");
    private final Button btnStop = new Button("Parar");
    private final ToggleButton btnStepMode = new ToggleButton("Executar - step");
    private final Button btnNextStep = new Button("Próximo step");

    private final byte[] fakeMemory = new byte[65536];
    private int fakePC = 0;
    private final Random rnd = new Random(1234);

    private boolean isMounted = false;
    private boolean isRunning = false;
    private boolean isStepMode = false;

    private AnimationTimer timer;
    private int stepsPerFrame = 100;

    public Controller() {
        buildUI();
        populateFakeContent();
        attachHandlers();
        ((ToolBar) root.getTop()).getItems().add(createLegend());
    }

    public Parent getRoot() { return root; }

    private void buildUI() {
        disableExecutionButtons();

        ToolBar tb = new ToolBar(btnNew, btnOpen, btnSave, btnAssemble, btnRun, btnStop, btnStepMode, btnNextStep);
        root.setTop(tb);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setDividerPositions(0.3, 0.65);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        HBox.setHgrow(mainSplit, Priority.ALWAYS);

        // coluna esquerda: editor + console
        SplitPane leftColumn = new SplitPane();
        leftColumn.setOrientation(Orientation.VERTICAL);
        leftColumn.setDividerPositions(0.5);
        VBox.setVgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        leftColumn.getItems().addAll(
            editorComponent.getRoot(),
            consoleComponent.getRoot()
        );

        // coluna meio - memória + instruções
        SplitPane middleColumn = new SplitPane();
        middleColumn.setOrientation(Orientation.VERTICAL);
        middleColumn.setDividerPositions(0.5);
        VBox.setVgrow(middleColumn, Priority.ALWAYS);
        HBox.setHgrow(middleColumn, Priority.ALWAYS);

        middleColumn.getItems().addAll(
            memoryComponent.getRoot(),
            instructionListComponent.getRoot()
        );

        // coluna direita - registradores
        VBox rightColumn = new VBox();
        rightColumn.setFillWidth(true);
        rightColumn.setSpacing(5);
        VBox.setVgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        rightColumn.getChildren().add(registersComponent.getRoot());

        mainSplit.getItems().addAll(leftColumn, middleColumn, rightColumn);
        root.setCenter(mainSplit);
    }

    private javafx.scene.Node createLegend() {
        Label cur = new Label(" ");
        cur.getStyleClass().add("legend-current");
        Label curTxt = new Label(" Instrução atual ");
        Label next = new Label(" ");
        next.getStyleClass().add("legend-next");
        Label nextTxt = new Label(" Próxima instrução ");
        HBox row = new HBox(6, cur, curTxt, next, nextTxt);
        return row;
    }

    private void populateFakeContent() {
        String sample = ".MODEL SMALL\n.STACK 100H\n.CODE\nMOV AX, 0x3C\nMOV BX, 0x10\nADD AX, BX\nSUB AX, BX\nINT 21H\n";
        editorComponent.createNewTab(null, sample);
        memoryComponent.populate(256);
        consoleComponent.getConsoleArea().setText("Console I/O (mock)\n");
    }

    private void disableExecutionButtons() {
        isMounted = false;
        btnRun.setDisable(true);
        btnStop.setDisable(true);
        btnStepMode.setDisable(true);
        btnStepMode.setSelected(false);
        btnNextStep.setDisable(true);
    }

    private void enableExecutionButtons() {
        isMounted = true;
        btnRun.setDisable(false);
        btnStepMode.setDisable(false);
    }

    private void attachHandlers() {
        btnNew.setOnAction(e -> {
            editorComponent.createNewTab(null, "");
            disableExecutionButtons();
        });
        btnOpen.setOnAction(e -> handleOpenFile());
        btnSave.setOnAction(e -> handleSaveFile());
        btnAssemble.setOnAction(e -> handleAssemble());
        btnRun.setOnAction(e -> {
            isRunning = true;
            isStepMode = false;
            btnRun.setDisable(true);
            btnStepMode.setDisable(true);
            btnStepMode.setSelected(false);
            btnNextStep.setDisable(true);
            btnStop.setDisable(false);
            startSimulation();
        });
        btnStop.setOnAction(e -> {
            isRunning = false;
            isStepMode = false;
            stopSimulation();
            btnRun.setDisable(false);
            btnStop.setDisable(true);
            btnStepMode.setDisable(false);
        });
        btnStepMode.setOnAction(e -> {
            isStepMode = btnStepMode.isSelected();
            if (isStepMode) {
                isRunning = false;
                if (timer != null) {
                    timer.stop();
                    timer = null;
                    editorComponent.getTabs().setDisable(false);
                }
                btnRun.setDisable(true);
                btnNextStep.setDisable(false);
                btnStop.setDisable(true);
            } else {
                btnRun.setDisable(false);
                btnNextStep.setDisable(true);
            }
        });
        btnNextStep.setOnAction(e -> {
            isStepMode = true;
            performSingleStep();
        });
    }

    private void handleOpenFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s"));
        File f = fc.showOpenDialog(root.getScene().getWindow());
        if (f != null) {
            editorComponent.loadFile(f);
            disableExecutionButtons();
        }
    }

    private void handleSaveFile() {
        EditorComponent.EditorTab et = editorComponent.getActiveEditorTab();
        if (et == null) return;
        try {
            if (et.file == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s"));
                File f = fc.showSaveDialog(root.getScene().getWindow());
                if (f == null) return;
                et.file = f;
            }
            editorComponent.saveFile(et);
            consoleComponent.appendText("Arquivo salvo\n");
        } catch (Exception ex) {
            consoleComponent.appendText("Erro ao salvar: " + ex.getMessage() + "\n");
        }
    }

    private void handleAssemble() {
        EditorComponent.EditorTab et = editorComponent.getActiveEditorTab();
        if (et == null) return;
        try {
            if (et.file == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s"));
                File f = fc.showSaveDialog(root.getScene().getWindow());
                if (f == null) return;
                et.file = f;
            }
            Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            consoleComponent.appendText("Erro ao salvar: " + ex.getMessage() + "\n");
            return;
        }
        boolean success = !et.area.getText().contains("ERROR");
        if (success) {
            consoleComponent.appendText("Montagem bem-sucedida\n");
            enableExecutionButtons();
            parseInstructions(et);
        } else {
            consoleComponent.appendText("Erro na montagem\n");
            disableExecutionButtons();
        }
    }

    private void parseInstructions(EditorComponent.EditorTab et) {
        et.getInstructionRows().clear();
        instructionListComponent.clearInstructions();
        String[] lines = et.area.getText().split("\r?\n");
        boolean inCode = false;
        int idx = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith(".CODE")) {
                inCode = true;
                continue;
            }
            if (!inCode) continue;
            EditorComponent.InstructionRow er = new EditorComponent.InstructionRow(idx, t);
            et.getInstructionRows().add(er);
            instructionListComponent.addInstruction(idx, t);
            idx++;
        }
    }

    private void startSimulation() {
        if (timer != null) return;
        if (!isMounted) {
            consoleComponent.appendText("Programa não montado\n");
            return;
        }
        editorComponent.getTabs().setDisable(true);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (int i = 0; i < stepsPerFrame; i++) fakeStep();
                memoryComponent.refresh();
            }
        };
        timer.start();
    }

    private void stopSimulation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        btnRun.setDisable(false);
        btnStop.setDisable(true);
        editorComponent.getTabs().setDisable(false);
    }

    private void performSingleStep() {
        fakeStep();
        memoryComponent.refresh();
    }

    private void fakeStep() {
        int idx = fakePC & 0xFFFF;
        byte v = (byte) rnd.nextInt(256);
        fakeMemory[idx] = v;
        memoryComponent.updateMemory(idx, v);
        fakePC = (fakePC + 1) & 0xFFFF;
    }
}