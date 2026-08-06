package com.sevenheadeddragon.event;

import com.sevenheadeddragon.SevenHeadedDragon;
import com.sevenheadeddragon.entity.CentipedeBossEntity;
import com.sevenheadeddragon.entity.FangKingEntity;
import com.sevenheadeddragon.entity.PotionMasterEntity;
import com.sevenheadeddragon.entity.dragon.ApocalypseSevenHeadedRedDragonEntity;
import com.sevenheadeddragon.entity.dragon.RainbowLightningEntity;
import com.sevenheadeddragon.registry.ModEffects;
import com.sevenheadeddragon.registry.ModItems;
import com.sevenheadeddragon.registry.ModPotions;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Handles custom triggers and progress logic for all 28 mod advancements.
 */
@Mod.EventBusSubscriber(modid = SevenHeadedDragon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ModAdvancementEvents {

    private ModAdvancementEvents() {}

    /** Grants a custom advancement by string ID. */
    public static void grant(ServerPlayer player, String advancementId) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResourceLocation loc = new ResourceLocation(SevenHeadedDragon.MODID, advancementId);
        Advancement advancement = server.getAdvancements().getAdvancement(loc);
        if (advancement == null) return;

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    // ------------------------------------------------------------------
    // Tracking active boss fights per player / boss entity
    // ------------------------------------------------------------------

    private static final Map<UUID, BossFightTracker> activeBossFights = new HashMap<>();
    private static final Map<UUID, List<Long>> playerDeathTimestamps = new HashMap<>();

    private static class BossFightTracker {
        final UUID bossUUID;
        final long startTimeTick;
        final Set<UUID> playerUUIDs = new HashSet<>();
        final Set<UUID> damagedPlayers = new HashSet<>();

        BossFightTracker(UUID bossUUID, long startTimeTick) {
            this.bossUUID = bossUUID;
            this.startTimeTick = startTimeTick;
        }
    }

    private static boolean isAnyBoss(Entity entity) {
        return entity instanceof PotionMasterEntity
                || entity instanceof CentipedeBossEntity
                || entity instanceof FangKingEntity
                || entity instanceof ApocalypseSevenHeadedRedDragonEntity;
    }

    // ------------------------------------------------------------------
    // Event: Boss Spawning / Encountering
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();

        if (entity instanceof PotionMasterEntity) {
            for (ServerPlayer p : event.getLevel().getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(64.0))) {
                grant(p, "root");
                grant(p, "spawn_potion_master");
            }
        } else if (entity instanceof CentipedeBossEntity) {
            for (ServerPlayer p : event.getLevel().getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(64.0))) {
                grant(p, "spawn_centipede");
            }
        } else if (entity instanceof FangKingEntity) {
            for (ServerPlayer p : event.getLevel().getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(64.0))) {
                grant(p, "spawn_fang_king");
            }
        } else if (entity instanceof ApocalypseSevenHeadedRedDragonEntity) {
            for (ServerPlayer p : event.getLevel().getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(128.0))) {
                grant(p, "spawn_red_dragon");
            }
        }
    }

    // ------------------------------------------------------------------
    // Event: Living Hurt / Damage Triggers
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();

        // ⚡ 七色の雷に打たれた -> 「夢と希望」
        if (victim instanceof ServerPlayer player && direct instanceof RainbowLightningEntity) {
            grant(player, "hit_by_rainbow_lightning");
        }

        // 🐉 赤い竜由来のダメージ -> 「地獄で焼かれるような痛み」
        if (victim instanceof ServerPlayer player && (attacker instanceof ApocalypseSevenHeadedRedDragonEntity || direct instanceof RainbowLightningEntity)) {
            grant(player, "damaged_by_dragon");
        }

        // 🦇 エナジードリンク効果中にファントムからダメージ -> 「エナジードリンク飲んでないで寝なさい」
        if (victim instanceof ServerPlayer player && attacker instanceof Phantom) {
            if (player.hasEffect(MobEffects.DIG_SPEED) && player.getEffect(MobEffects.DIG_SPEED).getAmplifier() >= 3) {
                grant(player, "phantom_attack_energy_drink");
            }
        }

        // 🐛 オオムカデに虫特効/殺虫ポーションでダメージ -> 「ここで役に立つのか!!」
        if (victim instanceof CentipedeBossEntity) {
            if (attacker instanceof ServerPlayer player) {
                ItemStack mainHand = player.getMainHandItem();
                int baneLvl = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS, mainHand);
                if (baneLvl > 0 || player.hasEffect(ModEffects.INSECTICIDE.get())) {
                    grant(player, "insecticide_on_centipede");
                }
            }
        }

        // 👑 牙の王に負傷の矢 / ポーションマスターのポーションを使う
        if (victim instanceof FangKingEntity) {
            if (attacker instanceof ServerPlayer player) {
                if (direct instanceof AbstractArrow) {
                    grant(player, "harming_arrow_on_fang_king");
                } else if (direct instanceof ThrownPotion) {
                    grant(player, "potion_master_potion_on_fang_king");
                }
            }
        }

        // 🛡️ ドッジマスター: ボス戦中のプレイヤー被害記録
        if (victim instanceof ServerPlayer player) {
            for (BossFightTracker tracker : activeBossFights.values()) {
                if (tracker.playerUUIDs.contains(player.getUUID())) {
                    tracker.damagedPlayers.add(player.getUUID());
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Event: Living Death Triggers & Boss Defeat
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        // ☠️ 赤い竜に殺された -> 「私が行くのは天国か?地獄か?」
        if (victim instanceof ServerPlayer player && attacker instanceof ApocalypseSevenHeadedRedDragonEntity) {
            grant(player, "killed_by_dragon");
        }

        // 🪽 元熾天使の翼着用中に落下死または激突死 -> 「天使も天から落ちる」
        if (victim instanceof ServerPlayer player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            boolean wearingWings = !chest.isEmpty() && (chest.is(ModItems.APOCALYPSE_ELYTRA.get()) || (chest.is(Items.ELYTRA) && chest.hasTag() && chest.getTag().getBoolean("Unbreakable")));
            if (wearingWings) {
                net.minecraft.world.damagesource.DamageSource source = event.getSource();
                if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL) || source.is(net.minecraft.world.damagesource.DamageTypes.FLY_INTO_WALL)) {
                    grant(player, "angel_falls_from_heaven");
                }
            }
        }

        // ☠️ ボス戦中に10分以内で5回死亡 -> 「ゾンビ戦法するならエナジードリンク飲めば?」
        if (victim instanceof ServerPlayer player) {
            long now = player.level().getGameTime();
            List<Long> deaths = playerDeathTimestamps.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            deaths.add(now);
            // 10 minutes = 12000 ticks
            deaths.removeIf(t -> now - t > 12000);
            if (deaths.size() >= 5) {
                grant(player, "zombie_strategy");
            }
        }

        // 🏆 ボス撃破時の処理
        if (isAnyBoss(victim)) {
            BossFightTracker tracker = activeBossFights.remove(victim.getUUID());
            long now = victim.level().getGameTime();

            // ワンパン判定: 最大体力からの即死
            if (victim.getHealth() >= victim.getMaxHealth() - 0.5F && attacker instanceof ServerPlayer p) {
                grant(p, "one_punch_man");
            }

            for (ServerPlayer p : victim.level().getEntitiesOfClass(ServerPlayer.class, victim.getBoundingBox().inflate(64.0))) {
                // ⏱️ 7分以内撃破 -> 「そこをどけ、私はRTA走者だ」
                if (tracker != null && (now - tracker.startTimeTick) <= 20 * 60 * 7) {
                    grant(p, "rta_runner");
                }

                // 🛡️ ノーダメージ撃破 -> 「ドッジマスター」
                if (tracker != null && !tracker.damagedPlayers.contains(p.getUUID())) {
                    grant(p, "dodge_master");
                }

                // 個別撃破実績
                if (victim instanceof PotionMasterEntity) {
                    grant(p, "defeat_potion_master");
                } else if (victim instanceof CentipedeBossEntity) {
                    grant(p, "defeat_centipede");
                } else if (victim instanceof FangKingEntity) {
                    grant(p, "defeat_fang_king");
                } else if (victim instanceof ApocalypseSevenHeadedRedDragonEntity) {
                    grant(p, "kill_apocalypse_red_dragon");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Event: Player Tick & Passive Effect Checks
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // ⚡ エリトラ着用 -> 「元熾天使の翼」
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingWings = !chest.isEmpty() && (chest.is(ModItems.APOCALYPSE_ELYTRA.get()) || (chest.is(Items.ELYTRA) && chest.hasTag() && chest.getTag().getBoolean("Unbreakable")));
        if (wearingWings) {
            grant(player, "wear_apocalypse_elytra");

            // 🔥 天使の翼着用中にネザー滞在 -> 「ルシファー?」
            if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) {
                grant(player, "lucifer");
            }
        }

        // ☠️ 「神殺し」エフェクト -> 「神を殺す絶望の槍」
        if (player.hasEffect(ModEffects.GOD_SLAYING.get())) {
            grant(player, "god_slaying_effect");
        }

        // 🧪 デバフ10個 -> 「最悪なカクテル」
        long debuffCount = player.getActiveEffects().stream()
                .filter(e -> e.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                .count();
        if (debuffCount >= 10) {
            grant(player, "worst_cocktail");
        }

        // 😈 七つの大罪全種類 -> 「七つの大罪」
        if (player.hasEffect(ModEffects.SIN_PRIDE.get())
                && player.hasEffect(ModEffects.SIN_WRATH.get())
                && player.hasEffect(ModEffects.SIN_ENVY.get())
                && player.hasEffect(ModEffects.SIN_SLOTH.get())
                && player.hasEffect(ModEffects.SIN_GREED.get())
                && player.hasEffect(ModEffects.SIN_GLUTTONY.get())
                && player.hasEffect(ModEffects.SIN_LUST.get())) {
            grant(player, "all_seven_sins");
        }

        // 🗼 ビーコン効果範囲内でボス戦 -> 「戦闘の最適解はこれ!!」
        boolean hasBeacon = player.hasEffect(MobEffects.REGENERATION)
                || player.hasEffect(MobEffects.DAMAGE_BOOST)
                || player.hasEffect(MobEffects.MOVEMENT_SPEED)
                || player.hasEffect(MobEffects.DAMAGE_RESISTANCE);
        if (hasBeacon) {
            boolean nearBoss = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(32.0), ModAdvancementEvents::isAnyBoss).size() > 0;
            if (nearBoss) {
                grant(player, "beacon_strategy");
            }
        }

        // 🥊 トラッカーの同期・登録
        if (player.tickCount % 20 == 0) {
            List<LivingEntity> bosses = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(64.0), ModAdvancementEvents::isAnyBoss);
            for (LivingEntity boss : bosses) {
                BossFightTracker tracker = activeBossFights.computeIfAbsent(boss.getUUID(),
                        uuid -> new BossFightTracker(uuid, player.level().getGameTime()));
                tracker.playerUUIDs.add(player.getUUID());
            }
        }
    }

    // ------------------------------------------------------------------
    // Event: Item Usage (Drinking / Using Items)
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onItemUseFinish(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();

        // 🧪 エナジードリンク使用 -> 「これ使えば100徹まで頑張れる!!」
        if (stack.is(ModItems.ENERGY_DRINK.get())) {
            grant(player, "drink_energy_drink");

            // ボス戦中にエナジードリンク使用 -> 「最強のスター状態」
            boolean nearAnyBoss = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(64.0), ModAdvancementEvents::isAnyBoss).size() > 0;
            if (nearAnyBoss) {
                grant(player, "ultimate_star_mode");
            }
        }

        // ポーションマスター戦闘中のアイテム
        boolean nearPotionMaster = player.level().getEntitiesOfClass(PotionMasterEntity.class,
                player.getBoundingBox().inflate(32.0)).size() > 0;
        if (nearPotionMaster) {
            if (stack.is(Items.MILK_BUCKET)) {
                grant(player, "milk_in_potion_master_fight");
            } else if (stack.is(Items.POTION)) {
                grant(player, "drink_potion_in_potion_master_fight");
            }
        }

        // オオムカデ戦闘中のハチミツ解毒
        boolean nearCentipede = player.level().getEntitiesOfClass(CentipedeBossEntity.class,
                player.getBoundingBox().inflate(32.0)).size() > 0;
        if (nearCentipede && stack.is(Items.HONEY_BOTTLE) && player.hasEffect(MobEffects.POISON)) {
            grant(player, "honey_cure_poison_in_centipede_fight");
        }

        // 🔮 ボス戦中にエンダーパール使用 -> 「瞬間移動を逃亡に使うか戦術として使うか」
        if (stack.is(Items.ENDER_PEARL)) {
            boolean nearAnyBoss = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(64.0), ModAdvancementEvents::isAnyBoss).size() > 0;
            if (nearAnyBoss) {
                grant(player, "use_ender_pearl_in_boss_fight");
            }
        }
    }
}
