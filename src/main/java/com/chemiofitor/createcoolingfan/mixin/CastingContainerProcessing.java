package com.chemiofitor.createcoolingfan.mixin;

import com.chemiofitor.createcoolingfan.api.IFanProcessingTarget;
import com.chemiofitor.createcoolingfan.config.CCFConfig;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

/**
 * 让铸造台/铸造盆响应Create风扇气流，加快或减慢冷却进度
 */
@Mixin(value = CastingBlockEntity.class, remap = false)
public abstract class CastingContainerProcessing implements IFanProcessingTarget {
    @Shadow
    private int timer;

    @Shadow
    private int coolingTime;

    @Shadow
    private ICastingRecipe currentRecipe;

    /** 未满一tick的进度余量，累计到1后补一次 */
    @Unique
    private double ccf$remainder;

    @Inject(method = "reset", at = @At("TAIL"))
    private void ccf$clearRemainder(CallbackInfo ci) {
        ccf$remainder = 0;
    }

    @Override
    public boolean ccf$canProcess(FanProcessingType processingType) {
        // coolingTime为-1表示液体未注满，此时timer无意义，不参与处理
        return currentRecipe != null && coolingTime > 0;
    }

    @Override
    public void ccf$process(FanProcessingType processingType, float speed) {
        if (speed == 0)
            return;

        double factor = CCFConfig.getFactor(processingType);
        if (factor == 0)
            return;

        // 强度随转速开方增长；吸风（负转速）方向相反
        double delta = Math.sqrt(Math.abs(speed) / 64.0) * factor * Math.signum(speed);

        int whole = (int) delta;
        ccf$remainder += delta - whole;

        // 余量可能为正也可能为负，两个方向都要结算
        if (ccf$remainder >= 1) {
            ccf$remainder -= 1;
            whole++;
        } else if (ccf$remainder <= -1) {
            ccf$remainder += 1;
            whole--;
        }

        if (whole == 0)
            return;

        // 上限取coolingTime：TiC的serverTick会在下一tick自增并完成配方
        timer = Mth.clamp(timer + whole, 0, coolingTime);
    }
}
