package com.sunset.item.fishes;

import com.sunset.creativetab.TabLavaFishing;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import static com.sunset.util.RegistryCollections.EffectCollection.EFFECT_BLESSED;

public class ItemAgniFish extends Item
{
    public ItemAgniFish() {
        super(GetProperties());
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack pStack, @NotNull Level pLevel, LivingEntity pLivingEntity) {
        pLivingEntity.addEffect(new MobEffectInstance(EFFECT_BLESSED, 2400));
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    public static Properties GetProperties() {
        FoodProperties foodProperties = new FoodProperties.Builder()
                .nutrition(1)
                .saturationMod(0.5F)
                .build();
        return new Properties()
                .tab(TabLavaFishing.TAB_LAVA_FISHING)
                .food(foodProperties)
                .fireResistant();
    }
}
