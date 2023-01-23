package plc.project;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    private void tab() {
        indent++;
    }

    private void untab() {
        indent--;
    }

    private void writeFromIndentln(String s) {
        newline(indent);
        print(s);
    }

    @Override
    public Void visit(Ast.Source ast) {

        print("public class Main {");
        tab();

        if (!ast.getGlobals().isEmpty())
        {
            newline(0);
            for (Ast.Global global : ast.getGlobals())
            {
                newline(indent);
                print(global);
            }
        }

        newline(0);

        writeFromIndentln("public static void main(String[] args) {");
        tab();
        writeFromIndentln("System.exit(new Main().main());");
        untab();
        writeFromIndentln("}");
        newline(0);

        for (Ast.Function function : ast.getFunctions())
        {
            newline(indent);
            print(function);
            newline(0);
        }

        untab();
        writeFromIndentln("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {

        if (!ast.getMutable())
            print("final ");

        print(ast.getVariable().getType().getJvmName());
        if (ast.getValue().isPresent())
            if (ast.getValue().get() instanceof Ast.Expression.PlcList)
                print("[]");

        print(" " + ast.getName());

        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        print(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");

        if (!ast.getParameters().isEmpty())
        {
            List<String> parameters = ast.getParameters();
            List<Environment.Type> types = ast.getFunction().getParameterTypes();

            print(types.get(0).getJvmName() + " " + parameters.get(0));

            for (int i = 1; i < parameters.size(); i++)
                print(", " + types.get(i).getJvmName() + " " + parameters.get(i));
        }

        print(") {");

        if (!ast.getStatements().isEmpty())
        {
            tab();
            for (Ast.Statement statement : ast.getStatements())
            {
                newline(indent);
                print(statement);
            }

            untab();
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        print(ast.getExpression(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        print(ast.getVariable().getType().getJvmName(), " ", ast.getName());

        if (ast.getValue().isPresent())
            print(" = ", ast.getValue().get());

        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        print(ast.getReceiver(), " = ", ast.getValue(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        print("if (", ast.getCondition(), ") {");

        if (!ast.getThenStatements().isEmpty())
        {
            tab();
            for (Ast.Statement statement : ast.getThenStatements())
            {
                newline(indent);
                print(statement);
            }

            untab();
            newline(indent);
        }

        if (!ast.getElseStatements().isEmpty())
        {
            print("} else {");

            tab();
            for (Ast.Statement statement : ast.getElseStatements())
            {
                newline(indent);
                print(statement);
            }

            untab();
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {

        print("switch (", ast.getCondition(), ") {");

        if (!ast.getCases().isEmpty())
        {
            tab();
            for (Ast.Statement.Case cases : ast.getCases())
            {
                newline(indent);
                print(cases);
            }

            untab();
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {

        if (ast.getValue().isPresent())
        {
            print("case ", ast.getValue().get(), ":");

            tab();
            for (Ast.Statement statement : ast.getStatements())
            {
                newline(indent);
                print(statement);
            }

            writeFromIndentln("break;");
            untab();
        }
        else
        {
            print("default:");

            tab();
            for (Ast.Statement statement : ast.getStatements())
            {
                newline(indent);
                print(statement);
            }

            untab();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty())
        {
            tab();
            for (Ast.Statement statement : ast.getStatements())
            {
                newline(indent);
                print(statement);
            }

            untab();
            newline(indent);
        }

        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {

        print("return ", ast.getValue(), ";");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        if (ast.getLiteral().equals(Environment.NIL) || ast.getType().equals(Environment.Type.NIL))
            print("Void");
        else if (ast.getLiteral() instanceof String)
            print("\"" + ast.getLiteral() + "\"");
        else if (ast.getLiteral() instanceof Character)
            print("'" + ast.getLiteral() + "'");
        else
            print(ast.getLiteral().toString());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        print("(", ast.getExpression(), ")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        if (!ast.getOperator().equals("^"))
            print(ast.getLeft(), " " + ast.getOperator() + " ", ast.getRight());
        else
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        print(ast.getVariable().getJvmName());
        if (ast.getOffset().isPresent())
            print("[", ast.getOffset().get(), "]");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getFunction().getJvmName() + "(");
        if (!ast.getArguments().isEmpty())
        {
            print(ast.getArguments().get(0));

            for (int i = 1; i < ast.getArguments().size(); i++)
                print(", ", ast.getArguments().get(i));
        }

        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        print("{");

        if (!ast.getValues().isEmpty())
        {
            print(ast.getValues().get(0));
            for (int i = 1; i < ast.getValues().size(); i++)
                print(", ", ast.getValues().get(i));
        }

        print("}");

        return null;
    }
}
