package ransomaware.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class StoredFile {
    //FIXME check with johnny; Signature
    private final String owner;
    private final String name;

    private String data = null;

    private Map<String, String> keys = null;
    private String iv = null;
    private String author = null;

    public StoredFile(String owner, String name, String data, JsonObject info) {
        this.owner = owner;
        this.name = name;
        this.data = data;

        this.keys = new HashMap<>();
        info.getAsJsonObject("keys").entrySet().forEach(e -> this.keys.put(e.getKey(), e.getValue().getAsString()));
        this.iv = info.get("iv").getAsString();
        this.author = info.get("author").getAsString();
    }

    public StoredFile(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public StoredFile(StoredFile file, String data) {
        this.owner = file.getOwner();
        this.name = file.getName();

        JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
        JsonObject info = obj.getAsJsonObject("info");
        this.data = obj.get("data").getAsString();

        JsonObject keysJson = info.getAsJsonObject("keys");
        this.keys = new HashMap<>();
        keysJson.entrySet().forEach(e -> this.keys.put(e.getKey(), e.getValue().getAsString()));
        this.iv = info.get("iv").getAsString();
        this.author = info.get("author").getAsString();
    }

    public String getFileName() {
        return owner + "/" + name;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public JsonObject getAsJsonObject() {
        JsonObject root = JsonParser.parseString("{}").getAsJsonObject();
        root.addProperty("data", data);

        JsonObject info = JsonParser.parseString("{}").getAsJsonObject();
        JsonObject keys = JsonParser.parseString("{}").getAsJsonObject();
        this.keys.forEach(keys::addProperty);
        info.add("keys", keys);
        info.addProperty("iv", iv);
        info.addProperty("author", author);

        root.add("info", info);

        return root;
    }

    @Override
    public String toString() {
        return getAsJsonObject().toString();
    }
}
