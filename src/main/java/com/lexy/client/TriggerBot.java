package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Optional crosshair-triggered melee actions. Uses the vanilla interaction manager,
 * obeys normal weapon recharge, line-of-sight and a conservative interaction range.
 * Runs only on the Minecraft client tick; no spoofed packets or targeting through walls.
 */
public final class TriggerBot {
    private TriggerBot() {}
    public enum Targets { PLAYERS, MOBS, BOTH }
    public static Targets targets = Targets.PLAYERS;
    public static int clicksPerSecond = 5;
    public static int cooldownPercent = 95;
    public static int reachTenths = 29;
    public static boolean requireWeapon = true;
    public static boolean requireSneak = false;
    public static boolean ignoreTeam = true;
    public static boolean requireChargedHit = true;
    private static long nextAttackMs = 0;
    private static int attacks = 0;

    public static String status() {
        return LexyClient.enabled.getOrDefault("TriggerBot", false) ? "Enabled  |  hits: " + attacks : "Disabled";
    }
    public static void reset() { nextAttackMs = 0; }

    public static void tick(MinecraftClient mc) {
        if (!LexyClient.enabled.getOrDefault("TriggerBot", false) || mc == null || mc.player == null
                || mc.world == null || mc.interactionManager == null || mc.currentScreen != null
                || mc.player.isSpectator() || !mc.player.isAlive()) {
            return;
        }
        if (mc.player.isUsingItem()) return;
        if (requireSneak && !mc.options.sneakKey.isPressed()) return;
        if (requireWeapon && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                && !(mc.player.getMainHandStack().getItem() instanceof AxeItem)) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ray)) return;
        Entity target = ray.getEntity();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == mc.player) return;
        boolean isPlayer = target instanceof PlayerEntity;
        if (targets == Targets.PLAYERS && !isPlayer) return;
        if (targets == Targets.MOBS && isPlayer) return;
        if (isPlayer && ignoreTeam && mc.player.isTeammate(target)) return;
        if (mc.player.squaredDistanceTo(target) > Math.pow(Math.min(3.0, reachTenths / 10.0), 2)) return;
        if (requireChargedHit && mc.player.getAttackCooldownProgress(0.0f) < cooldownPercent / 100.0f) return;
        long now = System.currentTimeMillis();
        if (now < nextAttackMs) return;
        nextAttackMs = now + Math.max(85, 1000 / Math.max(1, Math.min(12, clicksPerSecond)));
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        attacks++;
    }
}