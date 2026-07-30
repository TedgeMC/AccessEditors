package pl.olafcio.accesseditors.util;

import pl.olafcio.accesseditors.error.SyntaxException;

import java.util.List;

public enum StringListUtil {
    ;

    public static void expect(List<String> list, String value) {
        var chars = value.toCharArray();
        var line = list.getFirst().toCharArray();

        int index = 0;
        int lineindex = 0;

        for (char ch : chars) {
            if (ch == '\n')
            {
                if (line.length > index)
                    throw new SyntaxException("Expected line ending");

                list.removeFirst();

                line = list.getFirst().toCharArray();
                index = 0;

                lineindex++;

                continue;
            }
            else if (line[index] != ch)
            {
                throw new SyntaxException("Expected '%s', got '%c' (instead of '%c') at index %d:%d".formatted(value, line[index], ch, lineindex, index));
            }

            index++;
        }

        list.set(0, new String(line, index, line.length - index));
    }

    public static void expect(List<String> list, char ch) {
        var line = list.getFirst().toCharArray();

        int index = 0;
        int lineindex = 0;

        if (ch == '\n')
        {
            if (line.length > index)
                throw new SyntaxException("Expected line ending");

            list.removeFirst();
        }
        else if (line[index] != ch)
        {
            throw new SyntaxException("Expected '%c', got '%c' at index %d:%d".formatted(ch, line[index], lineindex, index));
        }
        else
        {
            list.set(0, new String(line, index, line.length - index));
        }
    }

    public static void skip(List<String> list, int amount) {
        var line = list.getFirst().toCharArray();

        int index = 0;

        for (int i = 0; i < amount; i++) {
            if (line.length <= index)
            {
                list.removeFirst();

                line = list.getFirst().toCharArray();
                index = 0;
            }
            else
            {
                index++;
            }
        }

        list.set(0, new String(line, index, line.length - index));
    }

    public static String until(List<String> list, char end) {
        var out = new StringBuilder();
        var line = list.getFirst().toCharArray();

        int index = 0;

        loop:
        {
            while (line.length > index) {
                var ch = line[index];
                if (ch == end) {
                    break loop;
                } else {
                    index++;
                    out.append(ch);
                }
            }

            if (end != '\n')
                throw new SyntaxException("Expected '%c', got end of line instead".formatted(end));
        }

        list.set(0, new String(line, index, line.length - index));

        return out.toString();
    }
}
