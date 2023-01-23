package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        for (Ast.Global globals : ast.getGlobals())
            visit(globals);

        for (Ast.Function functions : ast.getFunctions())
            visit(functions);

        if ((scope.lookupFunction("main", 0) == null) || !(scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER)))
            throw new RuntimeException("Missing main function with integer return type");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {

        Environment.Type type = Environment.getType(ast.getTypeName());

        if (ast.getValue().isPresent())
        {
            if(ast.getValue().get() instanceof Ast.Expression.PlcList)
            {
                Ast.Expression.PlcList temp = (Ast.Expression.PlcList) ast.getValue().get();
                temp.setType(type);
            }

            visit(ast.getValue().get());
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, ast.getMutable(), Environment.NIL));
            requireAssignable(type, ast.getValue().get().getType());
        }
        else
        {
            ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, ast.getMutable(), Environment.NIL));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        List<Environment.Type> parameterTypes = new ArrayList<>();

        for (String s : ast.getParameterTypeNames())
            parameterTypes.add(Environment.getType(s));

        Environment.Type returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent())
            returnType = Environment.getType(ast.getReturnTypeName().get());

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args->Environment.NIL));

        try
        {
            scope = new Scope(scope);

            for (int i = 0; i < ast.getParameters().size(); i++)
                scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), true, Environment.NIL );

            for (Ast.Statement statements : ast.getStatements())
            {
                visit(statements);

                if (statements instanceof Ast.Statement.Return)
                {
                    requireAssignable(returnType, ((Ast.Statement.Return) statements).getValue().getType());
                    break;
                }
            }
        }
        finally
        {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        if (!(ast.getExpression() instanceof Ast.Expression.Function))
            throw new RuntimeException("Error for ast.expression");

        visit(ast.getExpression());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent())
            throw new RuntimeException("Value or type is missing in Ast.Statement.Declaration");

        Environment.Type type = null;

        if (ast.getTypeName().isPresent())
            type = Environment.getType(ast.getTypeName().get());

        if (ast.getValue().isPresent())
        {
            visit(ast.getValue().get());

            if (type == null)
                type = ast.getValue().get().getType();

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        if (!(ast.getReceiver() instanceof Ast.Expression.Access))
            throw new RuntimeException("Error in ast.statement.assignment");

        visit(ast.getReceiver());
        visit(ast.getValue());

        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        visit(ast.getCondition());
        if (!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN)))
            throw new RuntimeException("While loop condition must be a boolean value");
        if (ast.getThenStatements().isEmpty())
            throw new RuntimeException("Missing then statements in if");

        try
        {
            scope = new Scope(scope);

            for (Ast.Statement i : ast.getThenStatements())
                visit(i);
        }
        finally
        {
            scope = scope.getParent();
        }

        try
        {
            scope = new Scope(scope);

            for (Ast.Statement i : ast.getElseStatements())
                visit(i);
        }
        finally
        {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {

        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();

        for (int i = 0; i < ast.getCases().size() - 1; i++)
        {

            if (!ast.getCases().get(i).getValue().isPresent())
                throw new RuntimeException("Expected DEFAULT to be last case in the switch statement");

            visit(ast.getCases().get(i).getValue().get());

            if (!(ast.getCases().get(i).getValue().get().getType().equals(conditionType)))
                throw new RuntimeException("Error in ast.statement.switch");
        }

        int defaultLocation = ast.getCases().size() - 1;

        if (ast.getCases().get(defaultLocation).getValue().isPresent())
            throw new RuntimeException("Error in ast.statement.switch");

        for (Ast.Statement.Case i : ast.getCases())
        {
            try
            {
                scope = new Scope(scope);

                visit(i);
            }
            finally
            {
                scope = scope.getParent();
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        for (int i = 0; i < ast.getStatements().size(); i++)
            visit(ast.getStatements().get(i));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        visit(ast.getCondition());
        if (!(ast.getCondition().getType().equals(Environment.Type.BOOLEAN)))
            throw new RuntimeException("While loop condition must be a boolean value");

        try
        {
            scope = new Scope(scope);

            for (Ast.Statement i : ast.getStatements())
                visit(i);
        }
        finally
        {
            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {

        visit(ast.getValue());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        Object literal = ast.getLiteral();

        if (literal.equals(Environment.NIL))
        {
            ast.setType(Environment.Type.NIL);
        }
        else if (literal instanceof Boolean)
        {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (literal instanceof  Character)
        {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (literal instanceof String)
        {
            ast.setType(Environment.Type.STRING);
        }
        else if (literal instanceof BigInteger)
        {
            if (((BigInteger) literal).bitLength() > 31)
                throw new RuntimeException("Integer out of bounds, greater than 32-bit signed value");

            ast.setType(Environment.Type.INTEGER);
        }
        else if (literal instanceof BigDecimal)
        {
            if (Double.isInfinite(((BigDecimal) literal).doubleValue()))
                throw new RuntimeException("Decimal out of bounds, greater than 64-bit signed value");

            ast.setType(Environment.Type.DECIMAL);
        }
        else
        {
            throw new RuntimeException("Literal is not a valid type");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        if (!(ast.getExpression() instanceof Ast.Expression.Binary))
            throw new RuntimeException("Contained expression must be a binary expression");

        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        String op = ast.getOperator();
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        if (op.equals("&&") || op.equals("||"))
        {
            requireAssignable(Environment.Type.BOOLEAN, leftType);
            requireAssignable(Environment.Type.BOOLEAN, rightType);

            ast.setType(Environment.Type.BOOLEAN);
        }

        else if (op.equals("<") || op.equals(">") || op.equals("==") || op.equals("!="))
        {
            requireAssignable(Environment.Type.COMPARABLE, leftType);
            requireAssignable(Environment.Type.COMPARABLE, rightType);

            if (!leftType.equals(rightType))
                throw new RuntimeException("Both operands are not the same comparable type");

            ast.setType(Environment.Type.BOOLEAN);
        }

        else if (op.equals("+"))
        {
            if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING))
            {
                ast.setType(Environment.Type.STRING);
            }

            else
            {
                if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER))
                    ast.setType(Environment.Type.INTEGER);
                else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL))
                    ast.setType(Environment.Type.DECIMAL);
                else
                    throw new RuntimeException("Invalid operand types");
            }
        }

        else if (op.equals("-") || op.equals("*") || op.equals("/"))
        {
            if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER))
                ast.setType(Environment.Type.INTEGER);
            else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL))
                ast.setType(Environment.Type.DECIMAL);
            else
                throw new RuntimeException("Invalid operand types");
        }

        else if (op.equals("^"))
        {
            if (!(leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)))
                throw new RuntimeException("Invalid operand types, expected INTEGER");

            ast.setType(Environment.Type.INTEGER);
        }

        else
        {
            throw new RuntimeException("Invalid operator");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        if (ast.getOffset().isPresent())
            if (!(ast.getOffset().get().getType().equals(Environment.Type.INTEGER)))
                throw new RuntimeException("Invalid access offset");

        ast.setVariable(scope.lookupVariable(ast.getName()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));

        for (int i = 0; i < ast.getFunction().getArity(); i++)
        {
            visit(ast.getArguments().get(i));
            requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        for (Ast.Expression i : ast.getValues())
        {
            visit(i);
            requireAssignable(ast.getType(), i.getType());
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {

        if (!target.equals(type) && !target.equals(Environment.Type.ANY) && !target.equals(Environment.Type.COMPARABLE))
            throw new RuntimeException("Type mismatch, target type does not match given type");

        if (target.equals(Environment.Type.COMPARABLE))
            if (!type.equals(Environment.Type.INTEGER) && !type.equals(Environment.Type.DECIMAL) && !type.equals(Environment.Type.CHARACTER) && !type.equals(Environment.Type.STRING) && !type.equals(Environment.Type.COMPARABLE))
                throw new RuntimeException("Type mismatch, assigned type is not an integer, decimal, character, or string (comparable types)");
    }
}
