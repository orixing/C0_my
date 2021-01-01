package tokenizer;

import error.ErrorCode;
import error.TokenizeError;
import util.Pos;

public class Tokenizer {
    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUIntOrDouble();
        } else if (Character.isAlphabetic(peek) || peek == '_') {
            return lexIdentOrKeyword();
        } else if (peek == '"') {
            return lexString();
        } else if (peek == '\'') {
            return lexChar();
        } else {
            return lexOperatorOrCommentOrUnknown();
        }
    }

    private Token lexOperatorOrCommentOrUnknown() throws TokenizeError {
        Pos startPos;
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
            case '-':
                startPos = it.previousPos();
                if (it.peekChar() == '>') {
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", startPos, it.currentPos());
                }
                return new Token(TokenType.MINUS, '-', startPos, it.currentPos());
            case '*':
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
            case '/':
                startPos = it.previousPos();
                if (it.peekChar() == '/') {
                    it.nextChar();
//                    StringBuilder tmpToken = new StringBuilder();
//                    tmpToken.append("//");
                    while (it.nextChar() != '\n');
//                        tmpToken.append(it.nextChar());
//                    tmpToken.append(it.nextChar());
//                    return new Token(TokenType.COMMENT, tmpToken.toString(), startPos, it.currentPos());
                    return nextToken();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
            case '=':
                startPos = it.previousPos();
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", startPos, it.currentPos());
                }
                return new Token(TokenType.ASSIGN, '=', startPos, it.currentPos());
            case '!':
                startPos = it.previousPos();
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", startPos, it.currentPos());
                } else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            case '<':
                startPos = it.previousPos();
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", startPos, it.currentPos());
                }
                return new Token(TokenType.LT, '<', startPos, it.currentPos());
            case '>':
                startPos = it.previousPos();
                if (it.peekChar() == '=') {
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", startPos, it.currentPos());
                }
                return new Token(TokenType.GT, '>', startPos, it.currentPos());
            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());
            default:
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private Token lexChar() throws TokenizeError {
        Pos startPos = it.currentPos();
        char value;
        it.nextChar();
        char peek = it.peekChar();
        if (peek != '\'') {
            if (peek == '\\') {
                it.nextChar();
                switch (it.nextChar()) {
                    case '\\':
                        value = '\\';
                        break;
                    case '"':
                        value = '"';
                        break;
                    case '\'':
                        value = '\'';
                        break;
                    case 'n':
                        value = '\n';
                        break;
                    case 'r':
                        value = '\r';
                        break;
                    case 't':
                        value = '\t';
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            } else value = it.nextChar();
            if (it.peekChar() == '\'') {
                it.nextChar();
                return new Token(TokenType.CHAR_LITERAL, value, startPos, it.currentPos());
            } else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
    }

    private Token lexString() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder tmpToken = new StringBuilder();
        it.nextChar();
        char peek;
        while (!it.isEOF() && (peek = it.peekChar()) != '"') {
            if (peek == '\\') {
                it.nextChar();
                switch (it.nextChar()) {
                    case '\\':
                        tmpToken.append('\\');
                        break;
                    case '"':
                        tmpToken.append('"');
                        break;
                    case '\'':
                        tmpToken.append('\'');
                        break;
                    case 'n':
                        tmpToken.append('\n');
                        break;
                    case 'r':
                        tmpToken.append('\r');
                        break;
                    case 't':
                        tmpToken.append('\t');
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            } else tmpToken.append(it.nextChar());
        }
        if (it.isEOF()) {
            throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        it.nextChar();
        return new Token(TokenType.STRING_LITERAL, tmpToken.toString(), startPos, it.currentPos());
    }

    private Token lexIdentOrKeyword() {
        Pos startPos = it.currentPos();
        StringBuilder tmpToken = new StringBuilder();
        do {
            tmpToken.append(it.nextChar());
        } while (Character.isLetterOrDigit(it.peekChar()) || it.peekChar() == '_');
        String readyToken = tmpToken.toString();
        TokenType tmpType;
        switch (readyToken) {
            case "fn":
                tmpType = TokenType.FN_KW;
                break;
            case "let":
                tmpType = TokenType.LET_KW;
                break;
            case "const":
                tmpType = TokenType.CONST_KW;
                break;
            case "as":
                tmpType = TokenType.AS_KW;
                break;
            case "while":
                tmpType = TokenType.WHILE_KW;
                break;
            case "if":
                tmpType = TokenType.IF_KW;
                break;
            case "else":
                tmpType = TokenType.ELSE_KW;
                break;
            case "return":
                tmpType = TokenType.RETURN_KW;
                break;
            case "break":
                tmpType = TokenType.BREAK_KW;
                break;
            case "continue":
                tmpType = TokenType.CONTINUE_KW;
                break;
            case "int":
                tmpType=TokenType.INT_KW;
                break;
            case "void":
                tmpType=TokenType.VOID_KW;
                break;
            case "double":
                tmpType=TokenType.DOUBLE_KW;
                break;
            default:
                tmpType = TokenType.IDENT;
        }
        return new Token(tmpType, readyToken, startPos, it.currentPos());
    }

    private Token lexUIntOrDouble() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder tmpToken = new StringBuilder();
        do {
            tmpToken.append(it.nextChar());
        } while (Character.isDigit(it.peekChar()));
        if (it.peekChar() == '.') {
            tmpToken.append(it.nextChar());
            if (Character.isDigit(it.peekChar())) {
                do {
                    tmpToken.append(it.nextChar());
                } while (Character.isDigit(it.peekChar()));
                if (it.peekChar() == 'E' || it.peekChar() == 'e') {
                    tmpToken.append(it.nextChar());
                    if(it.peekChar()=='+'||it.peekChar()=='-')
                        tmpToken.append(it.nextChar());
                    if (Character.isDigit(it.peekChar())) {
                        do {
                            tmpToken.append(it.nextChar());
                        } while (Character.isDigit(it.peekChar()));
                        return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(tmpToken.toString()), startPos, it.currentPos());
                    } else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                } else
                    return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(tmpToken.toString()), startPos, it.currentPos());
            } else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        return new Token(TokenType.UINT_LITERAL, Long.valueOf(tmpToken.toString()), startPos, it.currentPos());
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
