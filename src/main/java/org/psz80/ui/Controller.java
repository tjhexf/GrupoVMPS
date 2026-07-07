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
import org.psz80.assembler.Assembler;
import org.psz80.emulator.memory.Memory;
import org.psz80.ui.components.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.psz80.assembler.model.Instruction;
import org.psz80.assembler.model.Node;
import org.psz80.assembler.model.Operand;
import org.psz80.emulator.system.Z80System;
import org.psz80.encoder.InstructionTable;
import org.psz80.emulator.cpu.Registers;

public class Controller {

    //jolene: objeto do assembler que recebemos do MainApp
    private final Assembler assembler;
    //jolene: objeto do sistema
    private final Z80System system;
    //jolene: objeto da memória
    private final Memory memory;

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

    private boolean isMounted = false;
    private boolean isRunning = false;
    private boolean isStepMode = false;

    private AnimationTimer timer;
    private int stepsPerFrame = 100;
    private List<Integer> instrPcs = new ArrayList<>();

    public Controller(Assembler assembler, Z80System system) {
        // jolene: construir o assembler
        this.assembler = assembler;
        // jolene: construir o sistema
        this.system = system;
        // jolene: construir a memória
        this.memory = system.getMemory();
        memoryComponent.setMemory(this.memory);
        
        buildUI();
        initializeContent();
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

    private void initializeContent() {
        String sample = "LD A, 0x05\nLD B, 0x03\nADD A, B\nHALT\n";
        editorComponent.createNewTab(null, sample);
        memoryComponent.populate();
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
            byte[] assembled = assembler.assemble(et.area.getText());
            int[] programa = new int[assembled.length];
            for (int i = 0; i < assembled.length; i++) {
                programa[i] = assembled[i] & 0xFF;
            }
            system.reset();
            system.loadProgram(0x0000, programa);
            memoryComponent.refresh();
            registersComponent.refresh(system.getRegisters());
            memoryComponent.setHighlightedPc(system.getRegisters().getPC());
            consoleComponent.appendText("Montagem OK (" + programa.length + " bytes)\n");
            enableExecutionButtons();
            parseInstructions(et);
            highlightCurrentInstruction(system.getRegisters().getPC());
        } catch (Exception ex) {
            consoleComponent.appendText("Erro na montagem: " + ex.getMessage() + "\n");
            disableExecutionButtons();
        }
    }

    private void parseInstructions(EditorComponent.EditorTab et) {
        instructionListComponent.clearInstructions();
        instrPcs.clear();
        try {
            List<Node> nodes = assembler.parse(et.area.getText());
            InstructionTable table = new InstructionTable();
            int pc = 0;
            int idx = 0;
            for (Node node : nodes) {
                if (node instanceof Instruction inst) {
                    instrPcs.add(pc);
                    String text = inst.getMnemonic();
                    if (!inst.getOperands().isEmpty()) {
                        text += " " + inst.getOperands().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
                    }
                    instructionListComponent.addInstruction(idx, text);
                    Operand[] ops = inst.getOperands().toArray(new Operand[0]);
                    pc += table.find(inst).size(ops);
                    idx++;
                }
            }
        } catch (Exception ex) {
            consoleComponent.appendText("Erro ao analisar: " + ex.getMessage() + "\n");
        }
    }

    private void highlightCurrentInstruction(int pc) {
        int idx = -1;
        for (int i = 0; i < instrPcs.size(); i++) {
            if (instrPcs.get(i) <= pc) idx = i;
        }
        instructionListComponent.setHighlightedIdx(idx);
    }

    private void highlightNextInstruction(int pc) {
        int idx = -1;
        for (int i = 0; i < instrPcs.size(); i++) {
            if (instrPcs.get(i) <= pc) idx = i;
        }
        instructionListComponent.setNextIdx(idx);
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
                int startPc = system.getRegisters().getPC();
                for (int i = 0; i < stepsPerFrame; i++) {
                    if (system.isHalted()) {
                        stopSimulation();
                        consoleComponent.appendText("CPU em HALT\n");
                        memoryComponent.refresh();
                        registersComponent.refresh(system.getRegisters());
                        memoryComponent.setHighlightedPc(system.getRegisters().getPC());
                        highlightCurrentInstruction(system.getRegisters().getPC());
                        return;
                    }
                    system.step();
                }
                memoryComponent.refresh();
                registersComponent.refresh(system.getRegisters());
                memoryComponent.setHighlightedPc(startPc);
                memoryComponent.setNextPc(system.getRegisters().getPC());
                highlightCurrentInstruction(startPc);
                highlightNextInstruction(system.getRegisters().getPC());
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
        if (system.isHalted()) {
            consoleComponent.appendText("CPU em HALT\n");
            return;
        }
        int currentPc = system.getRegisters().getPC();
        system.step();
        memoryComponent.refresh();
        registersComponent.refresh(system.getRegisters());
        memoryComponent.setHighlightedPc(currentPc);
        highlightCurrentInstruction(currentPc);
        if (!system.isHalted()) {
            memoryComponent.setNextPc(system.getRegisters().getPC());
            highlightNextInstruction(system.getRegisters().getPC());
        }
    }
}