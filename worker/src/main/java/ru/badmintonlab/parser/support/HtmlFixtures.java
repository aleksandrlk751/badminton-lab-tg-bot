package ru.badmintonlab.parser.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HtmlFixtures {

    private HtmlFixtures() {
    }

    public static Document load(String resourceName) {
        String path = "html/" + resourceName;
        try (InputStream in = HtmlFixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return Jsoup.parse(in, StandardCharsets.UTF_8.name(), "https://badminton4u.ru/");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + path, e);
        }
    }

    public static Document loadPath(Path file) {
        try {
            return Jsoup.parse(file.toFile(), StandardCharsets.UTF_8.name(), "https://badminton4u.ru/");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load fixture: " + file, e);
        }
    }
}
