/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery.integration;

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Getter
@Setter
public class Hook {

    protected static final Config config = ConfigManager.getConfig(Config.class);

    public static final Hook LWC = new Hook("LWC", config.isUseLWC());
    public static final Hook GRIEFPREVENTION = new Hook("GriefPrevention", config.isUseGriefPrevention());
    public static final Hook TOWNY = new Hook("Towny", config.isUseTowny());
    public static final Hook BLOCKLOCKER = new Hook("BlockLocker", config.isUseBlockLocker());
    public static final Hook GAMEMODEINVENTORIES = new Hook("GameModeInventories", config.isUseGMInventories());
    public static final Hook MMOITEMS = new Hook("MMOItems");
    public static final Hook VAULT = new Hook("Vault");
    public static final Hook CHESTSHOP = new Hook("ChestShop");
    public static final Hook SHOPKEEPERS = new Hook("ShopKeepers");
    public static final Hook SLIMEFUN = new Hook("Slimefun");
    public static final Hook ORAXEN = new Hook("Oraxen");
    public static final Hook ITEMSADDER = new Hook("ItemsAdder");


    protected final String name;
    protected boolean enabled;
    protected boolean checked;

    public Hook(String name) {
        this.name = name;
        this.enabled = true;
    }

    public Hook(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        if (!checked) {
            checked = true;
            if (enabled) {
                enabled = Bukkit.getPluginManager().isPluginEnabled(name);
            }
        }
        return enabled;
    }

    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin(name);
    }

}
