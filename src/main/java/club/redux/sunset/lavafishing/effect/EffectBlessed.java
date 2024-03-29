package club.redux.sunset.lavafishing.effect;

import club.redux.sunset.lavafishing.util.RegistryCollection.MobEffectCollection;
import club.redux.sunset.lavafishing.util.RegistryCollection.ParticleTypeCollection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EffectBlessed extends MobEffect {
    public EffectBlessed() {
        super(MobEffectCategory.NEUTRAL, 0xCC3300);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        pLivingEntity.setRemainingFireTicks(20);
        pLivingEntity.heal(0.4f);
        pLivingEntity.setSharedFlagOnFire(true);
        if (pLivingEntity.isInWaterOrRain()) {
            pLivingEntity.hurt(pLivingEntity.damageSources().onFire(), 0.2f);

        }
        pLivingEntity.hurt(pLivingEntity.damageSources().onFire(), 0.1f);
        super.applyEffectTick(pLivingEntity, pAmplifier);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    public static void onEntityDamaged(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        if (source.getEntity() instanceof LivingEntity sourceEntity) {
            if (!source.is(DamageTypes.MOB_PROJECTILE) && !source.is(DamageTypes.MAGIC) && sourceEntity.getEffect(MobEffectCollection.EFFECT_BLESSED.get()) != null && sourceEntity.getMainHandItem().is(Items.AIR)) {
                target.addEffect(new MobEffectInstance(MobEffectCollection.EFFECT_ENDLESS_FLAME.get(), 1200), sourceEntity);
                spawnHitParticle((ServerLevel) target.level(), target.position());
            }
        }
    }

    private static void spawnHitParticle(ServerLevel level, Vec3 pos) {
//        level.addParticle(ParticleTypeCollection.PARTICLE_FIRE_PUNCH,
//                pos.x(), pos.y(), pos.z(),
//                0, 0, 0);
        level.sendParticles(ParticleTypeCollection.PARTICLE_FIRE_PUNCH.get(), pos.x(), pos.y() + 1.3f, pos.z(),
                1, 0.3, 0.3, 0.3, 1);
    }
}
