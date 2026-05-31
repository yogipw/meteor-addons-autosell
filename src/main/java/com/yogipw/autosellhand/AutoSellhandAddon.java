package com.yogipw.autosellhand;

import com.mojang.logging.LogUtils;
import com.yogipw.autosellhand.modules.AutoSellhand;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AutoSellhandAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Mining Utils");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Mining Utils addon");
        Modules.get().add(new AutoSellhand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.yogipw.autosellhand";
    }
}
