package ltown.hev_suit.client.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * Owns the flashlight toggle, raycast target, and battery. Framework-agnostic on purpose — this
 * class knows nothing about LambDynamicLights. The optional integration
 * (ltown.hev_suit.client.integration, only loaded if LambDynamicLights itself is present) polls
 * {@link #isOn()} and {@link #getTargetPos()} each tick to place/move/remove its own light source.
 */
public class FlashlightManager {
    private static final double MAX_DISTANCE = 32.0;

    // HL1's real numbers (dlls/player.cpp): FLASH_DRAIN_TIME = 1.2s/unit while on (100 units = 2
    // minutes), FLASH_CHARGE_TIME = 0.2s/unit while off (100 units = 20 seconds), translated to
    // 20-tick-per-second client ticks.
    private static final int DRAIN_TICKS_PER_UNIT = 24;
    private static final int CHARGE_TICKS_PER_UNIT = 4;
    private static final int LOW_BATTERY_THRESHOLD = 20;

    private static final KeyBinding FLASHLIGHT_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.hev_suit.toggle_flashlight",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "key.categories.hev_suit"
            )
    );

    private static volatile boolean on = false;
    private static volatile BlockPos targetPos = null;
    private static volatile int battery = 100;
    private static int batteryTickCounter = 0;

    public static void registerFlashlight() {
        ClientTickEvents.END_CLIENT_TICK.register(FlashlightManager::onClientTick);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> resetBattery());
    }

    private static void resetBattery() {
        battery = 100;
        batteryTickCounter = 0;
        on = false;
        targetPos = null;
    }

    private static void onClientTick(MinecraftClient client) {
        while (FLASHLIGHT_KEY.wasPressed()) {
            if (on) {
                on = false;
            } else if (battery > 0) {
                on = true;
            }
        }

        updateBattery();

        if (!on || client.player == null || client.world == null) {
            targetPos = null;
            return;
        }

        targetPos = raycastTarget(client.player);
    }

    // Same drain-then-auto-shutoff / recharge-while-off loop as HL1's per-tick flashlight update.
    // Unlike the original, toggling on is blocked above when battery is already 0 -- the real
    // CBasePlayer::FlashlightTurnOn() has no such guard, which (read literally) lets the light
    // stay on forever at 0 battery without ever recharging. That looked like an unintentional
    // engine quirk rather than something worth reproducing.
    private static void updateBattery() {
        batteryTickCounter++;
        if (on) {
            if (batteryTickCounter >= DRAIN_TICKS_PER_UNIT) {
                batteryTickCounter = 0;
                if (battery > 0) {
                    battery--;
                }
                if (battery <= 0) {
                    battery = 0;
                    on = false;
                }
            }
        } else if (battery < 100) {
            if (batteryTickCounter >= CHARGE_TICKS_PER_UNIT) {
                batteryTickCounter = 0;
                battery++;
            }
        } else {
            batteryTickCounter = 0;
        }
    }

    private static BlockPos raycastTarget(PlayerEntity player) {
        // Combined block+entity raycast along the player's view vector — an entity hit wins over
        // whatever block is behind it, matching how the vanilla crosshair itself picks targets.
        HitResult hit = ProjectileUtil.getCollision(
                player,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator(),
                MAX_DISTANCE
        );

        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            return BlockPos.ofFloored(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ());
        }

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            // Light the air just in front of the surface, not the solid block itself.
            return blockHit.getBlockPos().offset(blockHit.getSide());
        }

        return null;
    }

    public static boolean isOn() {
        return on;
    }

    public static int getBattery() {
        return battery;
    }

    public static boolean isLowBattery() {
        return battery < LOW_BATTERY_THRESHOLD;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }
}
