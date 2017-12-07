/*
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.jamiete.xpbottler;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;

public class XPBottler extends JavaPlugin implements Listener {
    private final int PER_BOTTLE = 10;

    int count(final Player player, final Material type) {
        int count = 0;

        for (final ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && type == stack.getType()) {
                count += stack.getAmount();
            }
        }

        return count;
    }

    int getXpFromPlayer(final int level, final float progress) {
        double tot_mod1 = 1;
        double tot_mod2 = 6;
        float tot_add = 0F;
        float req_mod = 2F;
        int req_add = 7;

        if (level > 16 && level <= 31) {
            tot_mod1 = 2.5;
            tot_mod2 = -40.5;
            tot_add = 360F;
            req_mod = 5F;
            req_add = -38;
        } else if (level >= 32) {
            tot_mod1 = 4.5;
            tot_mod2 = -162.5;
            tot_add = 2220F;
            req_mod = 9F;
            req_add = -158;
        }

        return (int) Math.floor(tot_mod1 * level * level + tot_mod2 * level + tot_add + progress * (req_mod * level + req_add));
    }

    @EventHandler
    public void onEmpty(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (event.getAction() != Action.LEFT_CLICK_BLOCK || block == null || block.getType() != Material.ENCHANTMENT_TABLE) { // Block check
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() != Material.GLASS_BOTTLE) { // Hand check
            return;
        }

        final int totalXp = this.getXpFromPlayer(event.getPlayer().getLevel(), event.getPlayer().getExp());
        final int available = totalXp / this.PER_BOTTLE;
        final int bottles = this.count(player, Material.GLASS_BOTTLE);
        final int change = Math.min(available, bottles);

        if (change == 0) {
            return;
        }

        // Remove from inventory
        player.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, change));

        // Drop new items
        int remaining = change;

        while (remaining > 0) {
            int amount = remaining;

            if (amount > 64) {
                amount = 64;
            }

            final ItemStack xp = new ItemStack(Material.EXP_BOTTLE, amount);
            block.getWorld().dropItemNaturally(block.getLocation().clone().add(0, 1.3, 0), xp);

            remaining -= amount;
        }

        player.setLevel(0);
        player.setExp(0);
        player.giveExp(totalXp - change * this.PER_BOTTLE);

        player.sendMessage(ChatColor.GREEN + "You just bottled your experience into " + change + " " + (change == 1 ? "bottle" : "bottles") + ".");
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onFull(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (event.getAction() != Action.LEFT_CLICK_BLOCK || block == null || block.getType() != Material.ENCHANTMENT_TABLE) { // Block check
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() != Material.EXP_BOTTLE) { // Hand check
            return;
        }

        final int amount = this.count(player, Material.EXP_BOTTLE);
        final int bottles = amount * this.PER_BOTTLE;

        // Remove from inventory
        player.getInventory().removeItem(new ItemStack(Material.EXP_BOTTLE, amount));

        // Drop new items
        int remaining = amount;

        while (remaining > 0) {
            int drop = remaining;

            if (drop > 64) {
                drop = 64;
            }

            final ItemStack bottle = new ItemStack(Material.GLASS_BOTTLE, drop);
            block.getWorld().dropItemNaturally(block.getLocation().clone().add(0, 1.3, 0), bottle);

            remaining -= drop;
        }

        // Modify player
        player.giveExp(bottles);
        player.sendMessage(ChatColor.GREEN + "You just emptied " + amount + " " + (amount == 1 ? "bottle" : "bottles") + " of experience.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onThrow(final ExpBottleEvent event) {
        event.setExperience(this.PER_BOTTLE);
    }

}
