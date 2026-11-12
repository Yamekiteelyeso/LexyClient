package com.lexy.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;

import java.util.concurrent.ThreadLocalRandom;

/** Tick-driven, bounded auto-clicks using normal client interactions. Never clicks a screen. */
public final class AutoClicker {
    private AutoClicker() {}
    public enum Buttons { LEFT, RIGHT, BOTH }
    public enum Activation { HOLD_MOUSE, HOLD_KEY, TOGGLE }

    public static Buttons buttons=Buttons.LEFT;
    public static Activation activation=Activation.HOLD_MOUSE;
    public static int minCps=6, maxCps=10, jitterMs=15, leftReachTenths=29;
    public static int attackChargePercent=85;
    public static boolean requireWeapon=true, ignoreTeam=true, ignoreInvisible=true;
    public static boolean airSwings=false, rightClickBlocks=true, rightClickItems=false;
    public static boolean requireSneak=false, pauseWhileUsing=true, pauseDuringTotem=true;

    private static long nextLeft, nextRight;
    private static boolean wasLeftHeld, wasRightHeld;
    private static long performedLeft, performedRight;
    private static long now() { return System.nanoTime()/1_000_000L; }
    private static boolean leftEnabled(){return buttons==Buttons.LEFT || buttons==Buttons.BOTH;}
    private static boolean rightEnabled(){return buttons==Buttons.RIGHT || buttons==Buttons.BOTH;}

    public static String status(){
        if (!LexyClient.enabled.getOrDefault("AutoClicker",false)) return "OFF";
        return buttons.name()+" · "+performedLeft+"L / "+performedRight+"R";
    }
    public static void reset(){ nextLeft=nextRight=0;wasLeftHeld=wasRightHeld=false; }
    private static boolean canRun(MinecraftClient mc){
        return LexyClient.enabled.getOrDefault("AutoClicker",false) && mc!=null && mc.player!=null
                && mc.world!=null && mc.interactionManager!=null && mc.currentScreen==null
                && mc.player.isAlive() && !mc.player.isSpectator()
                && (!requireSneak || mc.options.sneakKey.isPressed())
                && (!pauseWhileUsing || !mc.player.isUsingItem())
                && (!pauseDuringTotem || !AutoTotem.inProgress());
    }
    private static boolean activated(MinecraftClient mc, boolean left){
        return switch(activation){
            case HOLD_MOUSE -> left?mc.options.attackKey.isPressed():mc.options.useKey.isPressed();
            case HOLD_KEY -> LexyKeybinds.clickerHold!=null && LexyKeybinds.clickerHold.isPressed();
            case TOGGLE -> true;
        };
    }
    public static boolean handlesLeft(MinecraftClient mc){
        return canRun(mc) && leftEnabled() && activated(mc,true)
                && (!requireWeapon || mc.player.getMainHandStack().getItem() instanceof SwordItem
                   || mc.player.getMainHandStack().getItem() instanceof AxeItem);
    }
    private static int interval(){
        int lo=Math.max(1,Math.min(18,minCps));
        int hi=Math.max(lo,Math.min(18,maxCps));
        int cps=lo==hi?lo:ThreadLocalRandom.current().nextInt(lo,hi+1);
        int jitter=Math.max(0,Math.min(100,jitterMs));
        return Math.max(55,1000/cps+(jitter==0?0:ThreadLocalRandom.current().nextInt(-jitter,jitter+1)));
    }
    private static boolean doLeft(MinecraftClient mc){
        if (requireWeapon && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)
                && !(mc.player.getMainHandStack().getItem() instanceof AxeItem)) return false;
        if (mc.player.getAttackCooldownProgress(0.5f) < attackChargePercent/100f) return false;
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target){
            Entity entity=hit.getEntity();
            if (!target.isAlive() || target==mc.player || !target.isAttackable())return false;
            if (ignoreInvisible && target.isInvisible())return false;
            if (ignoreTeam && entity instanceof PlayerEntity && mc.player.isTeammate(entity))return false;
            double reach=Math.min(3.0,Math.max(2.0,leftReachTenths/10.0));
            if (mc.player.getEyePos().squaredDistanceTo(hit.getPos())>reach*reach || !mc.player.canSee(entity))return false;
            mc.interactionManager.attackEntity(mc.player,entity);
            mc.player.swingHand(Hand.MAIN_HAND);
            performedLeft++;
            return true;
        }
        if (airSwings && !(mc.crosshairTarget instanceof BlockHitResult)) {
            mc.player.swingHand(Hand.MAIN_HAND);performedLeft++;return true;
        }
        return false;
    }
    private static boolean doRight(MinecraftClient mc){
        if (mc.player.isUsingItem())return false;
        if (rightClickBlocks && mc.crosshairTarget instanceof BlockHitResult block){
            // A normal Minecraft interaction, never an inventory/window click.
            if(mc.interactionManager.interactBlock(mc.player,Hand.MAIN_HAND,block).isAccepted()){
                mc.player.swingHand(Hand.MAIN_HAND);performedRight++;return true;
            }
        }
        if (rightClickItems && !mc.player.getMainHandStack().isEmpty()){
            if(mc.interactionManager.interactItem(mc.player,Hand.MAIN_HAND).isAccepted()){
                mc.player.swingHand(Hand.MAIN_HAND);performedRight++;return true;
            }
        }
        return false;
    }
    public static void tick(MinecraftClient mc){
        if (!canRun(mc)){reset();return;}
        long time=now();
        boolean left=leftEnabled() && activated(mc,true);
        boolean right=rightEnabled() && activated(mc,false);
        if (left){
            if(!wasLeftHeld)nextLeft=time+interval();
            else if(time>=nextLeft){doLeft(mc);nextLeft=time+interval();}
        } else nextLeft=0;
        if (right){
            if(!wasRightHeld)nextRight=time+interval();
            else if(time>=nextRight){doRight(mc);nextRight=time+interval();}
        } else nextRight=0;
        wasLeftHeld=left;wasRightHeld=right;
    }
}