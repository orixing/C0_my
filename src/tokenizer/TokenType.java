package tokenizer;

public enum TokenType {
    /** 空 */
    None,
    /** fn */
    FN_KW,
    /** let */
    LET_KW,
    /** const */
    CONST_KW,
    /** as */
    AS_KW,
    /** while */
    WHILE_KW,
    /** if */
    IF_KW,
    /** else */
    ELSE_KW,
    /** return */
    RETURN_KW,
    /** break */
    BREAK_KW,
    /** continue */
    CONTINUE_KW,
    /** 无符号整数 */
    UINT_LITERAL,
    /** 字符串常量 */
    STRING_LITERAL,
    /** 浮点数常量 */
    DOUBLE_LITERAL,
    /** 字符常量 */
    CHAR_LITERAL,
    /** int */
    INT_KW,
    /** void */
    VOID_KW,
    /** double */
    DOUBLE_KW,
    /** 标识符 */
    IDENT,
    /** as */
    PLUS,
    /** while */
    MINUS,
    /** if */
    MUL,
    /** else */
    DIV,
    /** return */
    ASSIGN,
    /** break */
    EQ,
    /** continue */
    NEQ,
    /** 无符号整数 */
    LT,
    /** 字符串常量 */
    GT,
    /** 浮点数常量 */
    LE,
    /** 字符常量 */
    GE,
    /** break */
    L_PAREN,
    /** continue */
    R_PAREN,
    /** 无符号整数 */
    L_BRACE,
    /** 字符串常量 */
    R_BRACE,
    /** 浮点数常量 */
    ARROW,
    /** 字符常量 */
    COMMA,
    /** 无符号整数 */
    COLON,
    /** 字符串常量 */
    SEMICOLON,
    /** 浮点数常量 */
    COMMENT ,
    /** 文件尾 */
    EOF;


    @Override
    public String toString() {
        switch (this) {
            case None:
                return "NullToken";
            case FN_KW:
                return "fn";
            case LET_KW:
                return "let";
            case CONST_KW:
                return "const";
            case AS_KW:
                return "as";
            case WHILE_KW:
                return "while";
            case IF_KW:
                return "if";
            case ELSE_KW:
                return "else";
            case RETURN_KW:
                return "return";
            case BREAK_KW:
                return "break";
            case CONTINUE_KW:
                return "continue";
            case UINT_LITERAL:
                return "uint";
            case STRING_LITERAL:
                return "string";
            case DOUBLE_LITERAL:
                return "double";
            case CHAR_LITERAL:
                return "char";
            case IDENT:
                return "ident";
            case PLUS:
                return "plus";
            case MINUS:
                return "minus";
            case DIV:
                return "div";
            case EQ:
                return "equal";
            case NEQ:
                return "nequal";
            case LT:
                return "less";
            case GT:
                return "greater";
            case LE:
                return "lessequal";
            case GE:
                return "greaterequal";
            case L_PAREN:
                return "lparen";
            case R_PAREN:
                return "rparen";
            case L_BRACE:
                return "lbrace";
            case R_BRACE:
                return "rbrace";
            case ARROW:
                return "arrow";
            case COMMA:
                return "comma";
            case COLON:
                return "colon";
            case SEMICOLON:
                return "semicolon";
            case COMMENT:
                return "comment";
            default:
                return "InvalidToken";
        }
    }
}