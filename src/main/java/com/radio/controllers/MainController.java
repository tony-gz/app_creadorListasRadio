package com.radio.controllers;

import com.radio.models.*;
import com.radio.services.ArchivoService;
import com.radio.services.GeneradorListaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;

public class MainController implements Initializable {

    // Campos de carpetas especiales
    @FXML private TextField txtElementosEspeciales;
    @FXML private TextField txtIdentificaciones;
    @FXML private TextField txtFelicitaciones;  // NUEVO
    @FXML private TextField txtPromosA;         // NUEVO (reemplaza txtPromos)
    @FXML private TextField txtPromosB;         // NUEVO


    // Botones de carpetas especiales
    @FXML private Button btnElementosEspeciales;
    @FXML private Button btnIdentificaciones;
    @FXML private Button btnFelicitaciones;     // NUEVO
    @FXML private Button btnPromosA;            // NUEVO (reemplaza btnPromos)
    @FXML private Button btnPromosB;            // NUEVO

    // Tabla de horarios
    @FXML private TableView<ConfiguracionHora> tablaHoras;
    @FXML private TableColumn<ConfiguracionHora, String> colHora;
    @FXML private TableColumn<ConfiguracionHora, String> colGenero;
    @FXML private TableColumn<ConfiguracionHora, String> colCarpeta;
    @FXML private TableColumn<ConfiguracionHora, String> colAcciones;

    // Botones de acción
    @FXML private Button btnInicializarHoras;
    @FXML private Button btnCargarConfiguracion;
    @FXML private Button btnGuardarConfiguracion;
    @FXML private Button btnGenerarLista;
    @FXML private Button btnVistaPrevia;
    @FXML private Button btnExportarLST;
    @FXML private Button btnLimpiar;

    // Estado
    @FXML private Label lblEstado;

    // Servicios
    private GeneradorListaService generadorService;
    private ListaReproduccion listaGenerada;

    // Datos de la tabla
    private ObservableList<ConfiguracionHora> datosHoras;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        generadorService = new GeneradorListaService();
        datosHoras = FXCollections.observableArrayList();

        configurarTabla();
        inicializarHorarios();
        actualizarEstado("Aplicación iniciada. Configure las carpetas y géneros.");
    }

    private void configurarTabla() {
        // Configurar columnas básicas
        colHora.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getHoraTexto()));
        colGenero.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGenero().getNombre()));
        colCarpeta.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRutaCarpeta()));

        // NUEVO: Hacer editable la columna de hora
        colHora.setCellFactory(TextFieldTableCell.forTableColumn());
        colHora.setOnEditCommit(event -> {
            ConfiguracionHora config = event.getRowValue();
            String nuevoHorario = event.getNewValue();

            if (procesarCambioHorario(config, nuevoHorario)) {
                actualizarHorariosSecuenciales(config);
                tablaHoras.refresh();
                actualizarEstado("Horario actualizado y horarios siguientes ajustados automáticamente.");
            } else {
                mostrarAlerta("Error", "Formato de horario inválido. Use formato: HH:MM - HH:MM");
                tablaHoras.refresh(); // Revertir cambio
            }
        });

        // Configurar ComboBox para géneros (código existente)
        ObservableList<String> generosNombres = FXCollections.observableArrayList();
        for (Genero genero : Genero.values()) {
            generosNombres.add(genero.getNombre());
        }

        colGenero.setCellFactory(ComboBoxTableCell.forTableColumn(generosNombres));
        colGenero.setOnEditCommit(event -> {
            ConfiguracionHora config = event.getRowValue();
            String nuevoGeneroNombre = event.getNewValue();

            for (Genero genero : Genero.values()) {
                if (genero.getNombre().equals(nuevoGeneroNombre)) {
                    config.setGenero(genero);
                    break;
                }
            }
            tablaHoras.refresh();
        });

        // Configurar botones de carpeta (código existente)
        colAcciones.setCellFactory(col -> new TableCell<ConfiguracionHora, String>() {
            private final Button btnSeleccionar = new Button("...");

            {
                btnSeleccionar.setOnAction(event -> {
                    ConfiguracionHora config = getTableView().getItems().get(getIndex());
                    seleccionarCarpetaGenero(config);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnSeleccionar);
                }
            }
        });

        tablaHoras.setItems(datosHoras);
        tablaHoras.setEditable(true);
    }

    // NUEVO: Método para procesar cambio de horario
    private boolean procesarCambioHorario(ConfiguracionHora config, String nuevoHorario) {
        try {
            // Formato esperado: "HH:MM - HH:MM"
            String[] partes = nuevoHorario.split(" - ");
            if (partes.length != 2) {
                return false;
            }

            LocalTime nuevaHoraInicio = LocalTime.parse(partes[0].trim());
            LocalTime nuevaHoraFin = LocalTime.parse(partes[1].trim());

            if (nuevaHoraFin.isBefore(nuevaHoraInicio) || nuevaHoraInicio.equals(nuevaHoraFin)) {
                return false;
            }

            config.setHoraInicio(nuevaHoraInicio);
            config.setHoraFin(nuevaHoraFin);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // NUEVO: Método para actualizar horarios secuenciales
    // CORREGIDO: Método para actualizar horarios secuenciales
    private void actualizarHorariosSecuenciales(ConfiguracionHora configCambiado) {
        int indiceActual = datosHoras.indexOf(configCambiado);
        if (indiceActual == -1 || indiceActual == datosHoras.size() - 1) {
            return; // No hay elementos siguientes para actualizar
        }

        LocalTime horaFinActual = configCambiado.getHoraFin();

        // Actualizar todos los horarios siguientes
        for (int i = indiceActual + 1; i < datosHoras.size(); i++) {
            ConfiguracionHora configSiguiente = datosHoras.get(i);

            // CORREGIDO: Calcular duración del bloque actual
            Duration duracionBloque = Duration.between(
                    configSiguiente.getHoraInicio(),
                    configSiguiente.getHoraFin()
            );

            // Actualizar horarios
            configSiguiente.setHoraInicio(horaFinActual);
            configSiguiente.setHoraFin(horaFinActual.plus(duracionBloque));

            // Preparar para el siguiente bloque
            horaFinActual = configSiguiente.getHoraFin();

            System.out.println("Actualizado bloque " + (i + 1) + ": " +
                    configSiguiente.getHoraInicio() + " - " + configSiguiente.getHoraFin());
        }
    }

    // Métodos de selección de carpetas especiales
    @FXML
    private void seleccionarCarpetaElementosEspeciales() {
        seleccionarCarpeta("Elementos Especiales (Himnos y Poemas)", txtElementosEspeciales);
    }

    @FXML
    private void seleccionarCarpetaIdentificaciones() {
        seleccionarCarpeta("Identificaciones", txtIdentificaciones);
    }

    @FXML
    private void seleccionarCarpetaFelicitaciones() {
        seleccionarCarpeta("Felicitaciones", txtFelicitaciones);
    }

    @FXML
    private void seleccionarCarpetaPromosA() {
        seleccionarCarpeta("Promos A", txtPromosA);
    }

    @FXML
    private void seleccionarCarpetaPromosB() {
        seleccionarCarpeta("Promos B", txtPromosB);
    }

    private void seleccionarCarpeta(String titulo, TextField campo) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta de " + titulo);

        File carpetaSeleccionada = chooser.showDialog(getStage());
        if (carpetaSeleccionada != null) {
            campo.setText(carpetaSeleccionada.getAbsolutePath());
            verificarArchivosEnCarpeta(carpetaSeleccionada, titulo);
        }
    }

    private void seleccionarCarpetaGenero(ConfiguracionHora config) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta para " + config.getGenero().getNombre() +
                " (" + config.getHoraTexto() + ")");

        File carpetaSeleccionada = chooser.showDialog(getStage());
        if (carpetaSeleccionada != null) {
            config.setRutaCarpeta(carpetaSeleccionada.getAbsolutePath());
            tablaHoras.refresh();
            verificarArchivosEnCarpeta(carpetaSeleccionada, config.getGenero().getNombre());
        }
    }

    private void verificarArchivosEnCarpeta(File carpeta, String tipo) {
        List<String> archivos = ArchivoService.obtenerArchivosAudio(carpeta.getAbsolutePath());
        if (archivos.isEmpty()) {
            mostrarAlerta("Advertencia", "No se encontraron archivos de audio en la carpeta de " + tipo + ".");
        } else {
            actualizarEstado("Carpeta " + tipo + " configurada: " + archivos.size() + " archivos encontrados.");
        }
    }

    // Métodos de configuración de horarios
    @FXML
    private void inicializarHorarios() {
        datosHoras.clear();

        for (int hora = 7; hora < 20; hora++) {
            LocalTime inicio = LocalTime.of(hora, 0);
            LocalTime fin = LocalTime.of(hora + 1, 0);

            ConfiguracionHora config = new ConfiguracionHora(inicio, fin, Genero.VARIADO, "");
            datosHoras.add(config);
        }

        actualizarEstado("Horarios inicializados (7:00 AM - 8:00 PM). Configure los géneros y carpetas.");
    }

    // Métodos principales de generación
    @FXML
    private void generarLista() {
        if (!validarConfiguracion()) {
            return;
        }

        try {
            actualizarEstado("Generando lista...");
            btnGenerarLista.setDisable(true);

            // Configurar rutas en el servicio
            configurarRutasServicio();

            // Crear configuración de bloques (solo los que tienen carpeta)
            List<GeneradorListaService.ConfiguracionBloque> configuracion = crearConfiguracionBloques();

            // Generar la lista
            listaGenerada = generadorService.generarListaCompleta(LocalDate.now(), configuracion);

            btnExportarLST.setDisable(false);
            btnVistaPrevia.setDisable(false);

            // Contar horas programadas
            int horasProgramadas = configuracion.size();

            actualizarEstado(String.format("Lista generada exitosamente. %d horas programadas, %d canciones, %d elementos especiales.",
                    horasProgramadas, listaGenerada.getTotalCanciones(), listaGenerada.getTotalInserciones()));

        } catch (Exception e) {
            mostrarAlerta("Error", "Error al generar la lista: " + e.getMessage());
            actualizarEstado("Error en la generación.");
        } finally {
            btnGenerarLista.setDisable(false);
        }
    }

    @FXML
    private void mostrarVistaPrevia() {
        if (listaGenerada == null) {
            mostrarAlerta("Error", "Debe generar una lista primero.");
            return;
        }

        // Crear ventana de vista previa
        StringBuilder preview = new StringBuilder();
        preview.append("VISTA PREVIA DE LA LISTA\n");
        preview.append("========================\n");
        preview.append("Fecha: ").append(listaGenerada.getFecha()).append("\n");
        preview.append("Hora inicio: ").append(listaGenerada.getHoraInicio()).append("\n");
        preview.append("Hora fin: ").append(listaGenerada.getHoraFin()).append("\n\n");

        preview.append("APERTURA:\n");
        for (InsercionEspecial insercion : listaGenerada.getApertura()) {
            preview.append("- ").append(insercion.toString()).append("\n");
        }

        preview.append("\nBLOQUES POR HORA:\n");
        for (BloqueHora bloque : listaGenerada.getBloques()) {
            preview.append(bloque.toString()).append("\n");
        }

        preview.append("\nCIERRE:\n");
        for (InsercionEspecial insercion : listaGenerada.getCierre()) {
            preview.append("- ").append(insercion.toString()).append("\n");
        }

        mostrarVentanaTexto("Vista Previa de la Lista", preview.toString());
    }

    @FXML
    private void exportarLST() {
        if (listaGenerada == null) {
            mostrarAlerta("Error", "Debe generar una lista primero.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Lista LST");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos LST", "*.lst")
        );
        fileChooser.setInitialFileName("Lista_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".lst");

        File archivo = fileChooser.showSaveDialog(getStage());
        if (archivo != null) {
            boolean exito = ArchivoService.guardarListaLst(listaGenerada, archivo.getAbsolutePath());

            if (exito) {
                actualizarEstado("Lista exportada exitosamente: " + archivo.getName());
                mostrarInformacion("Éxito", "Lista LST exportada correctamente.\n\nArchivo: " +
                        archivo.getAbsolutePath());
            } else {
                mostrarAlerta("Error", "No se pudo exportar la lista LST.");
            }
        }
    }

    // Actualizar el método limpiarTodo:
    @FXML
    private void limpiarTodo() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar");
        confirmacion.setHeaderText("Limpiar toda la configuración");
        confirmacion.setContentText("¿Está seguro de que desea limpiar toda la configuración?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            // Limpiar campos de carpetas especiales
            txtElementosEspeciales.clear();
            txtIdentificaciones.clear();
            txtFelicitaciones.clear();
            txtPromosA.clear();
            txtPromosB.clear();

            // Limpiar tabla
            datosHoras.clear();

            // Resetear estado
            listaGenerada = null;
            btnExportarLST.setDisable(true);
            btnVistaPrevia.setDisable(true);

            actualizarEstado("Configuración limpiada.");
        }
    }

    // Métodos auxiliares
    // Actualizar el método de validación para incluir las nuevas carpetas:
    private boolean validarConfiguracion() {
        List<String> errores = new ArrayList<>();

        // Validar carpetas especiales
        if (txtElementosEspeciales.getText().trim().isEmpty()) errores.add("- Elementos Especiales (Himnos y Poemas)");
        if (txtIdentificaciones.getText().trim().isEmpty()) errores.add("- Identificaciones");
        if (txtFelicitaciones.getText().trim().isEmpty()) errores.add("- Felicitaciones");
        if (txtPromosA.getText().trim().isEmpty()) errores.add("- Promos A");
        if (txtPromosB.getText().trim().isEmpty()) errores.add("- Promos B");

        // Validar que al menos una hora tenga carpeta configurada
        boolean hayAlMenosUnaHora = false;
        for (ConfiguracionHora config : datosHoras) {
            if (!config.getRutaCarpeta().trim().isEmpty()) {
                hayAlMenosUnaHora = true;
                break;
            }
        }

        if (!hayAlMenosUnaHora) {
            errores.add("- Al menos una hora debe tener carpeta configurada");
        }

        if (!errores.isEmpty()) {
            StringBuilder mensaje = new StringBuilder("Faltan configurar:\n\n");
            errores.forEach(error -> mensaje.append(error).append("\n"));
            mostrarAlerta("Configuración incompleta", mensaje.toString());
            return false;
        }

        return true;
    }


    // Actualizar el método configurarRutasServicio:
    private void configurarRutasServicio() {
        generadorService.setRutaElementosEspeciales(txtElementosEspeciales.getText().trim());
        generadorService.setRutaIdentificaciones(txtIdentificaciones.getText().trim());
        generadorService.setRutaFelicitaciones(txtFelicitaciones.getText().trim());
        generadorService.setRutaPromosA(txtPromosA.getText().trim());
        generadorService.setRutaPromosB(txtPromosB.getText().trim());
    }

    private List<GeneradorListaService.ConfiguracionBloque> crearConfiguracionBloques() {
        List<GeneradorListaService.ConfiguracionBloque> configuracion = new ArrayList<>();

        for (ConfiguracionHora config : datosHoras) {
            // Solo agregar bloques que tengan carpeta configurada
            if (!config.getRutaCarpeta().trim().isEmpty()) {
                GeneradorListaService.ConfiguracionBloque bloque =
                        new GeneradorListaService.ConfiguracionBloque(
                                config.getHoraInicio(),
                                config.getHoraFin(),
                                config.getGenero(),
                                config.getRutaCarpeta()
                        );
                configuracion.add(bloque);
            }
        }

        return configuracion;
    }

    private ListaReproduccion crearListaDesdeConfiguracion() {
        ListaReproduccion lista = new ListaReproduccion();

        for (ConfiguracionHora config : datosHoras) {
            // Solo agregar bloques que tengan carpeta configurada
            if (!config.getRutaCarpeta().trim().isEmpty()) {
                BloqueHora bloque = new BloqueHora(
                        config.getHoraInicio(),
                        config.getHoraFin(),
                        config.getGenero(),
                        config.getRutaCarpeta()
                );
                lista.agregarBloque(bloque);
            }
        }

        return lista;
    }

    private void actualizarEstado(String mensaje) {
        lblEstado.setText(mensaje);
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarVentanaTexto(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(contenido);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);
        alert.showAndWait();
    }


    @FXML
    private void cargarConfiguracion() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Configuración");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de configuración", "*.txt")
        );

        File archivo = fileChooser.showOpenDialog(getStage());
        if (archivo != null) {
            if (cargarConfiguracionDesdeArchivo(archivo.getAbsolutePath())) {
                actualizarEstado("Configuración cargada exitosamente desde: " + archivo.getName());
            } else {
                mostrarAlerta("Error", "No se pudo cargar la configuración del archivo.");
            }
        }
    }
    // Reemplazar el método guardarConfiguracion() existente en MainController

    @FXML
    private void guardarConfiguracion() {
        if (datosHoras.isEmpty()) {
            mostrarAlerta("Error", "No hay configuración para guardar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Configuración");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de configuración", "*.txt")
        );

        File archivo = fileChooser.showSaveDialog(getStage());
        if (archivo != null) {
            if (exportarConfiguracionCompleta(archivo.getAbsolutePath())) {
                actualizarEstado("Configuración guardada exitosamente en: " + archivo.getName());
            } else {
                mostrarAlerta("Error", "No se pudo guardar la configuración.");
            }
        }
    }

    // Actualizar estos métodos en MainController para manejar las nuevas carpetas

    /**
     * Carga la configuración desde un archivo de texto (MÉTODO ACTUALIZADO)
     */
    private boolean cargarConfiguracionDesdeArchivo(String rutaArchivo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            datosHoras.clear();

            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();

                // Saltar comentarios y líneas vacías
                if (linea.startsWith("#") || linea.isEmpty()) {
                    continue;
                }

                // Buscar líneas de configuración de carpetas especiales
                if (linea.startsWith("ElementosEspeciales=")) {
                    txtElementosEspeciales.setText(linea.substring("ElementosEspeciales=".length()));
                    continue;
                }

                if (linea.startsWith("Identificaciones=")) {
                    txtIdentificaciones.setText(linea.substring("Identificaciones=".length()));
                    continue;
                }

                if (linea.startsWith("Felicitaciones=")) {
                    txtFelicitaciones.setText(linea.substring("Felicitaciones=".length()));
                    continue;
                }

                if (linea.startsWith("PromosA=")) {
                    txtPromosA.setText(linea.substring("PromosA=".length()));
                    continue;
                }

                if (linea.startsWith("PromosB=")) {
                    txtPromosB.setText(linea.substring("PromosB=".length()));
                    continue;
                }

                // Mantener compatibilidad con archivos antiguos que usen "Promos="
                if (linea.startsWith("Promos=") && !linea.startsWith("PromosA=") && !linea.startsWith("PromosB=")) {
                    // Asignar a PromosA para mantener compatibilidad
                    txtPromosA.setText(linea.substring("Promos=".length()));
                    continue;
                }

                // Procesar líneas de bloques de hora
                if (linea.startsWith("Bloque")) {
                    procesarLineaBloque(linea);
                }
            }

            tablaHoras.refresh();
            return true;

        } catch (IOException e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
            return false;
        }
    }

    /**
     * Exporta la configuración completa incluyendo carpetas especiales y bloques (MÉTODO ACTUALIZADO)
     */
    private boolean exportarConfiguracionCompleta(String rutaDestino) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaDestino))) {

            writer.write("# Configuración de Lista ZaraRadio\n");
            writer.write("# Fecha de creación: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "\n");
            writer.write("# Generado por: Generador de Listas ZaraRadio v2.0\n\n");

            // Escribir configuración de carpetas especiales
            writer.write("# === CARPETAS ESPECIALES ===\n");
            writer.write("# Elementos especiales (himnos y poemas)\n");
            writer.write("ElementosEspeciales=" + txtElementosEspeciales.getText().trim() + "\n\n");

            writer.write("# Carpetas de elementos rotatorios\n");
            writer.write("Identificaciones=" + txtIdentificaciones.getText().trim() + "\n");
            writer.write("Felicitaciones=" + txtFelicitaciones.getText().trim() + "\n");
            writer.write("PromosA=" + txtPromosA.getText().trim() + "\n");
            writer.write("PromosB=" + txtPromosB.getText().trim() + "\n\n");



            // CORREGIDO: Escribir configuración de bloques de hora directamente de datosHoras
            writer.write("# === PROGRAMACIÓN POR HORAS ===\n");
            writer.write("# Formato: Bloque XX: HH:MM:SS - HH:MM:SS | Género | Ruta\n");

            int numeroBloque = 1;
            for (ConfiguracionHora config : datosHoras) { // Usar datosHoras directamente
                // Solo guardar bloques que tengan carpeta configurada
                if (!config.getRutaCarpeta().trim().isEmpty()) {
                    writer.write(String.format("Bloque %02d: %s - %s | %s | %s\n",
                            numeroBloque,
                            config.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            config.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            config.getGenero().getNombre(),
                            config.getRutaCarpeta()
                    ));
                    numeroBloque++;
                }
            }

            writer.write("\n# === ESTADÍSTICAS ===\n");
            writer.write("# Total de bloques configurados: " + (numeroBloque - 1) + "\n");

            if (!datosHoras.isEmpty()) {
                LocalTime primeraHora = null;
                LocalTime ultimaHora = null;

                for (ConfiguracionHora config : datosHoras) {
                    if (!config.getRutaCarpeta().trim().isEmpty()) {
                        if (primeraHora == null) {
                            primeraHora = config.getHoraInicio();
                        }
                        ultimaHora = config.getHoraFin();
                    }
                }

                if (primeraHora != null && ultimaHora != null) {
                    writer.write("# Hora de inicio: " + primeraHora.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n");
                    writer.write("# Hora de fin: " + ultimaHora.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n");
                }
            } else {
                writer.write("# Hora de inicio: N/A\n");
                writer.write("# Hora de fin: N/A\n");
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error al exportar configuración: " + e.getMessage());
            return false;
        }
    }


    /**
     * Procesa una línea de configuración de bloque
     * Formato esperado: Bloque 01: 07:00:00 - 08:00:00 | Género | Ruta
     */
    private void procesarLineaBloque(String linea) {
        try {
            // Ejemplo: "Bloque 01: 07:00:00 - 08:00:00 | Pop | C:\\Music\\Pop"
            String[] partes = linea.split("\\|");
            if (partes.length != 3) {
                System.err.println("Formato incorrecto en línea: " + linea);
                return;
            }

            // CORREGIDO: Extraer información de tiempo correctamente
            String parteHora = partes[0].trim(); // "Bloque 01: 07:00:00 - 08:00:00"

            // Buscar el patrón después de los dos puntos
            int indiceDosPuntos = parteHora.indexOf(": ");
            if (indiceDosPuntos == -1) {
                System.err.println("No se encontró ':' en: " + parteHora);
                return;
            }

            String horarios = parteHora.substring(indiceDosPuntos + 2); // "07:00:00 - 08:00:00"
            String[] segmentosHora = horarios.split(" - ");

            if (segmentosHora.length != 2) {
                System.err.println("Formato de horario incorrecto: " + horarios);
                return;
            }

            LocalTime horaInicio = LocalTime.parse(segmentosHora[0].trim());
            LocalTime horaFin = LocalTime.parse(segmentosHora[1].trim());

            // Extraer género
            String nombreGenero = partes[1].trim();
            Genero genero = buscarGeneroPorNombre(nombreGenero);

            // Extraer ruta
            String rutaCarpeta = partes[2].trim();

            // Crear configuración
            ConfiguracionHora config = new ConfiguracionHora(horaInicio, horaFin, genero, rutaCarpeta);
            datosHoras.add(config);

            System.out.println("Bloque procesado correctamente: " + horaInicio + " - " + horaFin + " | " + genero.getNombre() + " | " + rutaCarpeta);

        } catch (Exception e) {
            System.err.println("Error al procesar línea de bloque: " + linea);
            e.printStackTrace();
        }
    }

    /**
     * Busca un género por su nombre
     */
    private Genero buscarGeneroPorNombre(String nombre) {
        for (Genero genero : Genero.values()) {
            if (genero.getNombre().equals(nombre)) {
                return genero;
            }
        }
        return Genero.VARIADO; // Por defecto
    }

    private Stage getStage() {
        return (Stage) btnGenerarLista.getScene().getWindow();
    }

    // Clase auxiliar para la tabla
    public static class ConfiguracionHora {
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private Genero genero;
        private String rutaCarpeta;

        public ConfiguracionHora(LocalTime horaInicio, LocalTime horaFin, Genero genero, String rutaCarpeta) {
            this.horaInicio = horaInicio;
            this.horaFin = horaFin;
            this.genero = genero;
            this.rutaCarpeta = rutaCarpeta != null ? rutaCarpeta : "";
        }

        public String getHoraTexto() {
            return horaInicio.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                    horaFin.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        // Getters y Setters
        public LocalTime getHoraInicio() { return horaInicio; }
        public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

        public LocalTime getHoraFin() { return horaFin; }
        public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

        public Genero getGenero() { return genero; }
        public void setGenero(Genero genero) {
            this.genero = genero;
        }

        public String getRutaCarpeta() { return rutaCarpeta; }
        public void setRutaCarpeta(String rutaCarpeta) { this.rutaCarpeta = rutaCarpeta != null ? rutaCarpeta : ""; }
    }
}