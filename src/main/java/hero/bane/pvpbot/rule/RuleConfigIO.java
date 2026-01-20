package hero.bane.pvpbot.rule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

public final class RuleConfigIO {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static void load(File file) {
        if (!file.exists()) {
            save(file);
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;

            for (Map.Entry<String, RuleEntry> e : RuleRegistry.all().entrySet()) {
                JsonElement el = json.get(e.getKey());
                if (el == null) continue;
                Object value = GSON.fromJson(el, e.getValue().type);
                e.getValue().set(value);
            }
        } catch (Exception ignored) {}
    }

    public static void save(File file) {
        JsonObject json = new JsonObject();

        for (RuleEntry rule : RuleRegistry.all().values()) {
            json.add(rule.name, GSON.toJsonTree(rule.get()));
        }

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(json, writer);
        } catch (Exception ignored) {}
    }
}
