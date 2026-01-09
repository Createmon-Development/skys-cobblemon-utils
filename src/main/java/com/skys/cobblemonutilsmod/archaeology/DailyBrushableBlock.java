package com.skys.cobblemonutilsmod.archaeology;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class DailyBrushableBlock extends Block implements EntityBlock {
    public static final IntegerProperty DUSTED = BlockStateProperties.DUSTED;

    private final Block turnsInto;
    private final SoundEvent brushSound;
    private final SoundEvent brushCompletedSound;

    public DailyBrushableBlock(Block turnsInto, SoundEvent brushSound, SoundEvent brushCompletedSound, Properties properties) {
        super(properties);
        this.turnsInto = turnsInto;
        this.brushSound = brushSound;
        this.brushCompletedSound = brushCompletedSound;
        this.registerDefaultState(this.stateDefinition.any().setValue(DUSTED, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DUSTED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 2);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, this, 2);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DailyBrushableBlockEntity brushable) {
            brushable.checkReset();
        }

        // Handle falling behavior like suspicious sand/gravel
        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, pos, state);
            fallingBlock.disableDrop();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DailyBrushableBlockEntity(pos, state);
    }

    public Block getTurnsInto() {
        return turnsInto;
    }

    public SoundEvent getBrushSound() {
        return brushSound;
    }

    public SoundEvent getBrushCompletedSound() {
        return brushCompletedSound;
    }
}
