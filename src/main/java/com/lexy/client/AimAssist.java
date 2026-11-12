package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Main-thread, visibility-checked target acquisition and bounded smooth camera rotation.
 * No reach, packet, or hitbox manipulation. Never runs in screens or while using an item.
 */
public final class AimAssist {
    private AimAssist() {}
    public enum Targets { PLAYERS, MOBS, BOTH }
    public static Targets targets = Targets.PLAYERS;
    public static int rangeTenths = 38;  // maximum selection distance (aiming, not attack reach)
    public static int fovDegrees = 65;   // total field-of-view cone, degrees
    public static int smoothness = 65;   // percentage of remaining delta damped each tick
    public static int maxTurnTenths = 90;// max yaw/pitch degrees per tick, times ten
    public static int aimHeight = 70;    // percent of target hitbox from bottom
    public static int leadPercent = 0;   // target movement prediction; 0..100
    public static int lockMs = 450;      // retain target during small movement
    public static boolean requireHold = false, requireWeapon = true;
    public static boolean ignoreTeammates = true, ignoreInvisible = true;
    public static boolean disableOnManualLook = false;

    private static LivingEntity locked;
    private static long lockedAt, nextSearch;
    private static int lastId = -1;
    private static int targetsSeen;
    public static String status() {
        if (!LexyClient.enabled.getOrDefault("AimAssist", false)) return "OFF";
        return locked != null && locked.isAlive() ? "Tracking " + locked.getName().getString() : "ON · searching";
    }
    public static void reset() { locked=null; lockedAt=0; nextSearch=0; lastId=-1; }

    private static boolean allowed(MinecraftClient mc, LivingEntity entity, double radiusSq) {
        if (entity == mc.player || !entity.isAlive() || entity.isRemoved() || entity.isSpectator()
                || !entity.isAttackable() || mc.player.squaredDistanceTo(entity) > radiusSq) return false;
        boolean isPlayer = entity instanceof PlayerEntity;
        if (targets==Targets.PLAYERS && !isPlayer || targets==Targets.MOBS && isPlayer) return false;
        if (isPlayer && ignoreTeammates && mc.player.isTeammate(entity)) return false;
        if (ignoreInvisible && entity.isInvisible()) return false;
        return mc.player.canSee(entity);
    }
    private static Vec3d point(LivingEntity target) {
        double y=target.getBoundingBox().minY+target.getHeight() * MathHelper.clamp(aimHeight/100.0, .2, .95);
        double lead=MathHelper.clamp(leadPercent/100.0,0,1) * .8;
        return new Vec3d(target.getX()+target.getVelocity().x*lead,y+target.getVelocity().y*lead,
                target.getZ()+target.getVelocity().z*lead);
    }
    private static float[] angles(Vec3d eye, Vec3d point) {
        Vec3d d=point.subtract(eye);
        double flat=Math.sqrt(d.x*d.x+d.z*d.z);
        return new float[]{(float)(Math.toDegrees(Math.atan2(d.z,d.x))-90),
                (float)-Math.toDegrees(Math.atan2(d.y,flat))};
    }
    private static float angleDistance(MinecraftClient mc, LivingEntity e) {
        float[] desired=angles(mc.player.getEyePos(),point(e));
        float yaw=MathHelper.wrapDegrees(desired[0]-mc.player.getYaw());
        float pitch=MathHelper.wrapDegrees(desired[1]-mc.player.getPitch());
        return (float)Math.sqrt(yaw*yaw+pitch*pitch);
    }
    public static void tick(MinecraftClient mc) {
        if (!LexyClient.enabled.getOrDefault("AimAssist",false) || mc==null || mc.player==null || mc.world==null
                || mc.currentScreen!=null || !mc.player.isAlive() || mc.player.isSpectator()
                || mc.player.isUsingItem() || mc.options.useKey.isPressed()
                || (requireHold && (LexyKeybinds.aimHold==null || !LexyKeybinds.aimHold.isPressed()))
                || (requireWeapon && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                    && !(mc.player.getMainHandStack().getItem() instanceof AxeItem))) {reset();return;}
        double radius=MathHelper.clamp(rangeTenths / 10.0,2.5,6.0);
        double radiusSq=radius*radius;
        long now=System.nanoTime()/1_000_000L;
        // A brief lock prevents flickering between players when the crosshair crosses the midpoint.
        if (locked!=null && (!allowed(mc,locked,radiusSq)
                || angleDistance(mc,locked)>fovDegrees * .75f || now-lockedAt>Math.max(200,lockMs)*5L)) {
            locked=null;
        }
        if (locked==null && now>=nextSearch) {
            nextSearch=now+75;
            double best=fovDegrees * .5;
            List<LivingEntity> candidates=mc.world.getEntitiesByClass(LivingEntity.class,
                    mc.player.getBoundingBox().expand(radius), e->e!=mc.player && e.isAlive());
            for (LivingEntity e:candidates) {
                if (!allowed(mc,e,radiusSq))continue;
                double angle=angleDistance(mc,e);
                if (angle<best) {best=angle;locked=e;}
            }
            if (locked!=null) {lockedAt=now;lastId=locked.getId();targetsSeen++;}
        }
        if (locked==null)return;
        float[] needed=angles(mc.player.getEyePos(),point(locked));
        float dy=MathHelper.wrapDegrees(needed[0]-mc.player.getYaw());
        float dp=MathHelper.wrapDegrees(needed[1]-mc.player.getPitch());
        // 2 stage cap: smoothing is adjustable but never instantly snaps to the entity.
        float portion=MathHelper.clamp((110-smoothness)/100f,.08f,.95f);
        float maxDelta=MathHelper.clamp(maxTurnTenths/10f, .5f, 24f);
        float yawStep=MathHelper.clamp(dy*portion,-maxDelta,maxDelta);
        float pitchStep=MathHelper.clamp(dp*portion,-maxDelta,maxDelta);
        if (Math.abs(dy) < .1f && Math.abs(dp) < .1f) return;
        mc.player.setYaw(mc.player.getYaw()+yawStep);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch()+pitchStep,-90f,90f));
    }
}