package com.lootlookup;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;

import com.lootlookup.utils.Constants;
import com.lootlookup.utils.Icons;
import com.lootlookup.views.LootLookupPanel;
import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.apache.commons.lang3.ArrayUtils;

@PluginDescriptor(
        name = Constants.PLUGIN_NAME
)
public class LootLookupPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private LootLookupConfig config;

    private LootLookupPanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception {
        panel = new LootLookupPanel(config);

        navButton =
                NavigationButton.builder()
                        .tooltip(Constants.PLUGIN_NAME)
                        .icon(Icons.NAV_BUTTON)
                        .priority(Constants.DEFAULT_PRIORITY)
                        .panel(panel)
                        .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals(Constants.PLUGIN_NAME)) {
            switch (event.getKey()) {
                case "showRarity":
                case "showQuantity":
                case "showPrice":
                case "disableItemsLinks":
                case "rareColor":
                case "superRareColor":
                case "priceColor":
                    if (panel != null) {
                        panel.refreshMainPanel();
                    }
            }
        }
    }

    /**
     * Insert option adjacent to "Examine" when target is attackable NPC
     *
     * @param event
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        final NPC[] cachedNPCs = client.getCachedNPCs();
        MenuEntry[] menuEntries = event.getMenuEntries();

        boolean isTargetAttackableNPC = false;
        MenuEntry entryToAppendOn = null;
        String targetMonsterName = "";

        for (MenuEntry menuEntry : menuEntries) {
            String optionText = menuEntry.getOption();

            int id = menuEntry.getIdentifier();

            if (id < cachedNPCs.length) {
                NPC target = cachedNPCs[id];

                if (target != null) {
                    int combatLevel = target.getCombatLevel();
                    if (optionText.equals("Attack") && combatLevel > 0) {
                        isTargetAttackableNPC = true;
                        targetMonsterName = target.getName();
                    } else if (optionText.equals("Examine")) {
                        entryToAppendOn = menuEntry;
                    }
                }
            }
        }

        if (isTargetAttackableNPC && entryToAppendOn != null && !config.disableMenuOption()) {

            int idx = ArrayUtils.indexOf(menuEntries, entryToAppendOn);

            String finalTargetMonsterName = targetMonsterName;
            client
                    .createMenuEntry(idx + 1)
                    .setOption("Lookup Drops")
                    .setTarget(entryToAppendOn.getTarget())
                    .setIdentifier(entryToAppendOn.getIdentifier())
                    .setParam1(entryToAppendOn.getParam1())
                    .setType(MenuAction.of(MenuAction.RUNELITE.getId()))
                    .onClick(
                            evt -> {
                                selectNavButton();
                                panel.lookupMonsterDrops(finalTargetMonsterName);
                            });
        }
    }


    @Provides
    LootLookupConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LootLookupConfig.class);
    }

    public void selectNavButton() {
        SwingUtilities.invokeLater(
                () -> {
                    if (!navButton.isSelected()) {
                        navButton.getOnSelect().run();
                    }
                });
    }
}