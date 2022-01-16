package ru.ckateptb.abilityslots.avatar.earth.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.earth.EarthElement;
import ru.ckateptb.abilityslots.common.util.VectorUtils;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.temporary.fallingblock.TemporaryFallingBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "EarthKick",
        displayName = "EarthKick",
        activationMethods = {ActivationMethod.SNEAK},
        category = "earth",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 7000
)
@CollisionParticipant
public class EarthKick extends Ability {
    @ConfigField
    private static int blocks = 10;
    @ConfigField
    private static double damage = 4;
    @ConfigField
    private static double activationRange = 2;
    @ConfigField
    private static double maxTravelDistance = 10;

    private final List<TemporaryFallingBlock> fallingBlocks = new ArrayList<>();
    private ImmutableVector origin;
    private ImmutableVector direction;
    private BlockData blockData;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        Block block = user.findBlock(activationRange + 1, true, true);
        if (block == null || !EarthElement.isEarthNotLava(user, block) || !user.canUse(block.getLocation())) return ActivateResult.NOT_ACTIVATE;
        this.origin = new ImmutableVector(block.getLocation().toCenterLocation());
        this.blockData = block.getBlockData();
        this.direction = user.getDirection().setY(0).normalize();
        this.launchBlocks();
        user.setCooldown(this);
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        List<Integer> ids = new ArrayList<>();
        for (TemporaryFallingBlock temporaryFallingBlock : fallingBlocks) {
            FallingBlock fallingBlock = temporaryFallingBlock.getFallingBlock();
            if (fallingBlock == null || fallingBlock.isDead()) {
                ids.add(fallingBlocks.indexOf(temporaryFallingBlock));
                continue;
            }
            Location location = fallingBlock.getLocation();
            Particle.BLOCK_CRACK.display(location, 2, (float) Math.random(), (float) Math.random(), (float) Math.random(), 0.1, blockData);
            Particle.BLOCK_CRACK.display(location, 2, (float) Math.random(), (float) Math.random(), (float) Math.random(), 0.2, blockData);
            AxisAlignedBoundingBoxCollider collider = new AxisAlignedBoundingBoxCollider(fallingBlock).at(new ImmutableVector(location));
            collider.handleEntityCollision(livingEntity, true, entity -> {
                AbilityTarget.of((LivingEntity) entity).damage(damage, this);
                return CollisionCallbackResult.CONTINUE;
            });
        }
        for (int id : ids) {
            if (id < fallingBlocks.size()) {
                fallingBlocks.remove(id);
            }
        }
        if (fallingBlocks.isEmpty()) {
            return UpdateResult.REMOVE;
        }
        return UpdateResult.CONTINUE;
    }

    private void launchBlocks() {
        EarthElement.play(origin.toLocation(world));
        for (int i = 0; i < blocks; i++) {
            ImmutableVector direction = this.direction;
            Random random = new Random();
            direction = VectorUtils.rotateYaw(direction, Math.toRadians(random.nextInt(-20, 20)));
            direction = VectorUtils.rotatePitch(direction, Math.toRadians(random.nextInt(-45, -20)));
            direction = direction.add(0, 0.8, 0).normalize();
            TemporaryFallingBlock temporaryFallingBlock = new TemporaryFallingBlock(origin.toLocation(world).add(0, 1, 0), blockData, maxTravelDistance, false, true);
            temporaryFallingBlock.getFallingBlock().setVelocity(direction);
            fallingBlocks.add(temporaryFallingBlock);
        }
    }

    @Override
    public Collection<Collider> getColliders() {
        return fallingBlocks.stream().map(TemporaryFallingBlock::getFallingBlock).map(AxisAlignedBoundingBoxCollider::new).collect(Collectors.toList());
    }

    @Override
    public void destroy() {

    }
}
