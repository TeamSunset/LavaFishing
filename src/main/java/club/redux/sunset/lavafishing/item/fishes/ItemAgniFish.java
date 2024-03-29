package club.redux.sunset.lavafishing.item.fishes;

import club.redux.sunset.lavafishing.util.RegistryCollection.MobEffectCollection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemAgniFish extends Item {
    public ItemAgniFish() {
        super(GetProperties());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        pLivingEntity.addEffect(new MobEffectInstance(MobEffectCollection.EFFECT_BLESSED.get(), 2400));
        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    public static Properties GetProperties() {
        FoodProperties foodProperties = new FoodProperties.Builder()
                .nutrition(1)
                .saturationMod(0.5F)
                .build();
        return new Properties()
                .food(foodProperties)
                .fireResistant();
    }
}
