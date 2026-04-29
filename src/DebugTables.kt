// ── Main para debug de tablas ──
// Este archivo carga e imprime las tablas usando las mismas clases que Parser.kt
// Compilar: kotlinc src/*.kt -d out/ && java -cp out/ DebugTablesKt

fun main() {
    try {
        println("╔═══════════════════════════════════════════╗")
        println("║   DEBUG: VERIFICAR CARGA DE TABLAS LR(1)  ║")
        println("╚═══════════════════════════════════════════╝\n")
        
        val tableLoader = TableLoader("data/action_table.csv")
        val loadedTables = tableLoader.loadTables()
        
        println("${"=".repeat(50)}")
        println("TABLA ACTION")
        println("${"=".repeat(50)}")
        println("Total de estados: ${loadedTables.actionTable.size}\n")
        
        // Imprimir estados 0, 1, 2
        for (stateNum in listOf(0, 1, 2)) {
            println("┌─ ESTADO $stateNum ${"─".repeat(30)}┐")
            val state = loadedTables.actionTable[stateNum] ?: emptyMap()
            
            if (state.isEmpty()) {
                println("│ (sin acciones)                          │")
            } else {
                val sortedKeys = state.keys.sorted()
                println("│ Terminales con acción: ${sortedKeys.size}")
                println("│ Claves: ${sortedKeys.joinToString(", ")}")
                println("│")
                println("│ Contenido detallado:")
                for ((terminal, action) in state.toSortedMap()) {
                    println("│   $terminal → $action")
                }
            }
            println("└${"─".repeat(40)}┘\n")
        }
        
        println("${"=".repeat(50)}")
        println("TABLA GOTO")
        println("${"=".repeat(50)}")
        println("Total de estados: ${loadedTables.gotoTable.size}\n")
        
        // Imprimir estados 0, 1, 2
        for (stateNum in listOf(0, 1, 2)) {
            println("┌─ ESTADO $stateNum ${"─".repeat(30)}┐")
            val state = loadedTables.gotoTable[stateNum] ?: emptyMap()
            
            if (state.isEmpty()) {
                println("│ (sin transiciones GOTO)                 │")
            } else {
                val sortedKeys = state.keys.sorted()
                println("│ No-terminales con GOTO: ${sortedKeys.size}")
                println("│ Claves: ${sortedKeys.joinToString(", ")}")
                println("│")
                println("│ Contenido detallado:")
                for ((nonterminal, targetState) in state.toSortedMap()) {
                    println("│   $nonterminal → $targetState")
                }
            }
            println("└${"─".repeat(40)}┘\n")
        }
        
        println("${"=".repeat(50)}")
        println("GRAMÁTICA")
        println("${"=".repeat(50)}")
        println("Total de producciones: ${loadedTables.grammar.size}\n")
        
        println("Primeras 10 producciones:")
        for ((idx, prod) in loadedTables.grammar.take(10).withIndex()) {
            val ruleNum = idx + 1
            val rhs = if (prod.rhs.isEmpty()) "ε" else prod.rhs.joinToString(" ")
            println("  $ruleNum: ${prod.lhs} → $rhs")
        }
        
        println("\n..." + (if (loadedTables.grammar.size > 10) " (${loadedTables.grammar.size - 10} más)" else ""))
        
        println("\n" + "${"=".repeat(50)}")
        println("✓ CARGA COMPLETADA EXITOSAMENTE")
        println("${"=".repeat(50)}")
        
    } catch (e: CompileError) {
        println("\n╔═══════════════════════════════════════════╗")
        println("║    ✗ ERROR DE CARGA                       ║")
        println("╠═══════════════════════════════════════════╣")
        println("║ ${e.message}")
        println("╚═══════════════════════════════════════════╝")
        System.exit(1)
    } catch (e: Exception) {
        println("\n╔═══════════════════════════════════════════╗")
        println("║    ✗ ERROR INESPERADO                     ║")
        println("╠═══════════════════════════════════════════╣")
        println("║ ${e.message}")
        println("║ ${e.javaClass.simpleName}")
        println("╚═══════════════════════════════════════════╝")
        e.printStackTrace()
        System.exit(1)
    }
}
