package ltown.hev_suit.client.integration;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import ltown.hev_suit.client.managers.FlashlightManager;
import ltown.hev_suit.client.managers.SettingsManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Only ever loaded/invoked by LambDynamicLights itself, via the "lambdynlights:initializer"
 * entrypoint declared in fabric.mod.json. If LambDynamicLights isn't installed, nothing queries
 * that entrypoint and this class is never classloaded — that's the entire mechanism behind the
 * flashlight being a no-op without the dependency, no isModLoaded checks needed here at all.
 * <p>
 * Switches between two light behaviors based on {@link SettingsManager#hl2FlashlightEnabled}:
 * the default HL1 point light at the raycast target, or the HL2 cone floodlight attached to the
 * player. Mode changes swap the registered behavior cleanly, even mid-toggle.
 */
public class HevSuitDynamicLights implements DynamicLightsInitializer {
    private FlashlightPointLight pointLight;
    private FlashlightConeLight coneLight;
    private PlayerEntity coneLightOwner;
    private DynamicLightBehavior activeLight;

    // This LDL build's DynamicLightsInitializer declares onInitializeDynamicLights() (no-arg) as
    // the abstract method and the context-taking overload below as a default that just forwards
    // to it — so the no-arg override is required to compile but is never actually invoked; the
    // entrypoint loader calls the context overload directly, which we override below.
    @Override
    public void onInitializeDynamicLights() {
    }

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        DynamicLightBehaviorManager manager = context.dynamicLightBehaviorManager();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            boolean hl2Mode = SettingsManager.hl2FlashlightEnabled;
            boolean flashlightOn = FlashlightManager.isOn() && player != null;

            DynamicLightBehavior wanted = null;
            if (flashlightOn && hl2Mode) {
                if (this.coneLight == null || this.coneLightOwner != player) {
                    this.coneLight = new FlashlightConeLight(player);
                    this.coneLightOwner = player;
                }
                wanted = this.coneLight;
            } else if (flashlightOn) {
                BlockPos target = FlashlightManager.getTargetPos();
                if (target != null) {
                    if (this.pointLight == null) {
                        this.pointLight = new FlashlightPointLight(target);
                    } else {
                        this.pointLight.setPosition(target);
                    }
                    wanted = this.pointLight;
                }
            }

            if (wanted != this.activeLight) {
                if (this.activeLight != null) {
                    manager.remove(this.activeLight);
                }
                if (wanted != null) {
                    manager.add(wanted);
                }
                this.activeLight = wanted;
            }
        });
    }
}
