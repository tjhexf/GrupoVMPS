package org.psz80.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

public class Controller {

	private final BorderPane root = new BorderPane();

	private final TabPane editorTabs = new TabPane();
	private final TableView<InstructionRow> instructionTable = new TableView<>();
	private final TableView<MemoryRow> memoryTable = new TableView<>();
	private final ObservableList<MemoryRow> memoryRows = FXCollections.observableArrayList();
	private final TextArea consoleArea = new TextArea();

	// registers
	private final Label lblA = new Label("A: 00");
	private final Label lblB = new Label("B: 00");
	private final Label lblC = new Label("C: 00");
	private final Label lblD = new Label("D: 00");
	private final Label lblE = new Label("E: 00");
	private final Label lblH = new Label("H: 00");
	private final Label lblL = new Label("L: 00");
	private final Label lblPC = new Label("PC: 0000");
	private final Label lblSP = new Label("SP: 0000");

	// F flags
	private final CheckBox flagS = new CheckBox("S");
	private final CheckBox flagZ = new CheckBox("Z");
	private final CheckBox flag5 = new CheckBox("5");
	private final CheckBox flagH = new CheckBox("H");
	private final CheckBox flag3 = new CheckBox("3");
	private final CheckBox flagPV = new CheckBox("P/V");
	private final CheckBox flagN = new CheckBox("N");
	private final CheckBox flagC = new CheckBox("C");

	// toolbar buttons
	private final Button btnNew = new Button("Criar novo");
	private final Button btnOpen = new Button("Abrir arquivo");
	private final Button btnSave = new Button("Salvar arquivo");
	private final Button btnAssemble = new Button("Montar");
	private final Button btnRun = new Button("Executar");
	private final Button btnStop = new Button("Parar");
	private final ToggleButton btnStepMode = new ToggleButton("Executar - step");
	private final Button btnNextStep = new Button("Próximo step");

	// mock memory
	private final byte[] fakeMemory = new byte[65536];
	private int fakePC = 0;
	private final Random rnd = new Random(1234);

	private final java.util.Map<Tab, Boolean> mountedMap = new java.util.HashMap<>();

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
		btnNextStep.setDisable(true);
		ToolBar tb = new ToolBar(btnNew, btnOpen, btnSave, btnAssemble, btnRun, btnStop, btnStepMode, btnNextStep);
		root.setTop(tb);

		// Editor tabs and instructions
		editorTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
		TableColumn<InstructionRow, String> idxCol = new TableColumn<>="#";
		idxCol.setCellValueFactory(c -> c.getValue().indexProperty());
		idxCol.setPrefWidth(50);
		TableColumn<InstructionRow, String> txtCol = new TableColumn<>("Instrução");
		txtCol.setCellValueFactory(c -> c.getValue().textProperty());
		instructionTable.getColumns().addAll(idxCol, txtCol);
		txtCol.prefWidthProperty().bind(instructionTable.widthProperty().subtract(idxCol.widthProperty()).subtract(2));

		instructionTable.setRowFactory(tv -> new TableRow<InstructionRow>() {
			@Override
			protected void updateItem(InstructionRow item, boolean empty) {
				super.updateItem(item, empty);
				getStyleClass().removeAll("current-instr", "next-instr");
				if (empty || item == null) return;
				ObservableList<InstructionRow> items = instructionTable.getItems();
				int cur = items.isEmpty() ? 0 : fakePC % items.size();
				int idx = Integer.parseInt(item.indexProperty().get());
				if (idx == cur) getStyleClass().add("current-instr");
				else if (idx == (cur + 1) % Math.max(1, items.size())) getStyleClass().add("next-instr");
			}
		});

		instructionTable.setPrefHeight(200);

		SplitPane leftVertical = new SplitPane();
		leftVertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
		leftVertical.getItems().addAll(editorTabs, instructionTable);

		// Memory table
		memoryTable.setItems(memoryRows);
		TableColumn<MemoryRow, String> addrCol = new TableColumn<>("Endereço");
		addrCol.setCellValueFactory(cell -> cell.getValue().addressProperty());
		memoryTable.getColumns().add(addrCol);
		final int byteCols = 16;
		for (int i = 0; i < byteCols; i++) {
			final int col = i;
			TableColumn<MemoryRow, String> c = new TableColumn<>(String.format("%X", i));
			c.setCellValueFactory(cell -> cell.getValue().byteProperty(col));
			c.setCellFactory(tc -> new TableCell<MemoryRow, String>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					getStyleClass().removeAll("current-instr", "next-instr");
					if (empty || item == null) { setText(null); return; }
					setText(item);
					int row = getIndex();
					if (row < 0 || row >= memoryRows.size()) return;
					int addr = row * byteCols + col;
					if ((addr & 0xFFFF) == (fakePC & 0xFFFF)) getStyleClass().add("current-instr");
					else if ((addr & 0xFFFF) == ((fakePC + 1) & 0xFFFF)) getStyleClass().add("next-instr");
				}
			});
			memoryTable.getColumns().add(c);
		}
		int totalCols = 1 + byteCols;
		addrCol.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
		for (TableColumn<MemoryRow, ?> col : memoryTable.getColumns()) col.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
		memoryTable.setPrefHeight(240);

		SplitPane centerVertical = new SplitPane();
		centerVertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
		centerVertical.getItems().addAll(memoryTable, consoleArea);

		GridPane regs = new GridPane(); regs.setPadding(new Insets(8)); regs.setHgap(8); regs.setVgap(6);
		regs.add(new Label("Registradores"), 0, 0);
		regs.add(lblA, 0, 1); regs.add(lblB, 0, 2); regs.add(lblC, 0, 3); regs.add(lblD, 0, 4); regs.add(lblE, 0, 5);
		regs.add(lblH, 0, 6); regs.add(lblL, 0, 7); regs.add(lblPC, 0, 8); regs.add(lblSP, 0, 9);

		VBox fbox = new VBox(6); fbox.getChildren().add(new Label("F (flags)"));
		HBox bits = new HBox(6, flagS, flagZ, flag5, flagH, flag3, flagPV, flagN, flagC);
		for (CheckBox cb : new CheckBox[]{flagS, flagZ, flag5, flagH, flag3, flagPV, flagN, flagC}) cb.setDisable(true);
		fbox.getChildren().add(bits); regs.add(fbox, 0, 10);

		SplitPane mainSplit = new SplitPane(); mainSplit.getItems().addAll(leftVertical, centerVertical, regs); mainSplit.setDividerPositions(0.45, 0.9);
		root.setCenter(mainSplit);
	}

	private Parent createLegend() {
		Label cur = new Label(" "); cur.getStyleClass().add("legend-current");
		Label curTxt = new Label(" Instrução atual "); Label next = new Label(" "); next.getStyleClass().add("legend-next");
		Label nextTxt = new Label(" Próxima instrução "); HBox row = new HBox(6, cur, curTxt, next, nextTxt); return row;
	}

	private void populateFakeContent() {
		String sample = ".MODEL SMALL\n.STACK 100H\n.CODE\nMOV AX, 0x3C\nMOV BX, 0x10\nADD AX, BX\nSUB AX, BX\nINT 21H\n";
		createEditorTab(null, sample);
		for (int i = 0; i < 256; i++) fakeMemory[i] = (byte) (i & 0xFF);
		memoryRows.clear();
		for (int r = 0; r < 16; r++) {
			MemoryRow mr = new MemoryRow(r * 16);
			for (int b = 0; b < 16; b++) mr.setByte(b, String.format("%02X", fakeMemory[r * 16 + b] & 0xFF));
			memoryRows.add(mr);
		}
		consoleArea.setText("Console I/O (mock)\n");
	}

	private void attachHandlers() {
		btnNew.setOnAction(e -> createEditorTab(null, ""));
		btnOpen.setOnAction(e -> handleOpenFile());
		btnSave.setOnAction(e -> handleSaveFile());
		btnAssemble.setOnAction(e -> handleAssemble());
		btnRun.setOnAction(e -> startSimulation());
		btnStop.setOnAction(e -> stopSimulation());
		btnStepMode.setOnAction(e -> { boolean step = btnStepMode.isSelected(); btnNextStep.setDisable(!step); if (step && timer != null) { timer.stop(); timer = null; btnRun.setDisable(false); btnStop.setDisable(true); editorTabs.setDisable(false); } });
		btnNextStep.setOnAction(e -> performSingleStep());

		editorTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
			if (newT == null) instructionTable.setItems(FXCollections.emptyObservableList());
			else { EditorTab et = (EditorTab) newT.getUserData(); instructionTable.setItems(et.instructionRows); mountedMap.putIfAbsent(newT, false); }
		});
	}

	private static class EditorTab {
		final Tab tab; final CodeArea area; final ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList(); File file;
		EditorTab(Tab t, CodeArea a, File f) { tab = t; area = a; file = f; }
	}

	private EditorTab getActiveEditorTab() {
		Tab t = editorTabs.getSelectionModel().getSelectedItem();
		if (t == null) return null;
		Object ud = t.getUserData();
		return (ud instanceof EditorTab) ? (EditorTab) ud : null;
	}

	private void createEditorTab(File file, String content) {
		CodeArea area = new CodeArea();
		if (content != null) area.replaceText(content);
		VirtualizedScrollPane<CodeArea> vs = new VirtualizedScrollPane<>(area);
		Tab tab = new Tab((file == null) ? "untitled" + (editorTabs.getTabs().size() + 1) : file.getName(), vs);
		EditorTab et = new EditorTab(tab, area, file);
		tab.setUserData(et);
		tab.setOnClosed(ev -> mountedMap.remove(tab));
		editorTabs.getTabs().add(tab);
		editorTabs.getSelectionModel().select(tab);
		parseInstructionsForEditor(et);
	}

	private void parseInstructionsForEditor(EditorTab et) {
		et.instructionRows.clear();
		String[] lines = et.area.getText().split("\r?\n");
		boolean inCode = false; int idx = 0;
		for (String line : lines) {
			String t = line.trim(); if (t.isEmpty()) continue;
			if (t.startsWith(".CODE")) { inCode = true; continue; }
			if (!inCode) continue;
			et.instructionRows.add(new InstructionRow(idx++, t));
		}
	}

	private void handleOpenFile() {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s"));
		File f = fc.showOpenDialog(root.getScene().getWindow());
		if (f != null) {
			try { List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8); createEditorTab(f, String.join("\n", lines)); }
			catch (Exception ex) { consoleArea.appendText("Erro ao abrir arquivo: " + ex.getMessage() + "\n"); }
		}
	}

	private void handleSaveFile() {
		EditorTab et = getActiveEditorTab(); if (et == null) return;
		try {
			if (et.file == null) { FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s")); File f = fc.showSaveDialog(root.getScene().getWindow()); if (f == null) return; et.file = f; et.tab.setText(f.getName()); }
			Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8));
			consoleArea.appendText("Arquivo salvo: " + et.file.getAbsolutePath() + "\n");
		} catch (Exception ex) { consoleArea.appendText("Erro ao salvar arquivo: " + ex.getMessage() + "\n"); }
	}

	private void handleAssemble() {
		EditorTab et = getActiveEditorTab(); if (et == null) return;
		try { if (et.file == null) { FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s")); File f = fc.showSaveDialog(root.getScene().getWindow()); if (f == null) return; et.file = f; et.tab.setText(f.getName()); } Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8)); }
		catch (Exception ex) { consoleArea.appendText("Erro ao salvar antes de montar: " + ex.getMessage() + "\n"); mountedMap.put(et.tab, false); return; }
		boolean success = !et.area.getText().contains("ERROR");
		if (success) { consoleArea.appendText("Montagem bem-sucedida: " + (et.file != null ? et.file.getName() : "(untitled)") + "\n"); mountedMap.put(et.tab, true); parseInstructionsForEditor(et); fakePC = 0; instructionTable.setItems(et.instructionRows); }
		else { consoleArea.appendText("Erro na montagem: conteúdo inválido\n"); mountedMap.put(et.tab, false); }
	}

	private void startSimulation() {
		if (timer != null) return;
		Tab t = editorTabs.getSelectionModel().getSelectedItem(); if (t == null) return;
		if (!mountedMap.getOrDefault(t, false)) { consoleArea.appendText("Programa não montado nesta aba.\n"); return; }
		btnRun.setDisable(true); btnStop.setDisable(false); editorTabs.setDisable(true);
		timer = new AnimationTimer() {
			@Override public void handle(long now) {
				for (int i = 0; i < stepsPerFrame; i++) fakeStep();
				refreshRegisters(); refreshMemoryArea();
				ObservableList<InstructionRow> items = instructionTable.getItems();
				if (items != null && !items.isEmpty()) { int instrIndex = fakePC % items.size(); instructionTable.getSelectionModel().select(instrIndex); instructionTable.scrollTo(Math.max(0, instrIndex - 4)); instructionTable.refresh(); }
			}
		};
		timer.start();
	}

	private void stopSimulation() { if (timer != null) { timer.stop(); timer = null; } btnRun.setDisable(false); btnStop.setDisable(true); editorTabs.setDisable(false); }

	private void performSingleStep() { fakeStep(); refreshRegisters(); refreshMemoryArea(); ObservableList<InstructionRow> items = instructionTable.getItems(); if (items != null && !items.isEmpty()) { int instrIndex = fakePC % items.size(); instructionTable.getSelectionModel().select(instrIndex); instructionTable.scrollTo(Math.max(0, instrIndex - 4)); instructionTable.refresh(); } }

	private void fakeStep() {
		int idx = fakePC & 0xFFFF;
		byte v = (byte) rnd.nextInt(256);
		fakeMemory[idx] = v;
		int addr8 = idx & 0xFF;
		if (addr8 < 256) { int row = addr8 / 16; int col = addr8 % 16; if (row < memoryRows.size()) memoryRows.get(row).setByte(col, String.format("%02X", v & 0xFF)); }
		fakePC = (fakePC + 1) & 0xFFFF;
		if (rnd.nextInt(10) == 0) { int rv = rnd.nextInt(256); lblA.setText(String.format("A: %02X", rv)); flagZ.setSelected((rv & 1) == 0); flagC.setSelected((rv & 2) != 0); }
	}

	private void refreshRegisters() { lblPC.setText(String.format("PC: %04X", fakePC & 0xFFFF)); }

	public static class InstructionRow { private final StringProperty index = new SimpleStringProperty(); private final StringProperty text = new SimpleStringProperty(); public InstructionRow(int idx, String txt) { index.set(String.format("%02d", idx)); text.set(txt); } public StringProperty indexProperty() { return index; } public StringProperty textProperty() { return text; } }

	public static class MemoryRow { private final StringProperty address = new SimpleStringProperty(); private final StringProperty[] bytes = new StringProperty[16]; public MemoryRow(int baseAddress) { address.set(String.format("%04X", baseAddress & 0xFFFF)); for (int i = 0; i < 16; i++) bytes[i] = new SimpleStringProperty("00"); } public StringProperty addressProperty() { return address; } public StringProperty byteProperty(int idx) { return bytes[idx]; } public void setByte(int idx, String value) { bytes[idx].set(value); } }

	private void refreshMemoryArea() { for (int row = 0; row < memoryRows.size(); row++) { MemoryRow mr = memoryRows.get(row); for (int col = 0; col < 16; col++) { int addr = row * 16 + col; mr.setByte(col, String.format("%02X", fakeMemory[addr] & 0xFF)); } } }

}
package org.psz80.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

/**
 * Controller: programmatic UI mock for the emulator. Supports multiple editor tabs
 * (each backed by a CodeArea), a memory TableView, an instruction TableView, and
 * a toolbar with file actions and execution controls. Everything is mocked so
 * the UI can be developed independently of the emulator backend.
 */
public class Controller {

	private final BorderPane root = new BorderPane();

	// Editor area: tabs with CodeArea
	private final TabPane editorTabs = new TabPane();

	// Instructions table (shows instructions of the active tab)
	private final TableView<InstructionRow> instructionTable = new TableView<>();

	// Memory table
	private final TableView<MemoryRow> memoryTable = new TableView<>();
	private final ObservableList<MemoryRow> memoryRows = FXCollections.observableArrayList();

	// Console (output)
	private final TextArea consoleArea = new TextArea();

	// Registers (mock labels)
	private final Label lblA = new Label("A: 00");
	private final Label lblB = new Label("B: 00");
	private final Label lblC = new Label("C: 00");
	private final Label lblD = new Label("D: 00");
	private final Label lblE = new Label("E: 00");
	private final Label lblH = new Label("H: 00");
	private final Label lblL = new Label("L: 00");
	private final Label lblPC = new Label("PC: 0000");
	private final Label lblSP = new Label("SP: 0000");

	// Flags (F register) - checkboxes (disabled for mock)
	private final javafx.scene.control.CheckBox flagS = new javafx.scene.control.CheckBox("S");
	private final javafx.scene.control.CheckBox flagZ = new javafx.scene.control.CheckBox("Z");
	private final javafx.scene.control.CheckBox flag5 = new javafx.scene.control.CheckBox("5");
	private final javafx.scene.control.CheckBox flagH = new javafx.scene.control.CheckBox("H");
	private final javafx.scene.control.CheckBox flag3 = new javafx.scene.control.CheckBox("3");
	private final javafx.scene.control.CheckBox flagPV = new javafx.scene.control.CheckBox("P/V");
	private final javafx.scene.control.CheckBox flagN = new javafx.scene.control.CheckBox("N");
	private final javafx.scene.control.CheckBox flagC = new javafx.scene.control.CheckBox("C");

	// Buttons (toolbar)
	private final Button btnNew = new Button("Criar novo");
	private final Button btnOpen = new Button("Abrir arquivo");
	private final Button btnSave = new Button("Salvar arquivo");
	private final Button btnAssemble = new Button("Montar");
	private final Button btnRun = new Button("Executar");
	private final Button btnStop = new Button("Parar");
	private final ToggleButton btnStepMode = new ToggleButton("Executar - step");
	private final Button btnNextStep = new Button("Próximo step");

	// Mock memory and execution state
	private final byte[] fakeMemory = new byte[65536];
	private int fakePC = 0;
	private final Random rnd = new Random(1234);

	// per-tab mounted state
	private final java.util.Map<Tab, Boolean> mountedMap = new java.util.HashMap<>();

	private AnimationTimer timer;
	private int stepsPerFrame = 200;

	public Controller() {
		buildUI();
		populateFakeContent();
		attachHandlers();
		// add legend
		((ToolBar) root.getTop()).getItems().add(createLegend());
	}

	public Parent getRoot() { return root; }

	// ---------------- UI construction ----------------
	private void buildUI() {
		btnNextStep.setDisable(true);
		ToolBar tb = new ToolBar(btnNew, btnOpen, btnSave, btnAssemble, btnRun, btnStop, btnStepMode, btnNextStep);
		root.setTop(tb);

		editorTabs.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);

		TableColumn<InstructionRow, String> idxCol = new TableColumn<>("#");
		idxCol.setCellValueFactory((CellDataFeatures<InstructionRow, String> c) -> c.getValue().indexProperty());
		idxCol.setPrefWidth(50);
		TableColumn<InstructionRow, String> txtCol = new TableColumn<>("Instrução");
		txtCol.setCellValueFactory((CellDataFeatures<InstructionRow, String> c) -> c.getValue().textProperty());
		instructionTable.getColumns().addAll(idxCol, txtCol);
		txtCol.prefWidthProperty().bind(instructionTable.widthProperty().subtract(idxCol.widthProperty()).subtract(2));

		instructionTable.setRowFactory(tv -> new TableRow<>() {
			@Override
			protected void updateItem(InstructionRow item, boolean empty) {
				super.updateItem(item, empty);
				getStyleClass().removeAll("current-instr", "next-instr");
				if (empty || item == null) return;
				ObservableList<InstructionRow> items = instructionTable.getItems();
				int cur = items.isEmpty() ? 0 : fakePC % items.size();
				int idx = Integer.parseInt(item.indexProperty().get());
				if (idx == cur) getStyleClass().add("current-instr");
				else if (idx == (cur + 1) % Math.max(1, items.size())) getStyleClass().add("next-instr");
			}
		});

		instructionTable.setPrefHeight(200);
		SplitPane leftVertical = new SplitPane();
		leftVertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
		leftVertical.getItems().addAll(editorTabs, instructionTable);

		memoryTable.setItems(memoryRows);
		TableColumn<MemoryRow, String> addrCol = new TableColumn<>("Endereço");
		addrCol.setCellValueFactory(cell -> cell.getValue().addressProperty());
		memoryTable.getColumns().add(addrCol);
		final int byteCols = 16;
		for (int i = 0; i < byteCols; i++) {
			final int col = i;
			TableColumn<MemoryRow, String> c = new TableColumn<>(String.format("%X", i));
			c.setCellValueFactory(cell -> cell.getValue().byteProperty(col));
			c.setCellFactory(tc -> new TableCell<>() {
				@Override
				protected void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					getStyleClass().removeAll("current-instr", "next-instr");
					if (empty || item == null) { setText(null); return; }
					setText(item);
					int row = getIndex();
					if (row < 0 || row >= memoryRows.size()) return;
					int addr = row * byteCols + col;
					if ((addr & 0xFFFF) == (fakePC & 0xFFFF)) getStyleClass().add("current-instr");
					else if ((addr & 0xFFFF) == ((fakePC + 1) & 0xFFFF)) getStyleClass().add("next-instr");
				}
			});
			memoryTable.getColumns().add(c);
		}
		int totalCols = 1 + byteCols;
		addrCol.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
		for (TableColumn<MemoryRow, ?> col : memoryTable.getColumns()) col.prefWidthProperty().bind(memoryTable.widthProperty().subtract(2).divide(totalCols));
		memoryTable.setPrefHeight(240);

		SplitPane centerVertical = new SplitPane();
		centerVertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
		centerVertical.getItems().addAll(memoryTable, consoleArea);

		GridPane regs = new GridPane();
		regs.setPadding(new Insets(8)); regs.setHgap(8); regs.setVgap(6);
		regs.add(new Label("Registradores"), 0, 0);
		regs.add(lblA, 0, 1); regs.add(lblB, 0, 2); regs.add(lblC, 0, 3); regs.add(lblD, 0, 4); regs.add(lblE, 0, 5);
		regs.add(lblH, 0, 6); regs.add(lblL, 0, 7); regs.add(lblPC, 0, 8); regs.add(lblSP, 0, 9);

		VBox fbox = new VBox(6); fbox.getChildren().add(new Label("F (flags)"));
		HBox bits = new HBox(6, flagS, flagZ, flag5, flagH, flag3, flagPV, flagN, flagC);
		for (javafx.scene.control.CheckBox cb : new javafx.scene.control.CheckBox[]{flagS, flagZ, flag5, flagH, flag3, flagPV, flagN, flagC}) cb.setDisable(true);
		fbox.getChildren().add(bits); regs.add(fbox, 0, 10);

		SplitPane mainSplit = new SplitPane(); mainSplit.getItems().addAll(leftVertical, centerVertical, regs); mainSplit.setDividerPositions(0.45, 0.9);
		root.setCenter(mainSplit);
	}

	private Parent createLegend() {
		Label cur = new Label(" "); cur.getStyleClass().add("legend-current");
		Label curTxt = new Label(" Instrução atual "); Label next = new Label(" "); next.getStyleClass().add("legend-next");
		Label nextTxt = new Label(" Próxima instrução "); HBox row = new HBox(6, cur, curTxt, next, nextTxt); return row;
	}

	// ---------------- content / sample population ----------------
	private void populateFakeContent() {
		String sample = ".MODEL SMALL\n.STACK 100H\n.CODE\nMOV AX, 0x3C\nMOV BX, 0x10\nADD AX, BX\nSUB AX, BX\nINT 21H\n";
		createEditorTab(null, sample);
		for (int i = 0; i < 256; i++) fakeMemory[i] = (byte) (i & 0xFF);
		memoryRows.clear(); for (int r = 0; r < 16; r++) { MemoryRow mr = new MemoryRow(r * 16); for (int b = 0; b < 16; b++) mr.setByte(b, String.format("%02X", fakeMemory[r * 16 + b] & 0xFF)); memoryRows.add(mr); }
		consoleArea.setText("Console I/O (mock)\n");
	}

	// ---------------- handlers ----------------
	private void attachHandlers() {
		btnNew.setOnAction(e -> createEditorTab(null, "")); btnOpen.setOnAction(e -> handleOpenFile()); btnSave.setOnAction(e -> handleSaveFile());
		btnAssemble.setOnAction(e -> handleAssemble()); btnRun.setOnAction(e -> startSimulation()); btnStop.setOnAction(e -> stopSimulation());
		btnStepMode.setOnAction(e -> { boolean step = btnStepMode.isSelected(); btnNextStep.setDisable(!step); if (step && timer != null) { timer.stop(); timer = null; btnRun.setDisable(false); btnStop.setDisable(true); editorTabs.setDisable(false); } });
		btnNextStep.setOnAction(e -> performSingleStep());
		editorTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> { if (newT == null) instructionTable.setItems(FXCollections.emptyObservableList()); else { EditorTab et = (EditorTab) newT.getUserData(); instructionTable.setItems(et.instructionRows); mountedMap.putIfAbsent(newT, false); } });
	}

	// ---------------- editor / tab management ----------------
	private static class EditorTab { final Tab tab; final CodeArea area; final ObservableList<InstructionRow> instructionRows = FXCollections.observableArrayList(); File file; EditorTab(Tab t, CodeArea a, File f) { tab = t; area = a; file = f; } }
	private EditorTab getActiveEditorTab() { Tab t = editorTabs.getSelectionModel().getSelectedItem(); if (t == null) return null; Object ud = t.getUserData(); return (ud instanceof EditorTab) ? (EditorTab) ud : null; }
	private void createEditorTab(File file, String content) { CodeArea area = new CodeArea(); if (content != null) area.replaceText(content); VirtualizedScrollPane<CodeArea> vs = new VirtualizedScrollPane<>(area); Tab tab = new Tab((file == null) ? "untitled" + (editorTabs.getTabs().size() + 1) : file.getName(), vs); EditorTab et = new EditorTab(tab, area, file); tab.setUserData(et); tab.setOnClosed(ev -> mountedMap.remove(tab)); editorTabs.getTabs().add(tab); editorTabs.getSelectionModel().select(tab); parseInstructionsForEditor(et); }
	private void parseInstructionsForEditor(EditorTab et) { et.instructionRows.clear(); String[] lines = et.area.getText().split("\r?\n"); boolean inCode = false; int idx = 0; for (String line : lines) { String t = line.trim(); if (t.isEmpty()) continue; if (t.startsWith(".CODE")) { inCode = true; continue; } if (!inCode) continue; et.instructionRows.add(new InstructionRow(idx++, t)); } }
	private void handleOpenFile() { FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s")); File f = fc.showOpenDialog(root.getScene().getWindow()); if (f != null) { try { List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8); createEditorTab(f, String.join("\n", lines)); } catch (Exception ex) { consoleArea.appendText("Erro ao abrir arquivo: " + ex.getMessage() + "\n"); } } }
	private void handleSaveFile() { EditorTab et = getActiveEditorTab(); if (et == null) return; try { if (et.file == null) { FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s")); File f = fc.showSaveDialog(root.getScene().getWindow()); if (f == null) return; et.file = f; et.tab.setText(f.getName()); } Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8)); consoleArea.appendText("Arquivo salvo: " + et.file.getAbsolutePath() + "\n"); } catch (Exception ex) { consoleArea.appendText("Erro ao salvar arquivo: " + ex.getMessage() + "\n"); } }
	private void handleAssemble() { EditorTab et = getActiveEditorTab(); if (et == null) return; try { if (et.file == null) { FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ASM files", "*.asm", "*.s")); File f = fc.showSaveDialog(root.getScene().getWindow()); if (f == null) return; et.file = f; et.tab.setText(f.getName()); } Files.write(et.file.toPath(), et.area.getText().getBytes(StandardCharsets.UTF_8)); } catch (Exception ex) { consoleArea.appendText("Erro ao salvar antes de montar: " + ex.getMessage() + "\n"); mountedMap.put(et.tab, false); return; } boolean success = !et.area.getText().contains("ERROR"); if (success) { consoleArea.appendText("Montagem bem-sucedida: " + (et.file != null ? et.file.getName() : "(untitled)") + "\n"); mountedMap.put(et.tab, true); parseInstructionsForEditor(et); fakePC = 0; instructionTable.setItems(et.instructionRows); } else { consoleArea.appendText("Erro na montagem: conteúdo inválido\n"); mountedMap.put(et.tab, false); } }

	// ---------------- execution (mock) ----------------
	private void startSimulation() { if (timer != null) return; Tab t = editorTabs.getSelectionModel().getSelectedItem(); if (t == null) return; if (!mountedMap.getOrDefault(t, false)) { consoleArea.appendText("Programa não montado nesta aba.\n"); return; } btnRun.setDisable(true); btnStop.setDisable(false); editorTabs.setDisable(true); timer = new AnimationTimer() { @Override public void handle(long now) { for (int i = 0; i < stepsPerFrame; i++) fakeStep(); refreshRegisters(); refreshMemoryArea(); ObservableList<InstructionRow> items = instructionTable.getItems(); if (items != null && !items.isEmpty()) { int instrIndex = fakePC % items.size(); instructionTable.getSelectionModel().select(instrIndex); instructionTable.scrollTo(Math.max(0, instrIndex - 4)); instructionTable.refresh(); } } }; timer.start(); }
	private void stopSimulation() { if (timer != null) { timer.stop(); timer = null; } btnRun.setDisable(false); btnStop.setDisable(true); editorTabs.setDisable(false); }
	private void performSingleStep() { fakeStep(); refreshRegisters(); refreshMemoryArea(); ObservableList<InstructionRow> items = instructionTable.getItems(); if (items != null && !items.isEmpty()) { int instrIndex = fakePC % items.size(); instructionTable.getSelectionModel().select(instrIndex); instructionTable.scrollTo(Math.max(0, instrIndex - 4)); instructionTable.refresh(); } }
	private void fakeStep() { int idx = fakePC & 0xFFFF; byte v = (byte) rnd.nextInt(256); fakeMemory[idx] = v; int addr8 = idx & 0xFF; if (addr8 < 256) { int row = addr8 / 16; int col = addr8 % 16; if (row < memoryRows.size()) memoryRows.get(row).setByte(col, String.format("%02X", v & 0xFF)); } fakePC = (fakePC + 1) & 0xFFFF; if (rnd.nextInt(10) == 0) { int rv = rnd.nextInt(256); lblA.setText(String.format("A: %02X", rv)); flagZ.setSelected((rv & 1) == 0); flagC.setSelected((rv & 2) != 0); } }
	private void refreshRegisters() { lblPC.setText(String.format("PC: %04X", fakePC & 0xFFFF)); }

	// ---------------- helper classes ----------------
	public static class InstructionRow { private final StringProperty index = new SimpleStringProperty(); private final StringProperty text = new SimpleStringProperty(); public InstructionRow(int idx, String txt) { index.set(String.format("%02d", idx)); text.set(txt); } public StringProperty indexProperty() { return index; } public StringProperty textProperty() { return text; } }
	public static class MemoryRow { private final StringProperty address = new SimpleStringProperty(); private final StringProperty[] bytes = new StringProperty[16]; public MemoryRow(int baseAddress) { address.set(String.format("%04X", baseAddress & 0xFFFF)); for (int i = 0; i < 16; i++) bytes[i] = new SimpleStringProperty("00"); } public StringProperty addressProperty() { return address; } public StringProperty byteProperty(int idx) { return bytes[idx]; } public void setByte(int idx, String value) { bytes[idx].set(value); } }

	private void refreshMemoryArea() { for (int row = 0; row < memoryRows.size(); row++) { MemoryRow mr = memoryRows.get(row); for (int col = 0; col < 16; col++) { int addr = row * 16 + col; mr.setByte(col, String.format("%02X", fakeMemory[addr] & 0xFF)); } } }

}
	// ensure memoryRows reflect entire displayed memory slice
	private void refreshMemoryArea() {
		for (int row = 0; row < memoryRows.size(); row++) {
			MemoryRow mr = memoryRows.get(row);
			for (int col = 0; col < 16; col++) {
				int addr = row * 16 + col;
				mr.setByte(col, String.format("%02X", fakeMemory[addr] & 0xFF));
			}
		}
	}

}
				refreshMemoryArea();
				// scroll instructions to current PC index and refresh table to update styles
				ObservableList<InstructionRow> items = instructionTable.getItems();
				if (items != null && !items.isEmpty()) {
					int instrIndex = fakePC % items.size();
					instructionTable.getSelectionModel().select(instrIndex);
					instructionTable.scrollTo(Math.max(0, instrIndex - 4));
					instructionTable.refresh();
				}
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
		editorTabs.setDisable(false);
	}

	private void fakeStep() {
		// modify memory at PC and advance PC
		int idx = fakePC & 0xFF;
		byte v = (byte) (rnd.nextInt(256));
		fakeMemory[idx] = v;
		// update memoryRows corresponding cell
		int row = idx / 16;
		int col = idx % 16;
		if (row < memoryRows.size()) {
			memoryRows.get(row).setByte(col, String.format("%02X", v & 0xFF));
		}
		fakePC = (fakePC + 1) & 0xFFFF;
		// randomly change some registers
		if (rnd.nextInt(10) == 0) {
			int rv = rnd.nextInt(256);
			lblA.setText(String.format("A: %02X", rv));
		}
	}

	private void refreshRegisters() {
		lblPC.setText(String.format("PC: %04X", fakePC & 0xFFFF));
		// other labels may be updated by fakeStep occasionally
	}

	// perform a single step (used by step mode)
	private void performSingleStep() {
		fakeStep();
		refreshRegisters();
		refreshMemoryArea();
		ObservableList<InstructionRow> items = instructionTable.getItems();
		if (items != null && !items.isEmpty()) {
			int instrIndex = fakePC % items.size();
			instructionTable.getSelectionModel().select(instrIndex);
			instructionTable.scrollTo(Math.max(0, instrIndex - 4));
			instructionTable.refresh();
		}
	}

	// MemoryRow nested class represents a row with address and 16 byte string properties
	public static class MemoryRow {
		private final StringProperty address = new SimpleStringProperty();
		private final StringProperty[] bytes = new StringProperty[16];

		public MemoryRow(int baseAddress) {
			address.set(String.format("%04X", baseAddress & 0xFFFF));
			for (int i = 0; i < 16; i++) {
				bytes[i] = new SimpleStringProperty("00");
			}
		}

		public StringProperty addressProperty() {
			return address;
		}

                    


		public StringProperty byteProperty(int idx) {
			return bytes[idx];
		}

		public void setByte(int idx, String value) {
			bytes[idx].set(value);
		}
	}

	private void refreshMemoryArea() {
		// TableView is bound to memoryRows; refreshing not required but keep method
		// for API compatibility. We can ensure rows match backing array.
		for (int row = 0; row < memoryRows.size(); row++) {
			MemoryRow mr = memoryRows.get(row);
			for (int col = 0; col < 16; col++) {
				int addr = row * 16 + col;
				mr.setByte(col, String.format("%02X", fakeMemory[addr] & 0xFF));
			}
		}
	}
}

