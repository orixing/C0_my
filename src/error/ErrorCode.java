package error;

public enum ErrorCode {
    NoError, // Should be only used internally.
    StreamError, EOF, InvalidInput, InvalidIdentifier, IntegerOverflow, // int32_t overflow.
    NoBegin, NoEnd, NeedIdentifier, ConstantNeedValue, NoSemicolon, InvalidDeclaration, IncompleteExpression,
    NotDeclared, AssignToConstant, DuplicateDeclaration, NotInitialized, InvalidAssignment, InvalidPrint, ExpectedToken,
    IllegalEscapeSequence, MissingReturnStatement, UnreachableStatement, BreakOutsideLoop, ContinueOutsideLoop,
    InvalidType,NeedMainFunction
}
