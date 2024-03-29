package club.redux.sunset.lavafishing.block.BlockEntity;

import club.redux.sunset.lavafishing.util.RegistryCollection.BlockEntityTypeCollection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockEntityPrometheusBounty extends ChestBlockEntity {
    public BlockEntityPrometheusBounty(BlockPos pos, BlockState state) {
        super(BlockEntityTypeCollection.BLOCK_ENTITY_PROMETHEUS_BOUNTY.get(), pos, state);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }
}
