package club.redux.sunset.lavafishing.entity;

import club.redux.sunset.lavafishing.loot.LootTableHandler;
import club.redux.sunset.lavafishing.util.Reference;
import club.redux.sunset.lavafishing.util.RegistryCollection.EntityTypeCollection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EntityObsidianHook extends FishingHook {
    private final Random lavaTickRand = new Random();

    public EntityObsidianHook(EntityType<? extends FishingHook> entityType, Level pLevel) {
        super(entityType, pLevel);
    }

    public EntityObsidianHook(Player pPlayer, Level pLevel, int pLuck, int pLureSpeed) {
        super(pPlayer, pLevel, pLuck, pLureSpeed);
    }

    private boolean isInLavaFishing() {
        return this.level().getFluidState(this.blockPosition().offset(0, -1, 0)).is(FluidTags.LAVA) || this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA);
    }

    @Override
    public void tick() {
        if (isInLavaFishing()) {
            lavaFishingTick();
        } else {
            super.tick();
        }
    }

    private void projectileTick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
            this.hasBeenShot = true;
        }
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        super.baseTick();
    }

    protected void lavaFishingTick() {
        this.lavaTickRand.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level().getGameTime());
        projectileTick();
        //---vanilla code
        Player player = this.getPlayerOwner();
        if (player == null) {
            this.discard();
        } else if (this.level().isClientSide || !this.shouldStopFishing(player)) {
            if (this.onGround()) {
                ++this.life;
                if (this.life >= 1200) {
                    this.discard();
                    return;
                }
            } else {
                this.life = 0;
            }
            float fluidHeight = 0.0F;
            BlockPos blockpos = this.blockPosition();
            FluidState fluidState = this.level().getFluidState(blockpos);
            if (fluidState.is(FluidTags.LAVA)) {
                fluidHeight = fluidState.getHeight(this.level(), blockpos);
            }
            boolean inLava = fluidHeight > 0.0F;
            if (this.currentState == FishingHook.FishHookState.FLYING) {
                if (this.getHookedIn() != null) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.currentState = FishingHook.FishHookState.HOOKED_IN_ENTITY;
                    return;
                }

                if (inLava) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
                    this.currentState = FishingHook.FishHookState.BOBBING;
                    return;
                }

                this.checkCollision();
            } else {
                if (this.currentState == FishingHook.FishHookState.HOOKED_IN_ENTITY) {
                    if (this.getHookedIn() != null) {
                        if (!this.getHookedIn().isRemoved() && this.getHookedIn().level().dimension() == this.getHookedIn().level().dimension()) {
                            this.setPos(this.getHookedIn().getX(), this.getHookedIn().getY(0.8D), this.getHookedIn().getZ());
                        } else {
                            this.setHookedEntity((Entity) null);
                            this.currentState = FishingHook.FishHookState.FLYING;
                        }
                    }

                    return;
                }

                if (this.currentState == FishingHook.FishHookState.BOBBING) {
                    Vec3 vec3 = this.getDeltaMovement();
                    double d0 = this.getY() + vec3.y - (double) blockpos.getY() - (double) fluidHeight;
                    if (Math.abs(d0) < 0.01D) {
                        d0 += Math.signum(d0) * 0.1D;
                    }
                    this.setDeltaMovement(vec3.x * 0.9D, vec3.y - d0 * (double) this.random.nextFloat() * 0.2D, vec3.z * 0.9D);
                    if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                        this.openWater = true;
                    } else {
                        this.openWater = this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(blockpos);
                    }
                    //vanilla code---
                    if (inLava) {
                        this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                        if (this.biting) {
                            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D * (double) this.lavaTickRand.nextFloat() * (double) this.lavaTickRand.nextFloat(), 0.0D));
                        }

                        if (!this.level().isClientSide) {
                            this.lavaCatchingFish(blockpos);
                        }
                    } else {
                        this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
                    }
                }
            }
            if (!fluidState.is(FluidTags.LAVA)) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
            }
            //---vanilla code
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.updateRotation();
            if (this.currentState == FishingHook.FishHookState.FLYING && (this.onGround() || this.horizontalCollision)) {
                this.setDeltaMovement(Vec3.ZERO);
            }

            double d1 = 0.92D;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
            this.reapplyPosition();
            //vanilla code---
        }
    }

    @Override
    public int retrieve(ItemStack pStack) {
        if (isInLavaFishing()) {
            //---vanilla code
            Player player = this.getPlayerOwner();
            if (!this.level().isClientSide && player != null && !this.shouldStopFishing(player)) {
                int i = 0;
                net.minecraftforge.event.entity.player.ItemFishedEvent event = null;
                if (this.getHookedIn() != null) {
                    this.pullEntity(this.getHookedIn());
                    CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, pStack, this, Collections.emptyList());
                    this.level().broadcastEntityEvent(this, (byte) 31);
                    i = this.getHookedIn() instanceof ItemEntity ? 3 : 5;
                } else if (this.nibble > 0) {
                    LootParams lootparams = (new LootParams.Builder((ServerLevel) this.level())).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.TOOL, pStack).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.KILLER_ENTITY, this.getOwner()).withParameter(LootContextParams.THIS_ENTITY, this).withLuck((float) this.luck + player.getLuck()).create(LootContextParamSets.FISHING);
                    //vanilla code---
                    //use lava fishing loot
                    LootTable loottable = this.level().getServer().getLootData().getLootTable(LootTableHandler.FISH);
                    //---vanilla code
                    List<ItemStack> list = loottable.getRandomItems(lootparams);
                    event = new net.minecraftforge.event.entity.player.ItemFishedEvent(list, this.onGround() ? 2 : 1, this);
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                    if (event.isCanceled()) {
                        this.discard();
                        return event.getRodDamage();
                    }
                    CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer) player, pStack, this, list);
                    //vanilla code---
                    for (ItemStack itemstack : list) {
                        ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemstack);
                        double d0 = player.getX() - this.getX();
                        double d1 = player.getY() - this.getY();
                        double d2 = player.getZ() - this.getZ();
                        double d3 = 0.3D;//change to retrieve strength multiplier
                        itementity.setDeltaMovement(d0 * d3, d1 * d3 + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * d3);
                        this.level().addFreshEntity(itementity);
                        player.level().addFreshEntity(new ExperienceOrb(player.level(), player.getX(), player.getY() + 0.5D, player.getZ() + 0.5D, this.random.nextInt(6) + 1));
                        if (itemstack.is(ItemTags.FISHES)) {
                            player.awardStat(Stats.FISH_CAUGHT, 1);
                        }
                    }
                    //---vanilla code
                    i = 1;
                }

                if (this.onGround()) {
                    i = 2;
                }

                this.discard();
                return event == null ? i : event.getRodDamage();
            } else {
                return 0;
            }
            //vanilla code---
        } else {
            return super.retrieve(pStack);
        }
    }

    protected void lavaCatchingFish(BlockPos pPos) {
        Block _BLOCK_STATE_IN_RANDOM_GENERATE_PARTICLE = Blocks.LAVA;
        SimpleParticleType _BUBBLE_PARTICLE = ParticleTypes.SMOKE;
        SimpleParticleType _FISHING_PARTICLE = ParticleTypes.ASH;
        SimpleParticleType _SPLASH_PARTICLE = ParticleTypes.LAVA;

        //---vanilla code
        ServerLevel serverlevel = (ServerLevel) this.level();
        int i = 1;
        BlockPos blockpos = pPos.above();
        if (this.random.nextFloat() < 0.25F && this.level().isRainingAt(blockpos)) {
            ++i;
        }

        if (this.random.nextFloat() < 0.5F && !this.level().canSeeSky(blockpos)) {
            --i;
        }

        if (this.nibble > 0) {
            --this.nibble;
            if (this.nibble <= 0) {
                this.timeUntilLured = 0;
                this.timeUntilHooked = 0;
                this.getEntityData().set(DATA_BITING, false);
            }
        } else if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= i;
            if (this.timeUntilHooked > 0) {
                this.fishAngle += (float) (this.random.nextGaussian() * 4.0D);
                float f = this.fishAngle * ((float) Math.PI / 180F);
                float f1 = Mth.sin(f);
                float f2 = Mth.cos(f);
                double d0 = this.getX() + (double) (f1 * (float) this.timeUntilHooked * 0.1F);
                double d1 = (double) ((float) Mth.floor(this.getY()) + 1.0F);
                double d2 = this.getZ() + (double) (f2 * (float) this.timeUntilHooked * 0.1F);
                BlockState blockstate = serverlevel.getBlockState(new BlockPos((int) d0, (int) (d1 - 1.0D), (int) d2));
                if (serverlevel.getBlockState(new BlockPos((int) d0, (int) d1 - 1, (int) d2)).is(Blocks.LAVA)) {
                    if (this.random.nextFloat() < 0.15F) {
                        serverlevel.sendParticles(_BUBBLE_PARTICLE, d0, d1 - (double) 0.1F, d2, 1, (double) f1, 0.1D, (double) f2, 0.0D);
                    }

                    float f3 = f1 * 0.04F;
                    float f4 = f2 * 0.04F;
                    serverlevel.sendParticles(_FISHING_PARTICLE, d0, d1, d2, 5, (double) f4, 0.01D, (double) (-f3), 1.0D);
                    serverlevel.sendParticles(_FISHING_PARTICLE, d0, d1, d2, 5, (double) (-f4), 0.01D, (double) f3, 1.0D);
                }
            } else {
                this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                double d3 = this.getY() + 0.5D;
                serverlevel.sendParticles(_BUBBLE_PARTICLE, this.getX(), d3, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), (double) this.getBbWidth(), 0.0D, (double) this.getBbWidth(), (double) 0.2F);
                serverlevel.sendParticles(_SPLASH_PARTICLE, this.getX(), d3, this.getZ(), (int) (1.0F + this.getBbWidth() * 50.0F), (double) this.getBbWidth(), 0.0D, (double) this.getBbWidth(), (double) 0.2F);
                this.nibble = Mth.nextInt(this.random, 20, 40);
                this.getEntityData().set(DATA_BITING, true);
            }
        } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= i;
            float f5 = 0.15F;
            if (this.timeUntilLured < 20) {
                f5 += (float) (20 - this.timeUntilLured) * 0.05F;
            } else if (this.timeUntilLured < 40) {
                f5 += (float) (40 - this.timeUntilLured) * 0.02F;
            } else if (this.timeUntilLured < 60) {
                f5 += (float) (60 - this.timeUntilLured) * 0.01F;
            }

            if (this.random.nextFloat() < f5) {
                float f6 = Mth.nextFloat(this.random, 0.0F, 360.0F) * ((float) Math.PI / 180F);
                float f7 = Mth.nextFloat(this.random, 25.0F, 60.0F);
                double d4 = this.getX() + (double) (Mth.sin(f6) * f7) * 0.1D;
                double d5 = (double) ((float) Mth.floor(this.getY()) + 1.0F);
                double d6 = this.getZ() + (double) (Mth.cos(f6) * f7) * 0.1D;
                BlockState blockstate1 = serverlevel.getBlockState(BlockPos.containing(d4, d5 - 1.0D, d6));
                if (blockstate1.is(_BLOCK_STATE_IN_RANDOM_GENERATE_PARTICLE)) {
                    serverlevel.sendParticles(_SPLASH_PARTICLE, d4, d5, d6, 2 + this.random.nextInt(2), (double) 0.1F, 0.0D, (double) 0.1F, 0.0D);
                }
            }

            if (this.timeUntilLured <= 0) {
                this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
                this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
            }
        } else {
            this.timeUntilLured = Mth.nextInt(this.random, 100, 600);
            this.timeUntilLured -= this.lureSpeed * 20 * 5;
        }
        //vanilla code---
    }

    @Override
    protected void pullEntity(Entity pulledEntity) {
        BiFunction<Entity, Vec3, Void> _MULTIPLY_RETRIEVE_STRENGTH_FROM_LAVA = (_pulledEntity, vec3) -> {
            if (_pulledEntity instanceof ItemEntity && _pulledEntity.isInLava()) {
                vec3 = vec3.scale(1D);
            } else {
                vec3 = vec3.scale(0.1D);
            }
            return null;
        };

        //---vanilla code
        Entity angler = this.getOwner();
        if (angler != null) {
            Vec3 vec3 = new Vec3(angler.getX() - this.getX(), angler.getY() - this.getY(), angler.getZ() - this.getZ());
            _MULTIPLY_RETRIEVE_STRENGTH_FROM_LAVA.apply(pulledEntity, vec3);
            pulledEntity.setDeltaMovement(pulledEntity.getDeltaMovement().add(vec3));
        }
        //vanilla code---
    }

    public static EntityType<EntityObsidianHook> BuildEntityType() {
        return EntityType.Builder
                .<EntityObsidianHook>of(EntityObsidianHook::new, MobCategory.MISC)
                .noSave()
                .noSummon()
                .fireImmune()
                .sized(0.25F, 0.25F)
                .setTrackingRange(4)
                .setUpdateInterval(5)
                .build(new ResourceLocation(Reference.MOD_ID, "obsidian_hook").toString());
    }

    @Override
    public EntityType<?> getType() {
        return EntityTypeCollection.ENTITY_OBSIDIAN_HOOK.get();
    }

}
