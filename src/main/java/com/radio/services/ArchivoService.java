package com.radio.services;

import com.radio.models.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ArchivoService {

    private static final String EXTENSION_LST = ".lst";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Guarda una lista de reproducción en formato .lst compatible con ZaraRadio
     */
    public static boolean guardarListaLst(ListaReproduccion lista, String rutaDestino) {
        try {
            // Crear el archivo con la fecha en el nombre si no se especifica
            String nombreArchivo = rutaDestino;
            if (!nombreArchivo.endsWith(EXTENSION_LST)) {
                nombreArchivo += "_" + lista.getFecha().format(FORMATO_FECHA) + EXTENSION_LST;
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(nombreArchivo), "UTF-8"))) {
                writer.write('\ufeff');

                // Escribir cabecera del archivo LST
                escribirCabeceraLst(writer, lista);

                // Escribir apertura (Himno Nacional, Himno Guerrero, Poema)
                escribirInserciones(writer, lista.getApertura());

                // Escribir cada bloque de hora con sus canciones e inserciones
                for (BloqueHora bloque : lista.getBloques()) {
                    escribirBloqueHora(writer, bloque);
                }

                // Escribir cierre (Poema, Himno Guerrero, Himno Nacional - orden inverso)
                escribirInserciones(writer, lista.getCierre());

                lista.setRutaArchivoLst(nombreArchivo);
                return true;
            }

        } catch (IOException e) {
            System.err.println("Error al guardar el archivo LST: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carga una lista de reproducción desde un archivo .lst
     */
    public static ListaReproduccion cargarListaLst(String rutaArchivo) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), "UTF-8"))) {

            ListaReproduccion lista = new ListaReproduccion();
            String linea;

            while ((linea = reader.readLine()) != null) {
                procesarLineaLst(linea, lista);
            }

            lista.setRutaArchivoLst(rutaArchivo);
            return lista;

        } catch (IOException e) {
            System.err.println("Error al cargar el archivo LST: " + e.getMessage());
            return null;
        }
    }

    /**
     * Exporta la configuración de géneros por hora a un archivo de texto
     */
    public static boolean exportarConfiguracion(ListaReproduccion lista, String rutaDestino) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaDestino))) {

            writer.write("# Configuración de Lista ZaraRadio\n");
            writer.write("# Fecha: " + lista.getFecha().format(FORMATO_FECHA) + "\n");
            writer.write("# Hora inicio: " + lista.getHoraInicio().format(FORMATO_HORA) + "\n");
            writer.write("# Hora fin: " + lista.getHoraFin().format(FORMATO_HORA) + "\n\n");

            for (int i = 0; i < lista.getBloques().size(); i++) {
                BloqueHora bloque = lista.getBloques().get(i);
                writer.write(String.format("Bloque %02d: %s - %s | %s | %s\n",
                        i + 1,
                        bloque.getHoraInicio().format(FORMATO_HORA),
                        bloque.getHoraFin().format(FORMATO_HORA),
                        bloque.getGenero().getNombre(),
                        bloque.getRutaCarpeta()
                ));
            }

            return true;

        } catch (IOException e) {
            System.err.println("Error al exportar configuración: " + e.getMessage());
            return false;
        }
    }

    /**
     * Valida que un archivo tenga la extensión .lst
     */
    public static boolean esArchivoLst(String rutaArchivo) {
        return rutaArchivo != null && rutaArchivo.toLowerCase().endsWith(EXTENSION_LST);
    }

    /**
     * Obtiene la lista de archivos de audio en una carpeta
     */
    public static List<String> obtenerArchivosAudio(String rutaCarpeta) {
        List<String> archivos = new ArrayList<>();
        File carpeta = new File(rutaCarpeta);

        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return archivos;
        }

        File[] listaArchivos = carpeta.listFiles((dir, nombre) -> {
            String extension = nombre.toLowerCase();
            return extension.endsWith(".mp3") || extension.endsWith(".wav") ||
                    extension.endsWith(".wma") || extension.endsWith(".ogg") ||
                    extension.endsWith(".aac");
        });

        if (listaArchivos != null) {
            for (File archivo : listaArchivos) {
                archivos.add(archivo.getAbsolutePath());
            }
        }

        return archivos;
    }

    // Métodos privados auxiliares
    private static void escribirCabeceraLst(BufferedWriter writer, ListaReproduccion lista) throws IOException {
        writer.write("#ZaraRadio List File\n");
        writer.write("#Generated by Generador de Listas\n");
        writer.write("#Date: " + lista.getFecha().format(FORMATO_FECHA) + "\n");
        writer.write("#Start: " + lista.getHoraInicio().format(FORMATO_HORA) + "\n");
        writer.write("#End: " + lista.getHoraFin().format(FORMATO_HORA) + "\n\n");
    }

    private static void escribirBloqueHora(BufferedWriter writer, BloqueHora bloque) throws IOException {
        // Comentario del bloque
        writer.write("#Bloque: " + bloque.getHoraInicio().format(FORMATO_HORA) +
                " - " + bloque.getHoraFin().format(FORMATO_HORA) +
                " (" + bloque.getGenero().getNombre() + ")\n");

        // Escribir canciones del bloque
        for (Cancion cancion : bloque.getCanciones()) {
            escribirCancionLst(writer, cancion);
        }

        // Escribir inserciones del bloque
        escribirInserciones(writer, bloque.getInserciones());

        writer.write("\n");
    }

    private static void escribirCancionLst(BufferedWriter writer, Cancion cancion) throws IOException {
        // Si es un marcador de tiempo, solo escribir -1 .time
        if ("TIME_MARKER".equals(cancion.getArtista())) {
            writer.write("-1 .time\n");
            return;
        }

        // CAMBIO: Calcular peso del archivo y escribir en formato ZaraRadio
        File archivo = new File(cancion.getRutaArchivo());
        if (!archivo.exists()) {
            System.err.println("Advertencia: Archivo no encontrado: " + cancion.getRutaArchivo());
            return; // O manejar el error como prefieras
        }
        long pesoEnBytes = archivo.exists() ? archivo.length() : 0;

        // Formato ZaraRadio: PESO RUTA_ARCHIVO
        writer.write(pesoEnBytes + " " + cancion.getRutaArchivo() + "\n");
    }

    private static void escribirInserciones(BufferedWriter writer, List<InsercionEspecial> inserciones) throws IOException {
        for (InsercionEspecial insercion : inserciones) {
            writer.write("#" + insercion.getTipo().getNombre() + ": " + insercion.getNombre() + "\n");

            // CAMBIO: Calcular peso del archivo antes de escribir la ruta
            File archivo = new File(insercion.getRutaArchivo());
            long pesoEnBytes = archivo.exists() ? archivo.length() : 0;

            // Escribir en formato ZaraRadio: PESO RUTA_ARCHIVO
            writer.write(pesoEnBytes + " " + insercion.getRutaArchivo() + "\n");
        }
    }

    private static void procesarLineaLst(String linea, ListaReproduccion lista) {
        // Lógica básica para procesar líneas del archivo LST
        if (linea.startsWith(";") || linea.trim().isEmpty()) {
            return; // Ignorar comentarios y líneas vacías
        }

        // Aquí se podría implementar la lógica completa de parsing
        // Por ahora es una implementación básica
        File archivo = new File(linea.trim());
        if (archivo.exists()) {
            // Crear canción básica y agregarla al último bloque
            // Esta es una implementación simplificada
        }
    }
}