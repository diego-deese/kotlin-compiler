// ── Tipos de token ──────────────────────────
enum class TokenType {
    // Literales
    LIT_INT, LIT_BOOL, LIT_CHAR, LIT_STR,

    // Keywords
    AND, FALSE, OR, BREAK, IF, RETURN,
    DEC, INC, TRUE, ELIF, LOOP, VAR,
    ELSE, NOT,

    // Identificador
    ID,

    // Operadores
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQ_EQ, NOT_EQ, LT, LT_EQ, GT, GT_EQ,
    ASSIGN,

    // Separadores
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    COMMA, SEMICOLON,

    // Fin de archivo
    EOF
}

data class Token(val type: TokenType, val value: String, val line: Int, val column: Int) {
    // Retorna el nombre del terminal que espera la tabla LR(1)
    fun getTerminal(): String = tokenTypeToTerminal(this.type, this.value)
}

// ── Mapeo de TokenType a Terminal LR(1) ──────────────────────────
fun tokenTypeToTerminal(type: TokenType, value: String): String = when (type) {
    TokenType.LIT_INT -> "lit-int"
    TokenType.LIT_BOOL -> "lit-bool"
    TokenType.LIT_CHAR -> "lit-char"
    TokenType.LIT_STR -> "lit-str"
    TokenType.ID -> "id"
    TokenType.AND -> "and"
    TokenType.OR -> "or"
    TokenType.FALSE -> "lit-bool"
    TokenType.TRUE -> "lit-bool"
    TokenType.BREAK -> "break"
    TokenType.IF -> "if"
    TokenType.RETURN -> "return"
    TokenType.DEC -> "dec"
    TokenType.INC -> "inc"
    TokenType.ELIF -> "elif"
    TokenType.LOOP -> "loop"
    TokenType.VAR -> "var"
    TokenType.ELSE -> "else"
    TokenType.NOT -> "not"
    TokenType.PLUS -> "+"
    TokenType.MINUS -> "-"
    TokenType.STAR -> "*"
    TokenType.SLASH -> "/"
    TokenType.PERCENT -> "%"
    TokenType.EQ_EQ -> "=="
    TokenType.NOT_EQ -> "!="
    TokenType.LT -> "<"
    TokenType.LT_EQ -> "<="
    TokenType.GT -> ">"
    TokenType.GT_EQ -> ">="
    TokenType.ASSIGN -> "="
    TokenType.LPAREN -> "("
    TokenType.RPAREN -> ")"
    TokenType.LBRACE -> "{"
    TokenType.RBRACE -> "}"
    TokenType.LBRACKET -> "["
    TokenType.RBRACKET -> "]"
    TokenType.COMMA -> ","
    TokenType.SEMICOLON -> ";"
    TokenType.EOF -> "$"
}

// ── Keywords ─────────────────────────────────────────────────────
val KEYWORDS = mapOf(
    "and" to TokenType.AND, "false" to TokenType.FALSE, "or" to TokenType.OR,
    "break" to TokenType.BREAK, "if" to TokenType.IF, "return" to TokenType.RETURN,
    "dec" to TokenType.DEC, "inc" to TokenType.INC, "true" to TokenType.TRUE,
    "elif" to TokenType.ELIF, "loop" to TokenType.LOOP, "var" to TokenType.VAR,
    "else" to TokenType.ELSE, "not" to TokenType.NOT
)

// ── Lexer ─────────────────────────────────────────────────────────
class Lexer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var column = 1
    private var lineStartPos = 0

    private fun peek(offset: Int = 0) = source.getOrNull(pos + offset)
    private fun advance(): Char {
        val c = source[pos++]
        if (c == '\n') {
            line++
            column = 1
            lineStartPos = pos
        } else {
            column++
        }
        return c
    }
    private fun isAtEnd() = pos >= source.length
    private fun getCurrentColumn(): Int = pos - lineStartPos + 1

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (!isAtEnd()) {
            skipWhitespaceAndComments()
            if (isAtEnd()) break
            tokens.add(nextToken())
        }
        tokens.add(Token(TokenType.EOF, "", line, getCurrentColumn()))
        return tokens
    }

    private fun skipWhitespaceAndComments() {
        while (!isAtEnd()) {
            when {
                peek() == '\n' -> advance()
                peek()?.isWhitespace() == true -> advance()

                // Comentario de una línea //
                peek() == '/' && peek(1) == '/' -> {
                    while (!isAtEnd() && peek() != '\n') advance()
                }

                // Comentario multilínea /* ... */
                peek() == '/' && peek(1) == '*' -> {
                    advance(); advance() // consumir /*
                    while (!isAtEnd()) {
                        if (peek() == '*' && peek(1) == '/') {
                            advance(); advance(); break
                        }
                        advance()
                    }
                }

                else -> return
            }
        }
    }

    private fun nextToken(): Token {
        val startLine = line
        val startCol = getCurrentColumn()
        val c = advance()

        return when {
            // ── Separadores y operadores simples ──
            c == '(' -> Token(TokenType.LPAREN, "(", startLine, startCol)
            c == ')' -> Token(TokenType.RPAREN, ")", startLine, startCol)
            c == '{' -> Token(TokenType.LBRACE, "{", startLine, startCol)
            c == '}' -> Token(TokenType.RBRACE, "}", startLine, startCol)
            c == '[' -> Token(TokenType.LBRACKET, "[", startLine, startCol)
            c == ']' -> Token(TokenType.RBRACKET, "]", startLine, startCol)
            c == ',' -> Token(TokenType.COMMA, ",", startLine, startCol)
            c == ';' -> Token(TokenType.SEMICOLON, ";", startLine, startCol)
            c == '+' -> Token(TokenType.PLUS, "+", startLine, startCol)
            c == '*' -> Token(TokenType.STAR, "*", startLine, startCol)
            c == '%' -> Token(TokenType.PERCENT, "%", startLine, startCol)
            c == '/' -> Token(TokenType.SLASH, "/", startLine, startCol)

            // ── Operadores de dos caracteres ──
            c == '=' && peek() == '=' -> { advance(); Token(TokenType.EQ_EQ, "==", startLine, startCol) }
            c == '=' -> Token(TokenType.ASSIGN, "=", startLine, startCol)
            c == '!' && peek() == '=' -> { advance(); Token(TokenType.NOT_EQ, "!=", startLine, startCol) }
            c == '<' && peek() == '=' -> { advance(); Token(TokenType.LT_EQ, "<=", startLine, startCol) }
            c == '<' -> Token(TokenType.LT, "<", startLine, startCol)
            c == '>' && peek() == '=' -> { advance(); Token(TokenType.GT_EQ, ">=", startLine, startCol) }
            c == '>' -> Token(TokenType.GT, ">", startLine, startCol)

            // ── Enteros (incluyendo negativos con -) ──
            c == '-' && peek()?.isDigit() == true -> {
                val sb = StringBuilder("-")
                while (peek()?.isDigit() == true) sb.append(advance())
                Token(TokenType.LIT_INT, sb.toString(), startLine, startCol)
            }
            c == '-' -> Token(TokenType.MINUS, "-", startLine, startCol)
            c.isDigit() -> {
                val sb = StringBuilder(c.toString())
                while (peek()?.isDigit() == true) sb.append(advance())
                Token(TokenType.LIT_INT, sb.toString(), startLine, startCol)
            }

            // ── Caracteres 'x' ──
            c == '\'' -> {
                val ch = readCharContent()
                if (peek() != '\'') throw CompileError("Se esperaba ' al cerrar char literal en línea $startLine, columna $startCol")
                advance()
                Token(TokenType.LIT_CHAR, ch, startLine, startCol)
            }

            // ── Strings "..." ──
            c == '"' -> {
                val sb = StringBuilder()
                while (!isAtEnd() && peek() != '"') {
                    if (peek() == '\\') sb.append(readEscapeSequence())
                    else sb.append(advance())
                }
                if (isAtEnd()) throw CompileError("String sin cerrar en línea $startLine, columna $startCol")
                advance() // consumir "
                Token(TokenType.LIT_STR, sb.toString(), startLine, startCol)
            }

            // ── Identificadores y keywords ──
            c.isLetter() -> {
                val sb = StringBuilder(c.toString())
                while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true)
                    sb.append(advance())
                val word = sb.toString()
                val type = KEYWORDS[word] ?: TokenType.ID
                Token(type, word, startLine, startCol)
            }

            else -> throw CompileError("Error en línea $startLine, columna $startCol: carácter inesperado '$c'")
        }
    }

    private fun readCharContent(): String {
        return if (peek() == '\\') readEscapeSequence() else advance().toString()
    }

    private fun readEscapeSequence(): String {
        advance() // consumir '\'
        return when (val esc = advance()) {
            'n' -> "\n"; 'r' -> "\r"; 't' -> "\t"
            '\\' -> "\\"; '\'' -> "'"; '"' -> "\""
            'u' -> {
                // \uhhhhhh — 6 dígitos hex
                val hex = (1..6).map { advance() }.joinToString("")
                String(Character.toChars(hex.toInt(16)))
            }
            else -> throw CompileError("Secuencia de escape inválida: \\$esc en línea $line, columna $column")
        }
    }
}
