package io.mewb.fancyElytras.gui;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.data.ElytraData;
import io.mewb.fancyElytras.data.ParticleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ParticleSelectionGUI {

    private final FancyElytras plugin;
    private final Player player;
    private final ItemStack elytra;
    private PaginatedGui gui;

    public ParticleSelectionGUI(FancyElytras plugin, Player player, ItemStack elytra) {
        this.plugin = plugin;
        this.player = player;
        this.elytra = elytra;
        createGUI();
    }

    private void createGUI() {
        // Get title from messages with fallback
        String titleStr;
        try {
            titleStr = plugin.getMessageUtil().getMessage("gui.particle-selection.title");
        } catch (Exception e) {
            // Fallback title if message system fails
            titleStr = "&5⚡ &dParticle Selection &5⚡";
            plugin.getLogger().warning("Failed to get GUI title from messages, using fallback");
        }
        
        // Ensure we have a valid title
        if (titleStr == null || titleStr.isEmpty()) {
            titleStr = "&5⚡ &dParticle Selection &5⚡";
        }
        
        // Convert color codes and then to Component
        titleStr = ChatColor.translateAlternateColorCodes('&', titleStr);
        Component title = LegacyComponentSerializer.legacySection().deserialize(titleStr);

        // Create paginated GUI with 6 rows
        gui = Gui.paginated()
                .title(title)
                .rows(6)
                .pageSize(28)
                .create();

        // Add particle items
        addParticleItems();

        // Add navigation items
        addNavigationItems();

        // Add close button
        addCloseButton();
    }

    private void addParticleItems() {
        List<ParticleType> availableParticles = plugin.getParticleManager().getParticlesForPlayer(player);
        plugin.getLogger().info("GUI: Found " + availableParticles.size() + " particles for player " + player.getName());

        ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
        String currentParticle = elytraData != null && elytraData.getParticleType() != null ?
                elytraData.getParticleType().toLowerCase() : "none";

        // Add "No Particles" option first
        ItemStack noneItem = createNoneParticleItem(currentParticle);
        GuiItem noneGuiItem = new GuiItem(noneItem, event -> {
            event.setCancelled(true);
            selectParticle(null);
        });
        gui.addItem(noneGuiItem);

        // Add all available particles
        for (ParticleType particleType : availableParticles) {
            ItemStack item = createParticleItem(particleType, currentParticle);

            GuiItem guiItem = new GuiItem(item, event -> {
                event.setCancelled(true);
                selectParticle(particleType);
            });

            gui.addItem(guiItem);
        }

        plugin.getLogger().info("GUI: Total items added to GUI: " + (availableParticles.size() + 1));
    }

    private void addNavigationItems() {
        // Previous page button (slot 45)
        ItemStack prevItem = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevItem.getItemMeta();
        if (prevMeta != null) {
            prevMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6◀ Previous Page"));
            List<String> prevLore = new ArrayList<>();
            prevLore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to go to the previous page"));
            prevMeta.setLore(prevLore);
            prevItem.setItemMeta(prevMeta);
        }

        GuiItem prevGuiItem = new GuiItem(prevItem, event -> {
            event.setCancelled(true);
            if (gui.previous()) {
                updatePageInfo();
                gui.update();
            }
        });
        gui.setItem(45, prevGuiItem);

        // Next page button (slot 53)
        ItemStack nextItem = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextItem.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Next Page ▶"));
            List<String> nextLore = new ArrayList<>();
            nextLore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to go to the next page"));
            nextMeta.setLore(nextLore);
            nextItem.setItemMeta(nextMeta);
        }

        GuiItem nextGuiItem = new GuiItem(nextItem, event -> {
            event.setCancelled(true);
            if (gui.next()) {
                updatePageInfo();
                gui.update();
            }
        });
        gui.setItem(53, nextGuiItem);

        // Page info (slot 49)
        updatePageInfo();
    }

    private void updatePageInfo() {
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&ePage Information"));
            List<String> pageLore = new ArrayList<>();
            pageLore.add(ChatColor.translateAlternateColorCodes('&', "&7Current Page: &f" + (gui.getCurrentPageNum() + 1)));
            pageLore.add(ChatColor.translateAlternateColorCodes('&', "&7Total Pages: &f" + gui.getPagesNum()));
            pageMeta.setLore(pageLore);
            pageInfo.setItemMeta(pageMeta);
        }

        GuiItem pageGuiItem = new GuiItem(pageInfo, event -> event.setCancelled(true));
        gui.setItem(49, pageGuiItem);
    }

    private void addCloseButton() {
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cClose"));
            List<String> closeLore = new ArrayList<>();
            closeLore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            closeMeta.setLore(closeLore);
            closeItem.setItemMeta(closeMeta);
        }

        GuiItem closeGuiItem = new GuiItem(closeItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
        });
        gui.setItem(47, closeGuiItem);
    }

    private ItemStack createParticleItem(ParticleType particleType, String currentParticle) {
        Material material;
        try {
            material = Material.valueOf(particleType.getGuiItem().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for particle " + particleType.getName() + ": " + particleType.getGuiItem());
            material = Material.PAPER; // Fallback
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Format display name with color codes
            String displayName = ChatColor.translateAlternateColorCodes('&', particleType.getDisplayName());

            // Check if this particle is currently selected
            boolean isSelected = particleType.getName().equalsIgnoreCase(currentParticle);

            if (isSelected) {
                displayName = ChatColor.translateAlternateColorCodes('&', "&a✓ ") + displayName + ChatColor.translateAlternateColorCodes('&', " &a(Selected)");
                // Add enchantment glow for selected item
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.setDisplayName(displayName);

            // Set lore
            List<String> lore = new ArrayList<>();

            // Add description
            if (particleType.getDescription() != null && !particleType.getDescription().isEmpty()) {
                for (String desc : particleType.getDescription()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', desc));
                }
                lore.add("");
            }

            // Add permission info
            if (plugin.getParticleManager().hasParticlePermission(player, particleType.getName())) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&a✓ Available"));
                lore.add("");
                if (isSelected) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7Currently selected"));
                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to select!"));
                }
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&c✗ No Permission"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Required: &c") + particleType.getPermission());
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createNoneParticleItem(String currentParticle) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            boolean isSelected = "none".equals(currentParticle) || currentParticle == null;

            String displayName = ChatColor.translateAlternateColorCodes('&', "&cNo Particles");

            if (isSelected) {
                displayName = ChatColor.translateAlternateColorCodes('&', "&a✓ &cNo Particles &a(Selected)");
                // Add enchantment glow for selected item
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.setDisplayName(displayName);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Disable all particle effects"));
            lore.add("");
            if (isSelected) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Currently selected"));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to disable particles!"));
            }
            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private void selectParticle(ParticleType particleType) {
        try {
            // Get current elytra data
            ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
            if (elytraData == null) {
                plugin.getMessageUtil().sendMessage(player, "general.error");
                return;
            }

            // Check permission for new particle
            if (particleType != null) {
                if (!plugin.getParticleManager().hasParticlePermission(player, particleType.getName())) {
                    plugin.getMessageUtil().sendMessage(player, "general.no-permission");
                    return;
                }
            }

            // Update particle type
            String newParticleType = particleType != null ? particleType.getName() : "NONE";
            elytraData.setParticleType(newParticleType);

            // Save changes
            plugin.getElytraManager().setElytraData(elytra, elytraData);

            // Also update equipped elytra if it exists and matches
            ItemStack equippedElytra = player.getInventory().getChestplate();
            if (equippedElytra != null && equippedElytra.getType() == Material.ELYTRA) {
                ElytraData equippedData = plugin.getElytraManager().getElytraData(equippedElytra);
                if (equippedData != null) {
                    equippedData.setParticleType(newParticleType);
                    plugin.getElytraManager().setElytraData(equippedElytra, equippedData);
                }
            }

            // Send success message
            if (particleType == null) {
                plugin.getMessageUtil().sendMessage(player, "commands.particle.disabled");
            } else {
                String message = plugin.getMessageUtil().getMessage("commands.particle.selected");
                String displayName = ChatColor.translateAlternateColorCodes('&', particleType.getDisplayName());
                message = message.replace("{particle}", ChatColor.stripColor(displayName));
                plugin.getMessageUtil().sendRawMessage(player, message);
            }

            // Close GUI and play sound
            player.closeInventory();
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        } catch (Exception e) {
            plugin.getLogger().warning("Error selecting particle for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            plugin.getMessageUtil().sendMessage(player, "general.error");
        }
    }

    public void open() {
        gui.open(player);
    }

    public void refresh() {
        gui.clearPageItems();
        addParticleItems();
        updatePageInfo();
        gui.update();
    }
}