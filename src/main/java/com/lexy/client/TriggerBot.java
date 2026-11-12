package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** Guarded single-target melee trigger. Does not spoof reach, aim, or attack packets. */
public final class TriggerBot {
    private TriggerBot() {}
    public enum Targets { PLAYERS, MOBS, BOTH }
    public static Targets targets = Targets.PLAYERS;
    public static int clicksPerSecond = 5, cooldownPercent = 93, reachTenths = 29;
    public static boolean requireWeapon = true, requireSneak = false, ignoreTeam = true;
    public static boolean requireChargedHit = true, requireHold = false, avoidManualClicks = true;
    private static long nextAttackMs;
    private static int attacks;
    private static Entity lastTarget;
    public static String status() {return LexyClient.enabled.getOrDefault("TriggerBot",false)?"ON • "+attacks+" hits":"OFF";}
    public static void reset() {nextAttackMs=0;lastTarget=null;}
    public static void tick(MinecraftClient mc) {
        if (!LexyClient.enabled.getOrDefault("TriggerBot",false) || mc==null || mc.player==null || mc.world==null
                || mc.interactionManager==null || mc.currentScreen!=null || !mc.player.isAlive() || mc.player.isSpectator()) return;
        if (mc.player.isUsingItem() || mc.options.useKey.isPressed() || (avoidManualClicks && mc.options.attackKey.isPressed())) return;
        if (requireHold && !LexyKeybinds.triggerHold.isPressed()) return;
        if (requireSneak && !mc.options.sneakKey.isPressed()) return;
        if (requireWeapon && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                && !(mc.player.getMainHandStack().getItem() instanceof AxeItem)) return;
        if (!(mc.crosshairTarget instanceof EntityHitResult ray)) {lastTarget=null;return;}
        Entity target=ray.getEntity();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target==mc.player || !target.isAttackable()) return;
        boolean player = target instanceof PlayerEntity;
        if ((targets==Targets.PLAYERS && !player) || (targets==Targets.MOBS && player)) return;
        if (player && ignoreTeam && mc.player.isTeammate(target)) return;
        double maxReach=Math.min(3.0,Math.max(2.0,reachTenths/10.0));
        if (mc.player.getEyePos().squaredDistanceTo(ray.getPos()) > maxReach*maxReach) return;
        if (!mc.player.canSee(target)) return;
        if (requireChargedHit && mc.player.getAttackCooldownProgress(0.5f) < cooldownPercent/100f) return;
        long now=System.nanoTime()/1_000_000L;
        if (now<nextAttackMs) return;
        long interval=1000L/Math.max(1,Math.min(12,clicksPerSecond));
        if (lastTarget!=target) nextAttackMs=Math.min(nextAttackMs,now);
        nextAttackMs=now+Math.max(85,interval);
        lastTarget=target;
        mc.interactionManager.attackEntity(mc.player,target);
        mc.player.swingHand(Hand.MAIN_HAND);
        attacks++;
    }
}