import java.io.File

// ── Excepción personalizada para errores de compilación ──
class CompileError(message: String) : Exception(message)

// ── Funciones auxiliares para construcción de tablas de error ──
fun buildErrorBox(title: String, messages: List<String>): String {
    val allText = messages.joinToString("\n")
    val maxLength = maxOf(title.length + 6, allText.split("\n").maxOf { it.length + 3 })
    val width = maxOf(maxLength, 40) // Mínimo 40 caracteres
    
    val horizontalLine = "═".repeat(width - 2)
    val topLine = "╔$horizontalLine╗"
    val middleLine = "╠$horizontalLine╣"
    val bottomLine = "╚$horizontalLine╝"
    
    val titlePadded = "║ $title${" ".repeat(width - title.length - 3)}║"
    val contentLines = messages.map { msg ->
        val paddedMsg = msg + " ".repeat(maxOf(0, width - msg.length - 3))
        "║ $paddedMsg║"
    }
    
    return buildString {
        appendLine()
        appendLine(topLine)
        appendLine(titlePadded)
        appendLine(middleLine)
        contentLines.forEach { appendLine(it) }
        appendLine(bottomLine)
    }
}

// ── Funciones auxiliares para selección de archivo ──
fun getQuetzalFiles(): List<File> {
    val filesDir = File("files")
    return if (filesDir.exists() && filesDir.isDirectory) {
        filesDir.listFiles { file -> file.extension == "quetzal" }?.toList() ?: emptyList()
    } else {
        emptyList()
    }
}

fun selectFile(): File {
    val quetzalFiles = getQuetzalFiles()
    
    return when {
        quetzalFiles.isEmpty() -> {
            println("No se encontraron archivos .quetzal en la carpeta 'files/'")
            println("\nIngresa el path completo del archivo .quetzal:")
            val path = readLine()?.trim() ?: throw CompileError("Path inválido")
            val file = File(path)
            if (!file.exists()) throw CompileError("Archivo no encontrado: $path")
            file
        }
        quetzalFiles.size == 1 -> {
            println("Se encontró 1 archivo .quetzal: ${quetzalFiles[0].name}")
            quetzalFiles[0]
        }
        else -> {
            println("Se encontraron ${quetzalFiles.size} archivos .quetzal:")
            quetzalFiles.forEachIndexed { index, file ->
                println("${index + 1}. ${file.name}")
            }
            println("0. Especificar otro path")
            println("\nSelecciona un archivo (1-${quetzalFiles.size}) o 0 para otro path:")
            
            val choice = readLine()?.trim()?.toIntOrNull()
            when {
                choice != null && choice in 1..quetzalFiles.size -> quetzalFiles[choice - 1]
                choice == 0 -> {
                    println("Ingresa el path completo del archivo .quetzal:")
                    val path = readLine()?.trim() ?: throw CompileError("Path inválido")
                    val file = File(path)
                    if (!file.exists()) throw CompileError("Archivo no encontrado: $path")
                    file
                }
                else -> throw CompileError("Selección inválida")
            }
        }
    }
}

// ── Main de prueba ────────────────────────────────────────────────
fun main() {
    try {
        println("╔════════════════════════════════════════╗")
        println("║  COMPILADOR QUETZAL - Análisis Léxico  ║")
        println("║           y Sintáctico LR(1)           ║")
        println("╚════════════════════════════════════════╝\n")
        
        val archivoFuente = selectFile()
        println("\nArchivo seleccionado: ${archivoFuente.absolutePath}\n")
        
        val codigo = archivoFuente.readText()
        
        debugPrintln("=== ANÁLISIS LÉXICO ===")
        val tokens = Lexer(codigo).tokenize()
        debugPrintln("✓ Tokens generados: ${tokens.size}")
        tokens.forEach { debugPrintln("  $it") }
        
        debugPrintln("\n=== CARGANDO TABLA Y GRAMÁTICA ===")
        val tableLoader = TableLoader("data/action_table.csv")
        val loadedTables = tableLoader.loadTables()
        debugPrintln("✓ Tabla ACTION cargada: ${loadedTables.actionTable.size} estados")
        debugPrintln("✓ Tabla GOTO cargada: ${loadedTables.gotoTable.size} estados")
        debugPrintln("✓ Gramática cargada: ${loadedTables.grammar.size} producciones")
        
        // Debug: check state 0 in both tables
        val action0Keys = loadedTables.actionTable[0]?.keys?.toList() ?: emptyList()
        val goto0Keys = loadedTables.gotoTable[0]?.keys?.toList() ?: emptyList()
        debugPrintln("  Estado 0 ACTION keys: $action0Keys")
        debugPrintln("  Estado 0 GOTO keys: $goto0Keys")
        
        val action2Keys = loadedTables.actionTable[2]?.keys?.toList() ?: emptyList()
        debugPrintln("  Estado 2 ACTION keys: $action2Keys")
        
        debugPrintln("\n=== ANÁLISIS SINTÁCTICO ===")
        val parser = LRParser(tokens, loadedTables.actionTable, loadedTables.gotoTable, loadedTables.grammar)
        parser.parse()
        
        println("\n╔════════════════════════════════════════╗")
        println("║    ✓ COMPILACIÓN EXITOSA               ║")
        println("╚════════════════════════════════════════╝")
    } catch (e: CompileError) {
        val errorMessage = e.message ?: "Error desconocido"
        print(buildErrorBox("✗ ERROR DE COMPILACIÓN", listOf(errorMessage)))
        System.exit(1)
    } catch (e: Exception) {
        val messages = listOf(e.message ?: "Error desconocido", e.javaClass.simpleName)
        print(buildErrorBox("✗ ERROR INESPERADO", messages))
        e.printStackTrace()
        System.exit(1)
    }
}