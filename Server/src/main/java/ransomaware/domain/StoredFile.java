package ransomaware.domain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class StoredFile {
    //FIXME check with johnny; Signature
    private final String owner;
    private final String name;
    private String data = null;
    private String signature = null;
    private String key = null;
    private String iv = null;

    public StoredFile(String owner, String name, String data, String signature, String key, String iv) {
        this.owner = owner;
        this.name = name;
        this.data = data;
        this.signature = signature;
        this.key = key;
        this.iv = iv;
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
        this.signature = obj.get("signature").getAsString();
        this.key = info.get("key").getAsString();
        this.iv = info.get("iv").getAsString();
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

    public String getKey() {
        return key;
    }

    public String getIV() {
        return iv;
    }

    public String getData() {
        return data;
    }

    public JsonObject getAsJsonObject() {
        JsonObject root = JsonParser.parseString("{}").getAsJsonObject();
        root.addProperty("data", data);

        JsonObject info = JsonParser.parseString("{}").getAsJsonObject();
        info.addProperty("key", key);
        info.addProperty("iv", iv);
        info.addProperty("signature", signature);
        root.add("info", info);

        return root;
    }

    @Override
    public String toString() {
        return getAsJsonObject().toString();
    }
}
