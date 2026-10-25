package com.example.tropicalfishbreed;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TropicalFishBreed extends JavaPlugin implements Listener {
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long BREED_COOLDOWN_MS = 5 * 60 * 1000L;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("TropicalFishBreed enabled!");
    }

    @EventHandler
    public void onFeedFish(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();

        if (!(target instanceof TropicalFish fish)) return;

        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item.getType() != Material.KELP) return;

        event.setCancelled(true);

        TropicalFish partner = fish.getWorld()
                .getNearbyEntities(fish.getLocation(), 4, 3, 4)
                .stream()
                .filter(e -> e instanceof TropicalFish)
                .map(e -> (TropicalFish) e)
                .filter(f -> !f.getUniqueId().equals(fish.getUniqueId()))
                .filter(f -> !isOnCooldown(f) && !isOnCooldown(fish))
                .findFirst()
                .orElse(null);

        if (partner == null) {
            player.sendMessage("§eРядом нет готовой второй тропической рыбки!");
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(event.getHand(), null);
        }

        long until = System.currentTimeMillis() + BREED_COOLDOWN_MS;
        cooldowns.put(fish.getUniqueId(), until);
        cooldowns.put(partner.getUniqueId(), until);

        fish.getWorld().spawnParticle(
                Particle.HEART, fish.getLocation().add(0, 0.5, 0),
                7, 0.35, 0.35, 0.35, 0
        );
        partner.getWorld().spawnParticle(
                Particle.HEART, partner.getLocation().add(0, 0.5, 0),
                7, 0.35, 0.35, 0.35, 0
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!fish.isValid() || !partner.isValid()) return;

                TropicalFish baby = fish.getWorld()
                        .spawn(fish.getLocation(), TropicalFish.class);

                baby.setPattern(fish.getPattern());
                baby.setBodyColor(fish.getBodyColor());
                baby.setPatternColor(fish.getPatternColor());

                baby.setCustomName("§eBaby Tropical Fish");
                baby.setCustomNameVisible(false);

                baby.getWorld().spawnParticle(
                        Particle.HEART, baby.getLocation().add(0, 0.5, 0),
                        10, 0.4, 0.4, 0.4, 0
                );

                player.sendMessage("§aПоявилась новая тропическая рыбка!");
            }
        }.runTaskLater(this, 40L);
    }

    private boolean isOnCooldown(TropicalFish fish) {
        Long until = cooldowns.get(fish.getUniqueId());

        if (until == null) return false;

        if (System.currentTimeMillis() >= until) {
            cooldowns.remove(fish.getUniqueId());
            return false;
        }

        return true;
    }
}
