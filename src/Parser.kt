import java.io.File

// ── Clase para representar una producción de la gramática ──
data class Production(val lhs: String, val rhs: List<String>, val length: Int)

// ── Cargador de tabla LR(1) desde CSV ──
class TableLoader(private val baseDataPath: String) {
    private val grammar = mutableListOf<Production>()
    private val actionTable = mutableMapOf<Int, MutableMap<String, String>>()
    private val gotoTable = mutableMapOf<Int, MutableMap<String, String>>()

    data class LoadedTables(
        val actionTable: Map<Int, Map<String, String>>,
        val gotoTable: Map<Int, Map<String, String>>,
        val grammar: List<Production>
    )

    fun loadTables(): LoadedTables {
        try {
            parseActionTable()
            parseGotoTable()
            parseGrammar()
            return LoadedTables(actionTable, gotoTable, grammar)
        } catch (e: Exception) {
            throw CompileError("Error al cargar tablas: ${e.message}")
        }
    }

    private fun parseActionTable() {
        val actionPath = baseDataPath  // baseDataPath already points to action_table.csv
        parseTable(actionPath, actionTable)
    }

    private fun parseGotoTable() {
        val gotoPath = baseDataPath.replace("action_table.csv", "goto_table.csv")
        parseTable(gotoPath, gotoTable)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseTable(csvPath: String, targetTable: MutableMap<Int, MutableMap<String, String>>) {
        val lines = File(csvPath).readLines()
        if (lines.isEmpty()) throw CompileError("Tabla CSV vacía: $csvPath")

        val headerLine = parseCsvLine(lines[0].trimStart('\uFEFF'))
        val headers = headerLine.drop(1).map { it.trim().trimEnd('\r') }
        val numColumns = headerLine.size

        for (i in 1 until lines.size) {
            val trimmedLine = lines[i].trim()
            if (trimmedLine.isEmpty()) continue

            val parts = parseCsvLine(trimmedLine).map { it.trim().trimEnd('\r') }.toMutableList()
            while (parts.size < numColumns) parts.add("")

            val state = parts[0].toIntOrNull() ?: continue
            val rowMap = mutableMapOf<String, String>()
            for (j in 1 until numColumns) {
                val value = parts.getOrNull(j) ?: ""
                if (value.isNotEmpty()) rowMap[headers[j - 1]] = value
            }
            targetTable[state] = rowMap
        }
    }

    private fun parseGrammar() {
        val grammarFile = File(baseDataPath.replace("action_table.csv", "grammar.txt"))
        if (!grammarFile.exists()) throw CompileError("Archivo grammar.txt no encontrado")

        for (line in grammarFile.readLines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("REGLAS") || trimmed.startsWith("Formato") ||
                trimmed.startsWith("NOTA") || trimmed.startsWith("=") || trimmed.startsWith("---")) {
                continue
            }

            val match = Regex("""^(\d+):\s*(.+?)\s*->\s*(.*)$""").find(trimmed)
            if (match != null) {
                val lhs = match.groupValues[2].trim()
                val rhsStr = match.groupValues[3].trim()
                val rhs = if (rhsStr == "ε" || rhsStr == "epsilon") {
                    emptyList()
                } else {
                    rhsStr.split(Regex("\\s+")).filter { it.isNotEmpty() }
                }
                grammar.add(Production(lhs, rhs, rhs.size))
            }
        }
        
        if (grammar.isEmpty()) {
            throw CompileError("La gramática no contiene producciones válidas")
        }
    }
}

// ── Analizador Sintáctico LR(1) ──
class LRParser(
    private val tokens: List<Token>,
    private val actionTable: Map<Int, Map<String, String>>,
    private val gotoTable: Map<Int, Map<String, String>>,
    private val grammar: List<Production>
) {
    private val stateStack = mutableListOf(0)
    private val symbolStack = mutableListOf<String>()
    private var currentTokenIndex = 0

    fun parse() {
        try {
            while (true) {
                if (stateStack.isEmpty()) {
                    throw CompileError("Error interno: state stack vacío")
                }
                
                val state = stateStack.last()
                if (currentTokenIndex >= tokens.size) {
                    throw CompileError("Error interno: se acabaron los tokens")
                }
                
                val token = tokens[currentTokenIndex]
                val terminal = token.getTerminal()

                val action = actionTable[state]?.get(terminal)
                    ?: throw CompileError(
                        "Error de sintaxis en línea ${token.line}, columna ${token.column}: " +
                        "token '${token.value}' inesperado (en estado $state, esperando acción para terminal '$terminal')"
                    )

                when {
                    action.startsWith("s") -> {
                        val newState = try {
                            action.substring(1).toInt()
                        } catch (e: Exception) {
                            throw CompileError("Error interno: acción shift inválida '$action'")
                        }
                        symbolStack.add(terminal)
                        stateStack.add(newState)
                        currentTokenIndex++
                    }

                    action.startsWith("r") -> {
                        val ruleNumberFromTable = try {
                            action.substring(1).toInt()
                        } catch (e: Exception) {
                            throw CompileError("Error interno: acción reduce inválida '$action'")
                        }
                        
                        // Las reglas en grammar.txt están numeradas de 1 a 83
                        // Pero el array grammar es 0-indexed, así que restamos 1
                        val grammarIndex = ruleNumberFromTable - 1
                        
                        if (grammarIndex < 0 || grammarIndex >= grammar.size) {
                            throw CompileError(
                                "Error interno: número de regla $ruleNumberFromTable fuera de rango (total: ${grammar.size})"
                            )
                        }
                        val production = grammar[grammarIndex]

                        // Pop production.length symbols from both stacks
                        // But ensure stateStack never becomes completely empty
                        val numToPop = production.length
                        
                        for (i in 0 until numToPop) {
                            if (stateStack.size > 1) {
                                stateStack.removeAt(stateStack.lastIndex)
                            }
                            if (symbolStack.isNotEmpty()) {
                                symbolStack.removeAt(symbolStack.lastIndex)
                            }
                        }

                        symbolStack.add(production.lhs)

                        if (stateStack.isEmpty()) {
                            throw CompileError(
                                "Error interno: state stack vacío después de desapilar en regla $ruleNumberFromTable"
                            )
                        }
                        
                        val prevState = stateStack.last()
                        
                        val gotoAction = gotoTable[prevState]?.get(production.lhs)
                            ?: run {
                                val availableNonTerminals = gotoTable[prevState]?.keys?.toList() ?: emptyList()
                                throw CompileError(
                                    "Error interno: no se encontró GOTO para estado $prevState y símbolo '${production.lhs}'. Disponibles: $availableNonTerminals"
                                )
                            }

                        val newState = try {
                            if (gotoAction.startsWith("s")) {
                                gotoAction.substring(1).toInt()
                            } else {
                                gotoAction.toInt()
                            }
                        } catch (e: Exception) {
                            throw CompileError(
                                "Error interno: GOTO inválido '$gotoAction' para símbolo '${production.lhs}'"
                            )
                        }

                        stateStack.add(newState)
                    }

                    action == "acc" -> {
                        println("✓ Análisis sintáctico completado exitosamente")
                        return
                    }

                    else -> throw CompileError(
                        "Error de sintaxis en línea ${token.line}, columna ${token.column}: " +
                        "acción desconocida '$action'"
                    )
                }
            }
        } catch (e: CompileError) {
            throw e
        } catch (e: IndexOutOfBoundsException) {
            val token = if (currentTokenIndex < tokens.size) tokens[currentTokenIndex] else tokens.lastOrNull() ?: Token(TokenType.EOF, "", 1, 1)
            throw CompileError(
                "Error de sintaxis en línea ${token.line}, columna ${token.column}: ${e.message ?: "acceso fuera de rango"}"
            )
        } catch (e: Exception) {
            val token = if (currentTokenIndex < tokens.size) tokens[currentTokenIndex] else tokens.lastOrNull() ?: Token(TokenType.EOF, "", 1, 1)
            throw CompileError(
                "Error de sintaxis en línea ${token.line}, columna ${token.column}: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }
}
