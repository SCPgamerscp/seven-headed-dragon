package com.sevenheadeddragon.event;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/** Makes Worm Dragon servants fire their specified projectile on every game tick. */
public class WormDragonMinionHandler {
    private static final String MINION = "WormDragonMinion";
    private static final String PROJECTILE = "WormDragonProjectile";
    private static final List<MobEffect> ARROW_EFFECTS = List.of(MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER);

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        // Snapshot first: firing adds projectiles to the level and must not mutate
        // the live entity collection while it is being iterated.
        List<Entity> entities = new ArrayList<>();
        level.getAllEntities().forEach(entities::add);
        for (Entity entity : entities) {
            if (entity.getPersistentData().getBoolean(PROJECTILE) && entity.tickCount > 100) {
                entity.discard();
            } else if (entity instanceof Mob mob && mob.isAlive() && mob.getPersistentData().getBoolean(MINION)) {
                LivingEntity target = mob.getTarget();
                if (mob.tickCount % 5 == 0 && target != null && target.isAlive() && mob.distanceToSqr(target) < 160.0D * 160.0D) fire(level, mob, target);
            }
        }
    }

    private static void fire(ServerLevel level, Mob shooter, LivingEntity target) {
        Vec3 origin = shooter.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(origin).normalize();
        Entity projectile = null;

        if (shooter instanceof Skeleton) {
            Arrow arrow = new Arrow(level, shooter);
            arrow.addEffect(new MobEffectInstance(ARROW_EFFECTS.get(shooter.getRandom().nextInt(ARROW_EFFECTS.size())), 20 * 20, 1));
            projectile = arrow;
        } else if (shooter instanceof Drowned) {
            projectile = new ThrownTrident(level, shooter, new ItemStack(Items.TRIDENT));
            projectile.getPersistentData().putBoolean("WormDragonChanneling", true);
        } else if (shooter instanceof Shulker) {
            projectile = new ShulkerBullet(level, shooter, target, Direction.Axis.Y);
        } else if (shooter instanceof Llama llama) {
            projectile = new LlamaSpit(level, llama);
        } else if (shooter instanceof Ghast) {
            projectile = new LargeFireball(level, shooter, aim.x, aim.y, aim.z, 1);
        } else if (shooter instanceof Blaze) {
            projectile = new SmallFireball(level, shooter, aim.x, aim.y, aim.z);
        } else if (shooter instanceof Pillager) {
            projectile = new FireworkRocketEntity(level, fireworkStack(), shooter, origin.x, origin.y, origin.z, true);
        }

        if (projectile == null) return;
        projectile.getPersistentData().putBoolean(PROJECTILE, true);
        if (!(projectile instanceof ShulkerBullet)) {
            projectile.setPos(origin.x, origin.y, origin.z);
            if (projectile instanceof net.minecraft.world.entity.projectile.Projectile p) p.shoot(aim.x, aim.y, aim.z, 2.2F, 0.0F);
        }
        level.addFreshEntity(projectile);
    }

    private static ItemStack fireworkStack() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fireworks = new CompoundTag();
        fireworks.putByte("Flight", (byte) 1);
        ListTag explosions = new ListTag();
        int[] colors = {0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0x8B00FF};
        for (int color : colors) {
            CompoundTag explosion = new CompoundTag();
            explosion.putByte("Type", (byte) 1);
            explosion.putIntArray("Colors", new int[]{color});
            explosion.putBoolean("Trail", true);
            explosions.add(explosion);
        }
        fireworks.put("Explosions", explosions);
        stack.getOrCreateTag().put("Fireworks", fireworks);
        return stack;
    }

    /** Ghast fireballs retain entity damage but never remove terrain. */
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (source != null && source.getPersistentData().getBoolean(PROJECTILE)) event.getAffectedBlocks().clear();
    }

    /** Channeling tridents summon lightning on hit regardless of weather. */
    @SubscribeEvent
    public void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (!event.getProjectile().getPersistentData().getBoolean("WormDragonChanneling")) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult)) return;
        if (!(event.getProjectile().level() instanceof ServerLevel level)) return;
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(event.getRayTraceResult().getLocation());
            level.addFreshEntity(bolt);
        }
    }

    /** Prevents Worm Dragon minions from taking friendly-fire damage from each other or the boss. */
    @SubscribeEvent
    public void onMinionHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.getPersistentData().getBoolean(MINION) || victim instanceof com.sevenheadeddragon.entity.WormDragonEntity) {
            Entity attacker = event.getSource().getEntity();
            Entity directSource = event.getSource().getDirectEntity();
            if ((attacker != null && (attacker.getPersistentData().getBoolean(MINION) || attacker instanceof com.sevenheadeddragon.entity.WormDragonEntity))
                    || (directSource != null && (directSource.getPersistentData().getBoolean(MINION) || directSource.getPersistentData().getBoolean(PROJECTILE)))) {
                event.setCanceled(true);
            }
        }
    }

    /** Prevents Worm Dragon minions from locking onto each other as attack targets. */
    @SubscribeEvent
    public void onMinionTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getPersistentData().getBoolean(MINION)) {
            LivingEntity newTarget = event.getNewTarget();
            if (newTarget != null && (newTarget.getPersistentData().getBoolean(MINION) || newTarget instanceof com.sevenheadeddragon.entity.WormDragonEntity)) {
                event.setCanceled(true);
            }
        }
    }
}
