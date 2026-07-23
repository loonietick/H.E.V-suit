package ltown.hev_suit.client.integration;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.util.math.BlockPos;

/**
 * A single point light with radial falloff — the canonical example from DynamicLightBehavior's
 * own javadoc. Position is mutable so the flashlight's one behavior instance can be moved each
 * tick instead of being torn down and recreated as the player looks around.
 */
class FlashlightPointLight implements DynamicLightBehavior {
    // Brightness at the light's center, kept at max so the beam actually reads as a bright
    // flashlight rather than a dim smudge.
    private static final double PEAK_LUMINANCE = 15.0;
    // Distance in blocks at which the light reaches 0 — the actual "how far it spreads" knob,
    // independent of brightness. Deliberately ignores falloffRatio so the beam's physical size
    // stays fixed rather than scaling with lighting-quality settings.
    private static final double RADIUS = 3.0;

    private double x;
    private double y;
    private double z;
    private double prevX = Double.NaN;
    private double prevY = Double.NaN;
    private double prevZ = Double.NaN;

    FlashlightPointLight(BlockPos pos) {
        setPosition(pos);
    }

    void setPosition(BlockPos pos) {
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        double dx = pos.getX() + 0.5 - this.x;
        double dy = pos.getY() + 0.5 - this.y;
        double dz = pos.getZ() + 0.5 - this.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance >= RADIUS) {
            return 0.0;
        }
        return PEAK_LUMINANCE * (1.0 - distance / RADIUS);
    }

    @Override
    public BoundingBox getBoundingBox() {
        int bx = (int) Math.floor(this.x);
        int by = (int) Math.floor(this.y);
        int bz = (int) Math.floor(this.z);
        return new BoundingBox(bx, by, bz, bx + 1, by + 1, bz + 1);
    }

    @Override
    public boolean hasChanged() {
        if (this.x != this.prevX || this.y != this.prevY || this.z != this.prevZ) {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            return true;
        }
        return false;
    }
}
