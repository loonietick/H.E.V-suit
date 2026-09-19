package ltown.hev_suit.client.integration;

import com.mojang.math.Axis;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3d;
import org.joml.Vector3d;

/**
 * HL2-style cone floodlight: a signed-distance-field cone attached to the holder entity's eye
 * position and view direction, illuminating whatever's in front of them rather than a single
 * fixed point on a surface. Ported from LambDynamicLights' own reference flashlight
 * (LambdAurora/Illuminated's FlashlightLightBehavior), adapted to this mod's mapping/version
 * matrix. Two euclidean spaces are used: world space (usual Minecraft space) and entity space
 * (eye direction along -y, cone centered on (0, 0)), converted between via a rotation matrix.
 */
class FlashlightConeLight implements DynamicLightBehavior {
    private static final double RADIUS = 2.2;
    private static final double DEPTH = 7.5;
    private static final double DISTANCE_DELTA = 1.2;

    private final Entity entity;
    private double prevX;
    private double prevY;
    private double prevZ;
    private float prevYaw;
    private float prevPitch;
    private Matrix3d rotationMatrix;
    private Matrix3d inverseRotationMatrix;

    FlashlightConeLight(Entity entity) {
        this.entity = entity;
        computeMatrices();
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        Vector3d coord = worldToEntitySpace(new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));

        double sdf = Math.min(
                RADIUS * (0.5 - coord.y() / DEPTH) - Math.sqrt(coord.x() * coord.x() + coord.z() * coord.z()),
                DEPTH * 0.5 - Math.abs(coord.y())
        );

        double distance = DEPTH / 2.0 - coord.y() - DISTANCE_DELTA;
        if (distance <= 0.0) {
            return 0.0;
        }

        double intensity = DEPTH / Math.pow(distance, 1.5);
        double light = intensity * 15.0;

        return smoothstep(sdf) * light;
    }

    @Override
    public BoundingBox getBoundingBox() {
        double[] horizontal = {-RADIUS, RADIUS};
        double[] vertical = {-Math.ceil(DEPTH / 2.0), Math.floor(DEPTH / 2.0)};

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (double x : horizontal) {
            for (double y : vertical) {
                for (double z : horizontal) {
                    Vector3d world = entityToWorldSpace(new Vector3d(x, y, z));
                    minX = Math.min(minX, world.x());
                    maxX = Math.max(maxX, world.x());
                    minY = Math.min(minY, world.y());
                    maxY = Math.max(maxY, world.y());
                    minZ = Math.min(minZ, world.z());
                    maxZ = Math.max(maxZ, world.z());
                }
            }
        }

        return new BoundingBox(
                (int) Math.floor(minX), (int) Math.floor(minY), (int) Math.floor(minZ),
                (int) Math.ceil(maxX), (int) Math.ceil(maxY), (int) Math.ceil(maxZ)
        );
    }

    @Override
    public boolean hasChanged() {
        if (Math.abs(entity.getX() - prevX) >= 0.1
                || Math.abs(entity.getY() - prevY) >= 0.1
                || Math.abs(entity.getZ() - prevZ) >= 0.1
                || Math.abs(entity.getYRot() - prevYaw) >= 0.1
                || Math.abs(entity.getXRot() - prevPitch) >= 0.1) {
            prevX = entity.getX();
            prevY = entity.getY();
            prevZ = entity.getZ();
            prevYaw = entity.getYRot();
            prevPitch = entity.getXRot();
            computeMatrices();
            return true;
        }
        return false;
    }

    private void computeMatrices() {
        Matrix3d matrix = new Matrix3d();
        matrix.rotate(Axis.ZP.rotationDegrees(entity.getXRot()));
        matrix.rotate(Axis.ZN.rotation(Mth.HALF_PI));
        matrix.rotate(Axis.YP.rotationDegrees(entity.getYRot()));
        matrix.rotate(Axis.YP.rotation(Mth.HALF_PI));
        this.rotationMatrix = matrix;
        this.inverseRotationMatrix = matrix.invert(new Matrix3d());
    }

    // ! mutates `in` !
    private Vector3d worldToEntitySpace(Vector3d in) {
        in.sub(entity.getX(), entity.getEyeY(), entity.getZ());
        in.mul(this.rotationMatrix);
        in.y += DEPTH / 2.0 - DISTANCE_DELTA;
        return in;
    }

    // ! mutates `in` !
    private Vector3d entityToWorldSpace(Vector3d in) {
        in.y -= DEPTH / 2.0 - DISTANCE_DELTA;
        in.mul(this.inverseRotationMatrix);
        in.add(entity.getX(), entity.getEyeY(), entity.getZ());
        return in;
    }

    private static double smoothstep(double x) {
        double t = Math.max(0.0, Math.min(1.0, x));
        return t * t * (3.0 - 2.0 * t);
    }
}
