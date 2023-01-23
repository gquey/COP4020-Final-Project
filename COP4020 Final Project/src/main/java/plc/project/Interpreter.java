package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        List<Ast.Global> globals = ast.getGlobals();
        for (Ast.Global cur : globals)
            visit(cur);

        List<Ast.Function> functions = ast.getFunctions();
        for (Ast.Function cur : functions)
            visit(cur);

        // Expected to throw runtimeException if main does not exist
        List<Environment.PlcObject> args = new ArrayList<>();
        return scope.lookupFunction("main", 0).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        if (ast.getValue().isPresent())
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        else
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        Scope childScope = new Scope(scope);
        try
        {
            int arity = ast.getParameters().size();
            scope.defineFunction(ast.getName(), arity, args ->
            {
                scope = childScope;
                try
                {
                    for (int i = 0; i < arity; i++)
                    {
                        String curName = ast.getParameters().get(i);
                        scope.defineVariable(curName, true, args.get(i));
                    }

                    ast.getStatements().forEach(this::visit);

                    return Environment.NIL;
                }
                catch(Return r)
                {
                    return r.value;
                }
            });
        }
        finally
        {
            scope = childScope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        Optional<Ast.Expression> optional = ast.getValue();

        if (optional.isPresent())
            scope.defineVariable(ast.getName(), true, visit(optional.get()));
        else
            scope.defineVariable(ast.getName(), true, Environment.NIL);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if (!(ast.getReceiver() instanceof Ast.Expression.Access))
            throw new RuntimeException("Expected Ast.Expression.Access type receiver");

        Ast.Expression.Access receiver = (Ast.Expression.Access) ast.getReceiver();
        visit(receiver);

        if (!(scope.lookupVariable(receiver.getName()).getMutable()))
            throw new RuntimeException("Exception in visit(Ast.Statement.Assignment): Cannot assign to an immutable variable");

        Environment.PlcObject value = visit(ast.getValue());

        if (receiver.getOffset().isPresent())
        {
            int offset = requireType(BigInteger.class, visit(receiver.getOffset().get())).intValue();
            ((List<Object>) scope.lookupVariable(receiver.getName()).getValue().getValue()).set(offset, visit(ast.getValue()).getValue());
        }
        else
        {
            scope.lookupVariable(receiver.getName()).setValue(value);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {

        Boolean condition = requireType(Boolean.class, visit(ast.getCondition()));
        try
        {
            scope = new Scope(scope);

            if (condition)
                ast.getThenStatements().forEach(this::visit);

            else
                ast.getElseStatements().forEach(this::visit);
        }
        finally
        {
            scope = scope.getParent();
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        
        try
        {
            scope = new Scope(scope);

            Environment.PlcObject condition = visit(ast.getCondition());
            List<Ast.Statement.Case> cases = ast.getCases();
            for (int i = 0; i < cases.size() - 1; i++)
            {
                if (visit(cases.get(i)).getValue().equals(condition.getValue()))
                {
                    cases.get(i).getStatements().forEach(this::visit);
                    return Environment.NIL;
                }
            }

            if (cases.get(cases.size() - 1).getValue().isPresent())
                throw new RuntimeException("Expected a default statement at end of cases");

            cases.get(cases.size() - 1).getStatements().forEach(this::visit);
            return Environment.NIL;
        }
        finally
        {
            scope = scope.getParent();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        
        if (!(ast.getValue().isPresent()))
            throw new RuntimeException("Expected a case, but received nothing");

        return visit(ast.getValue().get());
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        
        while (requireType(Boolean.class, visit(ast.getCondition())))
        {
            try
            {
                scope = new Scope(scope);

                ast.getStatements().forEach(this::visit);
            }
            finally
            {
                scope = scope.getParent();
            }
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) throws Return {

        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {

        if (ast.getLiteral() == null)
            return Environment.NIL;

        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {

        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {

        Environment.PlcObject left = visit(ast.getLeft());
        String operator = ast.getOperator();

        if (operator.equals("&&"))
        {
            Boolean lhs = requireType(Boolean.class, left);
            if (!lhs)
                return Environment.create(false);

            Environment.PlcObject right = visit(ast.getRight());
            Boolean rhs = requireType(Boolean.class, right);

            return Environment.create(rhs);
        }
        else if (operator.equals("||"))
        {
            Boolean lhs = requireType(Boolean.class, left);
            if (lhs)
                return Environment.create(true);

            Environment.PlcObject right = visit(ast.getRight());
            Boolean rhs = requireType(Boolean.class, right);

            return Environment.create(rhs);
        }



        Environment.PlcObject right = visit(ast.getRight());

        if (operator.equals("<"))
        {
            Comparable lhs = requireType(Comparable.class, left);
            Comparable rhs = (Comparable) requireType(left.getValue().getClass(), right);

            return Environment.create(lhs.compareTo(rhs) < 0);
        }
        else if (operator.equals(">"))
        {
            Comparable lhs = requireType(Comparable.class, left);
            Comparable rhs = (Comparable) requireType(left.getValue().getClass(), right);

            return Environment.create(lhs.compareTo(rhs) > 0);
        }

        if (operator.equals("=="))
            return Environment.create(Objects.equals(left.getValue(), right.getValue()));
        else if (operator.equals("!="))
            return Environment.create(!Objects.equals(left.getValue(), right.getValue()));

        switch (operator)
        {
            case "+":
                if ((left.getValue() instanceof String) || (right.getValue() instanceof String))
                {
                    String result;
                    if (left.getValue() instanceof String)
                    {
                        String lhs = (String) left.getValue();
                        result = lhs + right.getValue();
                    }
                    else
                    {
                        String rhs = (String) right.getValue();
                        result = left.getValue() + rhs;
                    }

                    return Environment.create(result);
                }
                else if (left.getValue() instanceof BigInteger)
                {
                    BigInteger lhs = requireType(BigInteger.class, left);
                    BigInteger rhs = requireType(BigInteger.class, right);

                    return Environment.create(lhs.add(rhs));
                }
                else if (left.getValue() instanceof BigDecimal)
                {
                    BigDecimal lhs = requireType(BigDecimal.class, left);
                    BigDecimal rhs = requireType(BigDecimal.class, right);

                    return Environment.create(lhs.add(rhs));
                }
                break;

            case "-":
                if (left.getValue() instanceof BigInteger)
                {
                    BigInteger lhs = requireType(BigInteger.class, left);
                    BigInteger rhs = requireType(BigInteger.class, right);

                    return Environment.create(lhs.subtract(rhs));
                }
                else if (left.getValue() instanceof BigDecimal)
                {
                    BigDecimal lhs = requireType(BigDecimal.class, left);
                    BigDecimal rhs = requireType(BigDecimal.class, right);

                    return Environment.create(lhs.subtract(rhs));
                }
                break;

            case "*":
                if (left.getValue() instanceof BigInteger)
                {
                    BigInteger lhs = requireType(BigInteger.class, left);
                    BigInteger rhs = requireType(BigInteger.class, right);

                    return Environment.create(lhs.multiply(rhs));
                }
                else if (left.getValue() instanceof BigDecimal)
                {
                    BigDecimal lhs = requireType(BigDecimal.class, left);
                    BigDecimal rhs = requireType(BigDecimal.class, right);

                    return Environment.create(lhs.multiply(rhs));
                }
                break;

            case "/":
                if (left.getValue() instanceof BigInteger)
                {
                    BigInteger lhs = requireType(BigInteger.class, left);
                    BigInteger rhs = requireType(BigInteger.class, right);

                    if (rhs.intValue() == 0)
                        throw new RuntimeException();

                    return Environment.create(lhs.divide(rhs));
                }
                else if (left.getValue() instanceof BigDecimal)
                {
                    BigDecimal lhs = requireType(BigDecimal.class, left);
                    BigDecimal rhs = requireType(BigDecimal.class, right);

                    if (rhs.doubleValue() == 0.0)
                        throw new RuntimeException();

                    return Environment.create(lhs.divide(rhs, RoundingMode.HALF_EVEN));
                }
                break;

            case "^":
                requireType(BigInteger.class, left);
                requireType(BigInteger.class, right);

                BigInteger leftVal = (BigInteger) left.getValue();
                BigInteger rightVal = (BigInteger) right.getValue();
                BigInteger result = BigInteger.ONE;

                for (BigInteger i = BigInteger.ZERO; i.compareTo(rightVal) < 0; i = i.add(BigInteger.ONE))
                    result = result.multiply(leftVal);

                return Environment.create(result);
        }

        throw new RuntimeException("Invalid Binary Operation");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        Optional<Ast.Expression> offset = ast.getOffset();

        if (offset.isPresent())
        {
            BigInteger offsetVal = requireType(BigInteger.class, visit(offset.get()));
            List<Ast.Expression> list = ((List<Ast.Expression>) scope.lookupVariable(ast.getName()).getValue().getValue());

            if ((offsetVal.intValue() < 0) || (offsetVal.intValue() >= list.size()))
                throw new RuntimeException("Exception in Access, index out of bounds");

            return Environment.create(list.get(offsetVal.intValue()));
        }
        else
        {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expression expr : ast.getArguments())
            args.add(visit(expr));

        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {

        ArrayList<Object> result = new ArrayList<>();
        for (Ast.Expression val : ast.getValues())
            result.add(visit(val).getValue());

        return Environment.create(result);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }
    }
}
