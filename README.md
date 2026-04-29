# Compilador Quetzal - Análisis Léxico y Sintáctico LR(1)

## Descripción

Este proyecto implementa un compilador para el lenguaje Quetzal que incluye:
- **Analizador Léxico (Lexer):** Tokeniza el código fuente identificando palabras clave, identificadores, literales, operadores y separadores
- **Analizador Sintáctico (Parser):** Valida que el código cumpla con la gramática LR(1) del lenguaje Quetzal

## Estructura del Proyecto

```
KotlinCompiler/
├── src/
│   ├── Main.kt           # Función main y utilidades de carga de archivos
│   ├── Lexer.kt          # Analizador léxico completo
│   ├── Parser.kt         # Analizador sintáctico LR(1)
│   ├── DebugTables.kt    # Funciones de depuración para tablas
│   └── FeatureFlags.kt   # Configuración de feature flags
├── data/
│   ├── grammar.txt       # Reglas de producción (83 reglas numeradas)
│   └── action_table.csv  # Tabla de análisis LR(1) (268 estados)
├── files/                # Archivos .quetzal para compilar
│   └── binary.quetzal
└── README.md            # Este archivo
```

## Cómo Ejecutar

### Opción 1: Desde IntelliJ IDEA
1. Abre el proyecto en IntelliJ IDEA
2. Haz clic derecho en `Main.kt` y selecciona "Run"
3. El programa te mostrará los archivos `.quetzal` disponibles
4. Selecciona uno o proporciona un path personalizado

### Opción 2: Desde línea de comandos
```bash
cd KotlinCompiler
kotlinc src/*.kt -d out/
java -cp out/ MainKt
```

## Flujo de Compilación

```
Código fuente (.quetzal)
        ↓
    LEXER (Main.kt)
        ↓
  Lista de Tokens
        ↓
    PARSER (Parser.kt)
        ↓
Validación sintáctica
        ↓
✓ Éxito o ✗ Error
```

## Manejo de Errores

El compilador reporta errores de forma robusta con **línea y columna** precisas. Las tablas de error se ajustan dinámicamente al tamaño del mensaje:

### Error del Lexer
```
Error en línea 5, columna 12: carácter inesperado '@'
```

### Error del Parser
```
Error de sintaxis en línea 7, columna 3: token 'xyz' inesperado
```

### Tabla de Error Dinámico
La tabla se expande automáticamente para mensajes largos, manteniendo el formato visual correcto sin que el contenido se salga de los bordes.

## Componentes Principales

### 1. Lexer (Lexer.kt)
- Reconoce todos los tokens del lenguaje Quetzal
- Rastrea línea y columna de cada token
- Maneja comentarios de una línea (`//`) y multilínea (`/* */`)
- Soporta escape sequences en caracteres y strings

### 2. Main (Main.kt)
- Selección interactiva de archivos `.quetzal`
- Carga del código fuente
- Flujo principal de compilación
- Manejo robusto de errores con tablas dinámicas
- Funciones auxiliares para interfaz de usuario

### 3. Parser (Parser.kt)
- Implementa el algoritmo LR(1) con stack de estados y símbolos
- Lee la tabla de análisis desde CSV
- Lee la gramática desde archivo de texto
- Realiza shift/reduce/accept según la tabla

### 4. Feature Flags (FeatureFlags.kt)
- Control centralizado de flags de depuración
- Flag `DEBUG` para mostrar/ocultar información del lexer y carga de tablas
- Funciones `debugPrintln()` para output condicional

### 5. Tabla LR(1) (action_table.csv)
- 268 estados
- Acciones: `s#` (shift), `r#` (reduce), `acc` (accept)
- Formato CSV con headers en primera fila

### 6. Gramática (grammar.txt)
- 83 producciones (reglas 0-82)
- Formato: `número: no-terminal → símbolo1 símbolo2 ...`
- Ε (epsilon) para producciones vacías

## Especificación del Lenguaje Quetzal

Para más información sobre la especificación completa del lenguaje, ver:
https://arielortiz.info/s202211/tc3048/quetzal/quetzal_language_spec.html

## Ejemplo

### Archivo: `files/binary.quetzal`
```quetzal
main() {
    var x;
    x = 5;
    println();
}
```

### Salida:
```
╔════════════════════════════════════════╗
║  COMPILADOR QUETZAL - Análisis Léxico  ║
║           y Sintáctico LR(1)           ║
╚════════════════════════════════════════╝

Archivo seleccionado: .../files/binary.quetzal

=== ANÁLISIS LÉXICO ===
✓ Tokens generados: 12
  Token(type=ID, value=main, line=1, column=1)
  Token(type=LPAREN, value=(, line=1, column=5)
  ...

=== ANÁLISIS SINTÁCTICO ===
✓ Tabla cargada: 268 estados
✓ Gramática cargada: 83 producciones
✓ Análisis sintáctico completado exitosamente

╔════════════════════════════════════════╗
║    ✓ COMPILACIÓN EXITOSA              ║
╚════════════════════════════════════════╝
```

## Características Implementadas

✅ Rastreo de línea y columna en tokens  
✅ Mapeo automático TokenType → Terminal LR(1)  
✅ Algoritmo LR(1) completo con shift/reduce  
✅ Lectura de tabla CSV con 268 estados  
✅ Lectura de gramática desde archivo  
✅ Selección interactiva de archivos  
✅ Mensajes de error con ubicación exacta  
✅ Tablas de error de ancho dinámico  
✅ Fail-fast: detiene al primer error  
✅ Feature flags para control de debug output  
✅ Código modularizado en archivos separados  

## Notas Técnicas

- El símbolo de fin es `$` (no `EOF`)
- Las columnas se cuentan desde 1 (1-indexed)
- Las líneas se cuentan desde 1 (1-indexed)
- La tabla usa notación estándar LR(1): `s#`, `r#`, `acc`
- Las acciones de reducción restauran el estado GOTO automáticamente
- Feature flag `DEBUG` en `FeatureFlags.kt` controla el output de depuración

## Autores

- Diego Sahid García Galván
- Iker Landeros de la O

## Licencia

Proyecto académico - Compiladores (TC3048)
