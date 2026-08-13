package com.oxipro.idcraft.api.configuration;


import com.oxipro.cmu.configlang.api.config.IConfigFile;

public interface IConfigManager {

    IConfigFile getMain();

    IConfigFile getCssdb();

    void reloadAll();
}
