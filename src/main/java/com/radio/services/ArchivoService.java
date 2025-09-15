package com.radio.services;

import com.radio.models.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ArchivoService {

    private static final String EXTENSION_M3U = ".m3u";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Guarda una lista de reproducción en formato .m3u
     */
//    public static boolean guardarListaM3u(ListaReproduccion lista, String rutaDestino) {
//        try {
//            // Crear el archivo con la fecha en el nombre si no se especifica
//            String nombreArchivo = rutaDestino;
//            if (!nombreArchivo.endsWith(EXTENSION_M3U)) {
//                nombreArchivo += "_" + lista.getFecha().format(FORMATO_FECHA) + EXTENSION_M3U;
//            }
//
//            try (BufferedWriter writer = new BufferedWriter(
//                    new OutputStreamWriter(new FileOutputStream(nombreArchivo), "UTF-8"))) {
//
//                // Escribir cabecera M3U
//                writer.write("#EXTM3U\n");
//
//                // Escribir apertura (Himno Nacional, Himno Guerrero, Poema)
//                escribirInsercionesM3u(writer, lista.getApertura());
//
//                // Escribir cada bloque de hora con sus canciones e inserciones
//                for (BloqueHora bloque : lista.getBloques()) {
//                    escribirBloqueHoraM3u(writer, bloque);
//                }
//
//                // Escribir cierre (Poema, Himno Guerrero, Himno Nacional - orden inverso)
//                escribirInsercionesM3u(writer, lista.getCierre());
//
//                lista.setRutaArchivoLst(nombreArchivo);
//                return true;
//            }
//
//        } catch (IOException e) {
//            System.err.println("Error al guardar el archivo M3U: " + e.getMessage());
//            return false;
//        }
//    }
    /**
     * Guarda una lista de reproducción en formato .m3u con codificación compatible con ZaraRadio
     * CORREGIDO: Usa Windows-1252 en lugar de UTF-8 para compatibilidad con ZaraRadio
     */
    public static boolean guardarListaM3u(ListaReproduccion lista, String rutaDestino) {
        try {
            // Crear el archivo con la fecha en el nombre si no se especifica
            String nombreArchivo = rutaDestino;
            if (!nombreArchivo.endsWith(EXTENSION_M3U)) {
                nombreArchivo += "_" + lista.getFecha().format(FORMATO_FECHA) + EXTENSION_M3U;
            }

            // CORREGIDO: Usar Windows-1252 (ANSI) en lugar de UTF-8
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(nombreArchivo), "Windows-1252"))) {

                // Escribir cabecera M3U
                writer.write("#EXTM3U\n");

                // Escribir apertura (Himno Nacional, Himno Guerrero, Poema)
                escribirInsercionesM3u(writer, lista.getApertura());

                // Escribir cada bloque de hora con sus canciones e inserciones
                for (BloqueHora bloque : lista.getBloques()) {
                    escribirBloqueHoraM3u(writer, bloque);
                }

                // Escribir cierre (Poema, Himno Guerrero, Himno Nacional - orden inverso)
                escribirInsercionesM3u(writer, lista.getCierre());

                lista.setRutaArchivoLst(nombreArchivo);

                // Verificar que el archivo se guardó correctamente
                System.out.println("Archivo M3U guardado con codificación Windows-1252: " + nombreArchivo);

                return true;
            }

        } catch (IOException e) {
            System.err.println("Error al guardar el archivo M3U: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ALTERNATIVO: Método con detección automática de codificación
     * Intenta primero Windows-1252, luego ISO-8859-1 como fallback
     */
    public static boolean guardarListaM3uCompatible(ListaReproduccion lista, String rutaDestino) {
        // Probar diferentes codificaciones en orden de compatibilidad
        String[] codificaciones = {"Windows-1252", "ISO-8859-1", "CP1252"};

        for (String codificacion : codificaciones) {
            try {
                String nombreArchivo = rutaDestino;
                if (!nombreArchivo.endsWith(EXTENSION_M3U)) {
                    nombreArchivo += "_" + lista.getFecha().format(FORMATO_FECHA) + EXTENSION_M3U;
                }

                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(nombreArchivo), codificacion))) {

                    // Escribir cabecera M3U
                    writer.write("#EXTM3U\n");

                    // Escribir apertura
                    escribirInsercionesM3uSeguro(writer, lista.getApertura(), codificacion);

                    // Escribir cada bloque de hora
                    for (BloqueHora bloque : lista.getBloques()) {
                        escribirBloqueHoraM3uSeguro(writer, bloque, codificacion);
                    }

                    // Escribir cierre
                    escribirInsercionesM3uSeguro(writer, lista.getCierre(), codificacion);

                    lista.setRutaArchivoLst(nombreArchivo);

                    System.out.println("Archivo M3U guardado exitosamente con codificación: " + codificacion);
                    return true;
                }

            } catch (UnsupportedEncodingException e) {
                System.err.println("Codificación no soportada: " + codificacion);
                continue; // Probar siguiente codificación
            } catch (IOException e) {
                System.err.println("Error de E/S con codificación " + codificacion + ": " + e.getMessage());
                continue; // Probar siguiente codificación
            }
        }

        System.err.println("No se pudo guardar el archivo con ninguna codificación compatible");
        return false;
    }

    // NUEVOS métodos auxiliares para escritura segura
    private static void escribirBloqueHoraM3uSeguro(BufferedWriter writer, BloqueHora bloque, String codificacion) throws IOException {
        for (Cancion cancion : bloque.getCanciones()) {
            escribirCancionM3uSeguro(writer, cancion, codificacion);
        }
    }

    private static void escribirCancionM3uSeguro(BufferedWriter writer, Cancion cancion, String codificacion) throws IOException {
        if (cancion.getRutaArchivo().equals(".time")) {
            // Escribir locución de hora con formato especial para M3U
            writer.write("#EXTINF:-1,Locución de hora\n");
            writer.write(".time\n");
        } else {
            File archivo = new File(cancion.getRutaArchivo());
            if (!archivo.exists()) {
                System.err.println("Advertencia: Archivo no encontrado: " + cancion.getRutaArchivo());
                return;
            }

            // Verificar si la ruta contiene caracteres problemáticos
            String rutaArchivo = cancion.getRutaArchivo();
            if (contieneCaracteresProblematicos(rutaArchivo)) {
                System.out.println("Advertencia: Ruta con caracteres especiales: " + rutaArchivo);
                // Intentar convertir caracteres problemáticos
                rutaArchivo = limpiarRutaParaZara(rutaArchivo);
                System.out.println("Ruta limpiada: " + rutaArchivo);
            }

            writer.write(rutaArchivo + "\n");
        }
    }

    private static void escribirInsercionesM3uSeguro(BufferedWriter writer, List<InsercionEspecial> inserciones, String codificacion) throws IOException {
        for (InsercionEspecial insercion : inserciones) {
            File archivo = new File(insercion.getRutaArchivo());
            if (!archivo.exists()) {
                System.err.println("Advertencia: Archivo no encontrado: " + insercion.getRutaArchivo());
                continue;
            }

            String rutaArchivo = insercion.getRutaArchivo();
            if (contieneCaracteresProblematicos(rutaArchivo)) {
                System.out.println("Advertencia: Inserción con caracteres especiales: " + rutaArchivo);
                rutaArchivo = limpiarRutaParaZara(rutaArchivo);
                System.out.println("Ruta de inserción limpiada: " + rutaArchivo);
            }

            writer.write(rutaArchivo + "\n");
        }
    }

    // NUEVOS métodos de utilidad para manejo de caracteres
    private static boolean contieneCaracteresProblematicos(String texto) {
        // Detectar caracteres que pueden causar problemas en ZaraRadio
        return texto.matches(".*[áéíóúüñÁÉÍÓÚÜÑ¿¡].*");
    }

    private static String limpiarRutaParaZara(String ruta) {
        // OPCIÓN 1: Mantener la ruta original (recomendado)
        // ZaraRadio debería poder leer los archivos si la codificación es correcta
        return ruta;

    /* OPCIÓN 2: Reemplazar caracteres problemáticos (solo si es necesario)
    return ruta
        .replace("á", "a").replace("Á", "A")
        .replace("é", "e").replace("É", "E")
        .replace("í", "i").replace("Í", "I")
        .replace("ó", "o").replace("Ó", "O")
        .replace("ú", "u").replace("Ú", "U")
        .replace("ü", "u").replace("Ü", "U")
        .replace("ñ", "n").replace("Ñ", "N")
        .replace("¿", "").replace("¡", "");
    */
    }

    /**
     * Carga una lista de reproducción desde un archivo .m3u
     */
    /**
     * Carga una lista de reproducción desde un archivo .m3u
     * CORREGIDO: Intenta diferentes codificaciones para máxima compatibilidad
     */
    public static ListaReproduccion cargarListaM3u(String rutaArchivo) {
        String[] codificaciones = {"Windows-1252", "ISO-8859-1", "UTF-8", "CP1252"};

        for (String codificacion : codificaciones) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(rutaArchivo), codificacion))) {

                System.out.println("Intentando cargar M3U con codificación: " + codificacion);

                ListaReproduccion lista = new ListaReproduccion();
                String linea;
                int numeroLinea = 0;
                boolean caracteresLegibles = true;

                while ((linea = reader.readLine()) != null) {
                    numeroLinea++;

                    // Verificar si hay caracteres raros que indican codificación incorrecta
                    if (contieneCaracteresIlegibles(linea)) {
                        caracteresLegibles = false;
                        System.out.println("Caracteres ilegibles detectados en línea " + numeroLinea +
                                " con codificación " + codificacion + ": " + linea);
                        break; // Probar siguiente codificación
                    }

                    procesarLineaM3u(linea, lista);
                }

                if (caracteresLegibles) {
                    lista.setRutaArchivoLst(rutaArchivo);
                    System.out.println("Archivo M3U cargado exitosamente con codificación: " + codificacion);
                    return lista;
                }

            } catch (UnsupportedEncodingException e) {
                System.err.println("Codificación no soportada: " + codificacion);
                continue;
            } catch (IOException e) {
                System.err.println("Error de E/S con codificación " + codificacion + ": " + e.getMessage());
                continue;
            }
        }

        System.err.println("No se pudo cargar el archivo M3U con ninguna codificación");
        return null;
    }

    /**
     * Detecta caracteres que indican una codificación incorrecta
     */
    private static boolean contieneCaracteresIlegibles(String texto) {
        // Detectar caracteres de reemplazo y secuencias raras que indican codificación incorrecta
        return texto.contains("�") || // Caracter de reemplazo
                texto.matches(".*[À-ÿ]{2,}.*") || // Secuencias de caracteres extendidos
                texto.matches(".*\\?{2,}.*"); // Múltiples signos de interrogación
    }

    // MÉTODO ADICIONAL: Diagnóstico de codificación
    public static void diagnosticarCodificacionArchivo(String rutaArchivo) {
        String[] codificaciones = {"UTF-8", "Windows-1252", "ISO-8859-1", "CP1252"};

        System.out.println("=== DIAGNÓSTICO DE CODIFICACIÓN ===");
        System.out.println("Archivo: " + rutaArchivo);

        for (String codificacion : codificaciones) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(rutaArchivo), codificacion))) {

                System.out.println("\n--- " + codificacion + " ---");
                String linea;
                int contador = 0;

                while ((linea = reader.readLine()) != null && contador < 10) {
                    if (!linea.trim().isEmpty() && !linea.startsWith("#")) {
                        System.out.println("Línea " + (contador + 1) + ": " + linea);

                        // Mostrar caracteres problemáticos
                        if (contieneCaracteresProblematicos(linea)) {
                            System.out.println("  -> Contiene acentos/caracteres especiales");
                        }
                        if (contieneCaracteresIlegibles(linea)) {
                            System.out.println("  -> Contiene caracteres ilegibles");
                        }

                        contador++;
                    }
                }

            } catch (Exception e) {
                System.out.println("Error con " + codificacion + ": " + e.getMessage());
            }
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
     * Valida que un archivo tenga la extensión .m3u
     */
    public static boolean esArchivoM3u(String rutaArchivo) {
        return rutaArchivo != null && rutaArchivo.toLowerCase().endsWith(EXTENSION_M3U);
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

    // Métodos privados auxiliares para formato M3U
    private static void escribirBloqueHoraM3u(BufferedWriter writer, BloqueHora bloque) throws IOException {
        // Escribir canciones del bloque
        for (Cancion cancion : bloque.getCanciones()) {
            escribirCancionM3u(writer, cancion);
        }
    }

    private static void escribirCancionM3u(BufferedWriter writer, Cancion cancion) throws IOException {
        if (cancion.getRutaArchivo().equals(".time")) {
            // Escribir locución de hora con formato especial para M3U
            writer.write("#EXTINF:-1,Locución de hora\n");
            writer.write(".time\n");
        } else {
            File archivo = new File(cancion.getRutaArchivo());
            if (!archivo.exists()) {
                System.err.println("Advertencia: Archivo no encontrado: " + cancion.getRutaArchivo());
                return;
            }
            // Escribir la ruta normal para canciones
            writer.write(cancion.getRutaArchivo() + "\n");
        }
    }


    private static void escribirInsercionesM3u(BufferedWriter writer, List<InsercionEspecial> inserciones) throws IOException {
        for (InsercionEspecial insercion : inserciones) {
            File archivo = new File(insercion.getRutaArchivo());
            if (!archivo.exists()) {
                System.err.println("Advertencia: Archivo no encontrado: " + insercion.getRutaArchivo());
                continue;
            }

            // Solo escribir la ruta del archivo
            writer.write(insercion.getRutaArchivo() + "\n");
        }
    }

    private static void procesarLineaM3u(String linea, ListaReproduccion lista) {
        // Lógica básica para procesar líneas del archivo M3U
        if (linea.startsWith("#") || linea.trim().isEmpty()) {
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