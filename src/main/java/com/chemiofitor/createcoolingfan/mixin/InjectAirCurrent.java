package com.chemiofitor.createcoolingfan.mixin;

import com.chemiofitor.createcoolingfan.api.IFanProcessingTarget;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 对Create模组的AirCurrent类进行Mixin
 * 用于扩展风扇气流对自定义方块实体的处理逻辑
 * <p>
 * 气流范围内的目标位置会被缓存，而不是每tick逐格调用getBlockEntity——
 * 长气流下那是每tick最多上百次方块实体查询。缓存在气流重建时失效，
 * 另外每{@link #CCF$SCAN_INTERVAL}tick重扫一次，用来发现后来放下的方块：
 * Create自己的重建只在服务端定期触发，客户端不会。
 * <p>
 * 处理在两侧同时运行，公式与转速一致，客户端的渲染进度才能跟上服务端。
 */
@Mixin(value = AirCurrent.class, remap = false)
public abstract class InjectAirCurrent {

    /** 重扫气流范围的间隔（tick） */
    @Unique
    private static final int CCF$SCAN_INTERVAL = 20;

    @Shadow
    @Final
    public IAirCurrentSource source;

    @Shadow
    public Direction direction;

    @Shadow
    public float maxDistance;

    @Shadow
    public abstract FanProcessingType getTypeAt(float offset);

    /** 气流范围内的目标：位置 + 该处的处理类型（null表示该段无处理类型） */
    @Unique
    private final List<Pair<BlockPos, FanProcessingType>> ccf$targets = new ArrayList<>();

    @Unique
    private int ccf$scanCooldown;

    @Inject(method = "findAffectedHandlers", at = @At("TAIL"))
    private void ccf$invalidateTargets(CallbackInfo ci) {
        // 气流形状或处理段变了，缓存的类型也跟着失效
        ccf$scanCooldown = 0;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/fan/AirCurrent;tickAffectedHandlers()V")
    )
    private void ccf$tickTargets(CallbackInfo ci) {
        Level world = source.getAirCurrentWorld();
        if (world == null)
            return;

        if (--ccf$scanCooldown <= 0) {
            ccf$scanCooldown = CCF$SCAN_INTERVAL;
            ccf$scanTargets(world);
        }

        if (ccf$targets.isEmpty())
            return;

        float speed = source.getSpeed();

        for (Pair<BlockPos, FanProcessingType> entry : ccf$targets) {
            BlockPos pos = entry.getLeft();
            if (!world.isLoaded(pos))
                continue;

            // 方块可能已被替换，每次都要重新确认类型
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof IFanProcessingTarget target))
                continue;

            FanProcessingType type = entry.getRight();
            if (target.ccf$canProcess(type))
                target.ccf$process(type, speed);
        }
    }

    @Unique
    private void ccf$scanTargets(Level world) {
        ccf$targets.clear();

        if (direction == null || maxDistance <= 0)
            return;

        BlockPos start = source.getAirCurrentPos();
        int limit = Mth.ceil(maxDistance);

        for (int i = 1; i <= limit; i++) {
            BlockPos pos = start.relative(direction, i);
            // 不因为查询方块实体而强制加载区块
            if (!world.isLoaded(pos))
                continue;
            if (world.getBlockEntity(pos) instanceof IFanProcessingTarget)
                ccf$targets.add(Pair.of(pos, getTypeAt(i - 1)));
        }
    }
}
