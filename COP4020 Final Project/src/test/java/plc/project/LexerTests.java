package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading @", "@someIdentifier123_-", true),

                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading Underscore", "_badIdentifier", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Single Zero", "0", true),

                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Single Hyphen", "-", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Valid Leading Zero", "0.0001", true),
                Arguments.of("Negative Decimal with Leading Zero", "-0.3", true),

                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Zero Trailing Decimal", "0.", false),
                Arguments.of("Zero Leading Decimal", ".0", false),
                Arguments.of("Negative Trailing Decimal", "-1.", false),
                Arguments.of("Negative Leading Decimal", "-.5", false),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Negative Zero Trailing Decimal", "-0.", false),
                Arguments.of("Negative Alphabetical", "-A", false),
                Arguments.of("Invalid Leading Zero", "000.01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "'c'", true),
                Arguments.of("Backspace Escape", "'\\b'", true),
                Arguments.of("Newline Escape", "'\\n'", true),
                Arguments.of("Return Escape", "'\\r'", true),
                Arguments.of("Tab Escape", "'\\t'", true),
                Arguments.of("Single Quote Escape", "'\\''", true),
                Arguments.of("Double Quote Escape", "'\\\"'", true),
                Arguments.of("Backslash Escape", "'\\\\'", true),

                Arguments.of("Return Standalone", "'\r'", false),
                Arguments.of("Newline Standalone", "'\n'", false),
                Arguments.of("Unterminated", "'", false),
                Arguments.of("Invalid Escape", "'\\a'", false),
                Arguments.of("Empty", "''", false),
                Arguments.of("Multiple", "'abc'", false),
                Arguments.of("Trailing Characters", "'a'b", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Backspace Escape", "\"\\b\"", true),
                Arguments.of("Newline Escape", "\"\\n\"", true),
                Arguments.of("Return Escape", "\"\\r\"", true),
                Arguments.of("Tab Escape", "\"\\t\"", true),
                Arguments.of("Single Quote Escape", "\"\\'\"", true),
                Arguments.of("Double Quote Escape", "\"\\\"\"", true),
                Arguments.of("Backslash Escape", "\"\\\\\"", true),

                Arguments.of("Return Standalone", "\"\r\"", false),
                Arguments.of("Newline Standalone", "\"\n\"", false),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Trailing String", "\"some\"string", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Comparison 2", "==", true),
                Arguments.of("Assignment", "=", true),
                Arguments.of("Negation", "!", true),
                Arguments.of("Logical AND", "&&", true),
                Arguments.of("Logical OR", "||", true),

                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Tab Escape", "\\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Some string + char", "\"Hello world\"'c'", Arrays.asList(
                        new Token(Token.Type.STRING, "\"Hello world\"", 0),
                        new Token(Token.Type.CHARACTER, "'c'", 13)
                )),
                Arguments.of("Some string + space + char", "\"Hello world\" 'c'", Arrays.asList(
                        new Token(Token.Type.STRING, "\"Hello world\"", 0),
                        new Token(Token.Type.CHARACTER, "'c'", 14)
                )),
                Arguments.of("Leading space", " \"Hello world\"'c'", Arrays.asList(
                        new Token(Token.Type.STRING, "\"Hello world\"", 1),
                        new Token(Token.Type.CHARACTER, "'c'", 14)
                )),
                Arguments.of("Trailing space", "\"Hello world\"'c' ", Arrays.asList(
                        new Token(Token.Type.STRING, "\"Hello world\"", 0),
                        new Token(Token.Type.CHARACTER, "'c'", 13)
                )),

                Arguments.of("Double Decimal", "1..0", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.OPERATOR, ".", 2),
                        new Token(Token.Type.INTEGER, "0", 3)
                )),
                Arguments.of("Number Method", "1.toString()", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.IDENTIFIER, "toString", 2),
                        new Token(Token.Type.OPERATOR, "(", 10),
                        new Token(Token.Type.OPERATOR, ")", 11)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
