import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.olafcio.accesseditors.AccessEditorLoader;

@Test
void main() {
    var loader = new AccessEditorLoader();

    try (var stream = getClass().getResourceAsStream("accesseditor.txt")) {
        Assertions.assertNotNull(stream);
        loader.add(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
