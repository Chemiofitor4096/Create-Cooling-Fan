package com.chemiofitor.createcoolingfan.mixin;

import com.chemiofitor.createcoolingfan.api.IFanProcessingTarget;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 对Create模组的AirCurrent类进行Mixin
 * 用于扩展风扇气流对自定义方块实体的处理逻辑
 */
@Mixin(value = AirCurrent.class, remap = false)
public abstract class InjectAirCurrent {

    @Shadow
    @Final
    public IAirCurrentSource source;

    @Shadow
    public Direction direction;

    @Shadow
    public float maxDistance;

    @Shadow
    public abstract FanProcessingType getTypeAt(float offset);

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/fan/AirCurrent;tickAffectedHandlers()V")
    )
    public void tickMixin(CallbackInfo ci) {
        Level world = source.getAirCurrentWorld();
        BlockPos start = source.getAirCurrentPos();
        float speed = source.getSpeed();
        // getLimit() is private in Create 6.0.8, compute it directly from maxDistance
        int limit = (float) (int) maxDistance == maxDistance ? (int) maxDistance : (int) maxDistance + 1;
        if (world != null) {
            for (int i = 1; i <= limit; i++) {
                FanProcessingType type = getTypeAt(i - 1);
                BlockPos pos = start.relative(direction, i);
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof IFanProcessingTarget target)
                    if (target.ccf$canProcess(type))
                        target.ccf$process(type, speed);
            }
        }
    }
}
