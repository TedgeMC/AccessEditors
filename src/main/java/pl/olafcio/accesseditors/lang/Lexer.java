package pl.olafcio.accesseditors.lang;

import pl.olafcio.accesseditors.error.SyntaxException;

import java.util.ArrayList;

public final class Lexer {
    private final int[] input;
    public int index;

    public Lexer(String input) {
        this.input = input.replace("\r", "\n").codePoints().toArray();
        this.index = 0;
    }

    public int consume() {
        return input[index++];
    }

    public int peek(int after) {
        return input[index + after];
    }

    public boolean reachedEOF() {
        return index >= input.length;
    }

    public boolean reachedEOF(int after) {
        return index + after >= input.length;
    }

    public String until(int end) {
        var value = new ArrayList<Integer>();

        safe:
        {
            while (!reachedEOF()) {
                var ch = consume();
                if (ch == end)
                    break safe;
                else if (ch == '\n')
                    throw new SyntaxException("Expected '%c'; got end of line".formatted(end));

                value.add(ch);
            }

            throw new SyntaxException("Expected '%c'".formatted(end));
        }

        return new String(value.stream().mapToInt(v -> v).toArray(), 0, value.size());
    }

    public void expect(String value) {
        char[] val = value.toCharArray();

        for (int i = 0; i < val.length; i++)
            if (reachedEOF(i))
                throw new SyntaxException("Expected '%s'; got EOF".formatted(value));
            else if (peek(i) != val[i])
                throw new SyntaxException("Expected '%s'; got '%c' at offset %d".formatted(value, peek(i), i));

        index += val.length;
    }

    public void expect(int ch) {
        if (reachedEOF(ch))
            throw new SyntaxException("Expected '%c'; got EOF".formatted(ch));
        else if (peek(0) != ch)
            throw new SyntaxException("Expected '%c'; got '%c'".formatted(ch, peek(0)));

        index++;
    }

    public boolean now(String value) {
        char[] val = value.toCharArray();

        for (int i = 0; i < val.length; i++)
            if (reachedEOF(i) || peek(i) != val[i])
                return false;

        index += val.length;

        return true;
    }

    public boolean now(int ch) {
        if (peek(0) == ch) {
            index++;
            return true;
        } else {
            return false;
        }
    }

    public boolean future(String value) {
        char[] val = value.toCharArray();

        for (int i = 0; i < val.length; i++)
            if (reachedEOF(i) || peek(i) != val[i])
                return false;

        return true;
    }

    public boolean future(int ch) {
        return !reachedEOF(ch) && peek(0) == ch;
    }
}
