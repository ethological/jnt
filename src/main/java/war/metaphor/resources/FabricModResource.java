package war.metaphor.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

public class FabricModResource implements IResource {

    @Override
    public String getName() {
        return "fabric.mod.json";
    }

    @Override
    public String handle(String contents, Map<String, String> mapping) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject obj = gson.fromJson(contents, JsonObject.class);
        JsonObject entrypoints = obj.getAsJsonObject("entrypoints");

        for (String s : entrypoints.keySet()) {
            JsonArray values = entrypoints.getAsJsonArray(s);
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i).getAsString();
                if (value != null && !value.isEmpty()) {
                    String normalized = value.replace('.', '/');
                    String mappedValue = mapping.getOrDefault(normalized, value);
                    values.set(i, gson.toJsonTree(mappedValue.replace("/", ".")));
                }
            }
        }

        contents = gson.toJson(obj);

        return contents;

    }
}
