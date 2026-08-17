package com.oxipro.idcraft.api.configuration;

public enum ConfigsType {

    CONFIG("config.yml"),
    CSSDB("cssdb.yml"),
    LANG_EN("languages/en_US.yml"),
    LANG_FR("languages/fr_FR.yml");

    public final String filePath;

    ConfigsType(String filePath) {
        this.filePath = filePath;
    }
}
