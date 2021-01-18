package tokenizer;
import util.*;
import error.TokenizeError;
import error.ErrorCode;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
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
            return lexNumber();
        } else if (Character.isAlphabetic(peek) || peek=='_') {
            return lexIdentOrKeyword();
        } else if (peek=='"') {
            return lexString();
        } else if (peek=='\'') {
            return lexChar();
        } else {
            return lexOperatorOrUnknown();
        }
    }
    private Token lexString() throws TokenizeError {
        Pos startPos = it.currentPos();
        StringBuilder tmpToken = new StringBuilder();
        it.nextChar();
        char peek;
        while (it.isEOF()==false && (peek = it.peekChar()) != '"') {
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
    private Token lexNumber() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        Pos startPos = it.currentPos();
        StringBuilder tmpToken = new StringBuilder();
        do {
            tmpToken.append(it.nextChar());
        } while (Character.isDigit(it.peekChar()));
        if (it.peekChar() == '.') {
            tmpToken.append(it.nextChar());
            if (Character.isDigit(it.peekChar())) 
            {
                do {
                    tmpToken.append(it.nextChar());
                } while (Character.isDigit(it.peekChar()));
                if (it.peekChar() == 'E' || it.peekChar() == 'e') 
                {
                    tmpToken.append(it.nextChar());
                    if(it.peekChar()=='+'||it.peekChar()=='-')
                        tmpToken.append(it.nextChar());
                    if (Character.isDigit(it.peekChar())) 
                    {
                        do {
                            tmpToken.append(it.nextChar());
                        } while (Character.isDigit(it.peekChar()));
                        return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(tmpToken.toString()), startPos, it.currentPos());
                    } 
                    else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                } 
                else
                    return new Token(TokenType.DOUBLE_LITERAL, Double.valueOf(tmpToken.toString()), startPos, it.currentPos());
            } 
            else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        return new Token(TokenType.UINT_LITERAL, Long.valueOf(tmpToken.toString()), startPos, it.currentPos());
    }
    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        Pos pos = it.currentPos();
        String s = "";
        char peek = it.peekChar();
        while(Character.isDigit(peek) || Character.isAlphabetic(peek) || peek == '_')
        {   
            char next = it.nextChar();
            s = s + next;
            peek = it.peekChar();
        }
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        if(s.equals("fn")==true)
        {
            return new Token(TokenType.FN_KW,s,pos,it.currentPos());
        }
        else if(s.equals("let")==true)
        {
            return new Token(TokenType.LET_KW,s,pos,it.currentPos());
        }
        else if(s.equals("const")==true)
        {
            return new Token(TokenType.CONST_KW,s,pos,it.currentPos());
        }
        else if(s.equals("as")==true)
        {
            return new Token(TokenType.AS_KW,s,pos,it.currentPos());
        }
        else if(s.equals("while")==true)
        {
            return new Token(TokenType.WHILE_KW,s,pos,it.currentPos());
        }
        else if(s.equals("if")==true)
        {
            return new Token(TokenType.IF_KW,s,pos,it.currentPos());
        }
        else if(s.equals("else")==true)
        {
            return new Token(TokenType.ELSE_KW,s,pos,it.currentPos());
        }
        else if(s.equals("return")==true)
        {
            return new Token(TokenType.RETURN_KW,s,pos,it.currentPos());
        }
        else if(s.equals("break")==true)
        {
            return new Token(TokenType.BREAK_KW,s,pos,it.currentPos());
        }
        else if(s.equals("continue")==true)
        {
            return new Token(TokenType.CONTINUE_KW,s,pos,it.currentPos());
        }
        else if(s.equals("int")==true)
        {
            return new Token(TokenType.INT_KW,s,pos,it.currentPos());
        }
        else if(s.equals("double")==true)
        {
            return new Token(TokenType.DOUBLE_KW,s,pos,it.currentPos());
        }
        else if(s.equals("void")==true)
        {
            return new Token(TokenType.VOID_KW,s,pos,it.currentPos());
        }
        else
        {
            return new Token(TokenType.IDENT,s,pos,it.currentPos());
        }
        // Token 的 Value 应填写标识符或关键字的字符串
    }

    private Token lexOperatorOrUnknown() throws TokenizeError 
    {
        char peek;
        switch (it.nextChar()) 
        {
            case '+':
                return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());

            case '-':
                // 填入返回语句
                peek = it.peekChar();
                if(peek == '>')
                {
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());

                }
                else
                {
                    return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
                }

            case '*':
                // 填入返回语句
                return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());

            case '/':
                // 填入返回语句
                if (it.peekChar() == '/') {
                    it.nextChar();
                    while (it.nextChar() != '\n');
                    return nextToken();
                }
                return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());

            // 填入更多状态和返回语句
            case '=':
                peek = it.peekChar();
                if(peek == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());

                }
                else
                {
                    return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
                }
            case '!':
                peek = it.peekChar();
                if(peek == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());

                }
                else
                {
                    throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                }
            case '<':
                peek = it.peekChar();
                if(peek == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());

                }
                else
                {
                    return new Token(TokenType.LT, "<", it.previousPos(), it.currentPos());
                }
            case '>':
                peek = it.peekChar();
                if(peek == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());

                }
                else
                {
                    return new Token(TokenType.GT, ">", it.previousPos(), it.currentPos());
                }
            case ';':
                return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());

            case ':':
                return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());

            case ',':
                return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());

            case '(':
                return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());

            case ')':
                return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());

            case '{':
                return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());

            case '}':
                return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
