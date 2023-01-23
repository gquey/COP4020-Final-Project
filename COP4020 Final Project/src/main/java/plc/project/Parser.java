package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    //source = global* function*
    public Ast.Source parseSource() throws ParseException {

        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();
        if (!tokens.has(0))
            return new Ast.Source(globals, functions);


        while (peek("LIST") || peek("VAR") || peek("VAL"))
            globals.add(parseGlobal());

        while (peek("FUN"))
            functions.add(parseFunction());

        if (tokens.has(0))
            throw new ParseException("Exception in SOURCE, expected FUN at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {

        Ast.Global result;

        if (peek("LIST"))
            result = parseList();
        else if (peek("VAR"))
            result = parseMutable();
        else //peek("VAL")
            result = parseImmutable();

        if (!match(";"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in GLOBAL, expected ';' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in GLOBAL, expected ';' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return result;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {

        List<Ast.Expression> value = new ArrayList<>();
        String name;
        String type;
        match("LIST");

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        name = tokens.get(-1).getLiteral();

        if (!match(":"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        type = tokens.get(-1).getLiteral();

        if (!match("="))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected '=' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected '=' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        if (!match("["))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected '[' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected '[' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        value.add(parseExpression());
        while (match(","))
            value.add(parseExpression());

        if (!match("]"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LIST, expected ']' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LIST, expected ']' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Global(name, type, true, Optional.of(new Ast.Expression.PlcList(value)));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {

        String name;
        String type;
        Optional<Ast.Expression> value = Optional.empty();
        match("VAR");

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in MUTABLE, expected an identifier after VAR at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in MUTABLE, expected an identifier after VAR at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        name = tokens.get(-1).getLiteral();

        if (!match(":"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in MUTABLE, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in MUTABLE, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in MUTABLE, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in MUTABLE, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        type = tokens.get(-1).getLiteral();

        if (match("="))
            value = Optional.of(parseExpression());

        return new Ast.Global(name, type, true, value);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {

        String name;
        String type;
        match("VAL");

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IMMUTABLE, expected an identifier after VAL at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IMMUTABLE, expected an identifier after VAL at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        name = tokens.get(-1).getLiteral();

        if (!match(":"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IMMUTABLE, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IMMUTABLE, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IMMUTABLE, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IMMUTABLE, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        type = tokens.get(-1).getLiteral();

        if (!match("="))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IMMUTABLE, expected '=' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IMMUTABLE, expected '=' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Global(name, type, false, Optional.of(parseExpression()));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {

        Optional<String> returnType = Optional.empty();
        match("FUN");

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in FUN, expected an identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in FUN, expected an identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        String name = tokens.get(-1).getLiteral();

        if (!match("("))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in FUN, expected '(' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in FUN, expected '(' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        List<String> parameters = new ArrayList<>();
        List<String> parameterTypes = new ArrayList<>();
        if (!peek(")"))
        {
            if (!match(Token.Type.IDENTIFIER))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in FUN, expected an identifier or ')' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in FUN, expected an identifier or ')' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
            parameters.add(tokens.get(-1).getLiteral());

            if (!match(":"))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in FUN, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in FUN, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }

            if (!match(Token.Type.IDENTIFIER))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in FUN, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in FUN, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
            parameterTypes.add(tokens.get(-1).getLiteral());

            while (match(","))
            {
                if (!match(Token.Type.IDENTIFIER))
                {
                    if (tokens.has(0))
                        throw new ParseException("Exception in FUN, expected an identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    throw new ParseException("Exception in FUN, expected an identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }
                parameters.add(tokens.get(-1).getLiteral());

                if (!match(":"))
                {
                    if (tokens.has(0))
                        throw new ParseException("Exception in FUN, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    throw new ParseException("Exception in FUN, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }

                if (!match(Token.Type.IDENTIFIER))
                {
                    if (tokens.has(0))
                        throw new ParseException("Exception in FUN, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    throw new ParseException("Exception in FUN, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }
                parameterTypes.add(tokens.get(-1).getLiteral());
            }
        }

        if (!match(")"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in FUN, expected ')' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in FUN, expected ')' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        if (match(":"))
        {
            if (!match(Token.Type.IDENTIFIER))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in FUN, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in FUN, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
            returnType = Optional.of(tokens.get(-1).getLiteral());
        }

        if (!match("DO"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in FUN, expected DO at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in FUN, expected DO at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        List<Ast.Statement> statements = parseBlock();

        if (!match("END"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in FUN, expected END at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in FUN, expected END at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Function(name, parameters, parameterTypes, returnType, statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {

        List<Ast.Statement> statements = new ArrayList<>();

        while((!(peek("END") || peek("DEFAULT") || peek("ELSE") || peek("CASE"))) && tokens.has(0))
            statements.add(parseStatement());

        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {

        Ast.Statement result;
        if (peek("LET"))
        {
            result = parseDeclarationStatement();
        }
        else if (peek("SWITCH"))
        {
            result = parseSwitchStatement();
        }
        else if (peek("IF"))
        {
            result = parseIfStatement();
        }
        else if (peek("WHILE"))
        {
            result = parseWhileStatement();
        }
        else if (peek("RETURN"))
        {
            result = parseReturnStatement();
        }
        else
        {
            Ast.Expression lhs = parseExpression();

            if (match("="))
                result = new Ast.Statement.Assignment(lhs, parseExpression());
            else
                result = new Ast.Statement.Expression(lhs);

            if (!match(";"))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in STATEMENT, expected ';' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in STATEMENT, expected ';' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }

        return result;
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {

        Optional<String> type = Optional.empty();
        Optional<Ast.Expression> value = Optional.empty();
        match("LET");

        if (!match(Token.Type.IDENTIFIER))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LET, expected an identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LET, expected an identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        String name = tokens.get(-1).getLiteral();

        if (match(":"))
        {
            if (!match(Token.Type.IDENTIFIER))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in FUN, expected identifier at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in FUN, expected identifier at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
            type = Optional.of(tokens.get(-1).getLiteral());
        }

        if (match("="))
            value = Optional.of(parseExpression());

        if (!match(";"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in LET, expected ';' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in LET, expected ';' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Statement.Declaration(name, type, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {

        match("IF");

        Ast.Expression condition = parseExpression();

        if (!match("DO"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IF, expected DO at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IF, expected DO at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        List<Ast.Statement> then = parseBlock();

        List<Ast.Statement> otherwise = new ArrayList<>();
        if (match("ELSE"))
            otherwise = parseBlock();

        if (!match("END"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in IF, expected END at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in IF, expected END at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Statement.If(condition, then, otherwise);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {

        match("SWITCH");

        Ast.Expression condition = parseExpression();

        List<Ast.Statement.Case> cases = new ArrayList<>();
        while (peek("CASE"))
            cases.add(parseCaseStatement());

        if (!peek("DEFAULT"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in SWITCH, expected DEFAULT at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in SWITCH, expected DEFAULT at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        cases.add(parseCaseStatement());

        if (!match("END"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in SWITCH, expected END at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in SWITCH, expected END at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Statement.Switch(condition, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {

        Optional<Ast.Expression> value = Optional.empty();
        List<Ast.Statement> statements;

        if (match("CASE"))
        {
            value = Optional.of(parseExpression());

            if (!match(":"))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in CASE, expected ':' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in CASE, expected ':' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }

        else
        {
            match("DEFAULT");
        }

        statements = parseBlock();

        return new Ast.Statement.Case(value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {

        match("WHILE");

        Ast.Expression condition = parseExpression();

        if (!match("DO"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in WHILE, expected DO at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in WHILE, expected DO at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
        List<Ast.Statement> statements = parseBlock();

        if (!match("END"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in WHILE, expected END at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in WHILE, expected END at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {

        match("RETURN");

        Ast.Expression value = parseExpression();

        if (!match(";"))
        {
            if (tokens.has(0))
                throw new ParseException("Exception in RETURN, expected ';' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in RETURN, expected ';' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }

        return new Ast.Statement.Return(value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {

        Ast.Expression result = parseComparisonExpression();
        while(peek("&&") || peek("||"))
        {
            String operator = "||";
            if (match("&&"))
                operator = "&&";
            else
                match("||");

            Ast.Expression rhs = parseComparisonExpression();
            result = new Ast.Expression.Binary(operator, result, rhs);
        }

        return result;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {

        Ast.Expression result = parseAdditiveExpression();
        while(peek("<") || peek(">") || peek("==") || peek("!="))
        {
            String operator = "!=";
            if (match("<"))
                operator = "<";
            else if (match(">"))
                operator = ">";
            else if (match("=="))
                operator = "==";
            else
                match("!=");

            Ast.Expression rhs = parseAdditiveExpression();
            result = new Ast.Expression.Binary(operator, result, rhs);
        }

        return result;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {

        Ast.Expression result = parseMultiplicativeExpression();
        while(peek("+") || peek("-"))
        {
            String operator = "-";
            if (match("+"))
                operator = "+";
            else
                match("-");

            Ast.Expression rhs = parseMultiplicativeExpression();
            result = new Ast.Expression.Binary(operator, result, rhs);
        }

        return result;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {

        Ast.Expression result = parsePrimaryExpression();
        while(peek("*") || peek("/") || peek("^"))
        {
            String operator = "^";
            if (match("*"))
                operator = "*";
            else if (match("/"))
                operator = "/";
            else
                match("^");

            Ast.Expression rhs = parsePrimaryExpression();
            result = new Ast.Expression.Binary(operator, result, rhs);
        }

        return result;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {

        Ast.Expression result = null;

        if (match("NIL"))
        {
            result = new Ast.Expression.Literal(null);
        }

        else if (match("TRUE"))
        {
            result = new Ast.Expression.Literal(Boolean.TRUE);
        }

        else if (match("FALSE"))
        {
            result = new Ast.Expression.Literal(Boolean.FALSE);
        }

        else if (match(Token.Type.INTEGER))
        {
            result = new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }

        else if (match(Token.Type.DECIMAL))
        {
            result = new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }

        else if (match(Token.Type.CHARACTER))
        {
            String literalChar = tokens.get(-1).getLiteral();
            String newLitChar = literalChar.substring(1, literalChar.length() - 1);

            if (newLitChar.contains("\\b"))
                newLitChar = newLitChar.replace("\\b", "\b");
            else if (newLitChar.contains("\\n"))
                newLitChar = newLitChar.replace("\\n", "\n");
            else if (newLitChar.contains("\\r"))
                newLitChar = newLitChar.replace("\\r", "\r");
            else if (newLitChar.contains("\\t"))
                newLitChar = newLitChar.replace("\\t", "\t");
            else if (newLitChar.contains("\\'"))
                newLitChar = newLitChar.replace("\\'", "'");
            else if (newLitChar.contains("\\\""))
                newLitChar = newLitChar.replace("\\\"", "\"");
            else if (newLitChar.contains("\\\\"))
                newLitChar = newLitChar.replace("\\\\", "\\");

            result = new Ast.Expression.Literal(newLitChar.charAt(0));
        }

        else if (match(Token.Type.STRING))
        {
            String literalString = tokens.get(-1).getLiteral();
            String newLitString = literalString.substring(1, literalString.length() - 1);

            if (newLitString.contains("\\b"))
                newLitString = newLitString.replace("\\b", "\b");
            else if (newLitString.contains("\\n"))
                newLitString = newLitString.replace("\\n", "\n");
            else if (newLitString.contains("\\r"))
                newLitString = newLitString.replace("\\r", "\r");
            else if (newLitString.contains("\\t"))
                newLitString = newLitString.replace("\\t", "\t");
            else if (newLitString.contains("\\'"))
                newLitString = newLitString.replace("\\'", "'");
            else if (newLitString.contains("\\\""))
                newLitString = newLitString.replace("\\\"", "\"");
            else if (newLitString.contains("\\\\"))
                newLitString = newLitString.replace("\\\\", "\\");

            result = new Ast.Expression.Literal(newLitString);
        }

        else if (match("("))
        {
            result = new Ast.Expression.Group(parseExpression());

            if (!match(")"))
            {
                if (tokens.has(0))
                    throw new ParseException("Exception in PRIMARY_EXPRESSION grouping, expected ')' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                throw new ParseException("Exception in PRIMARY_EXPRESSION grouping, expected ')' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
            }
        }

        else if (match(Token.Type.IDENTIFIER))
        {
            String identifierName = tokens.get(-1).getLiteral();

            if (match("("))
            {
                List<Ast.Expression> parameters = new ArrayList<>();

                if (!peek(")"))
                {
                    Ast.Expression firstExpression = parseExpression();
                    parameters.add(firstExpression);

                    while (match(","))
                        parameters.add(parseExpression());
                }

                if (!match(")"))
                {
                    if (tokens.has(0))
                        throw new ParseException("Exception in PRIMARY_EXPRESSION function call, expected ')' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    throw new ParseException("Exception in PRIMARY_EXPRESSION function call, expected ')' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }

                result = new Ast.Expression.Function(identifierName, parameters);
            }

            else if (match("["))
            {
                Ast.Expression expression = parseExpression();

                if (!match("]"))
                {
                    if (tokens.has(0))
                        throw new ParseException("Exception in PRIMARY_EXPRESSION list access, expected ']' at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
                    throw new ParseException("Exception in PRIMARY_EXPRESSION list access, expected ']' at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
                }

                result = new Ast.Expression.Access(Optional.of(expression), identifierName);
            }

            else
            {
                result = new Ast.Expression.Access(Optional.empty(), identifierName);
            }
        }

        if (result != null)
        {
            return result;
        }

        else
        {
            if (tokens.has(0))
                throw new ParseException("Exception in parseExpression, invalid expression at index " + tokens.get(0).getIndex(), tokens.get(0).getIndex());
            throw new ParseException("Exception in parseExpression, invalid expression at index " + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()), (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length()));
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }
}
