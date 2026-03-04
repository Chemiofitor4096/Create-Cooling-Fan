package com.chemiofitor.createcoolingfan.mixin;

import com.chemiofitor.createcoolingfan.api.IFanProcessingTarget;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;

@Mixin(CastingBlockEntity.class)
public class CastingContainerProcessing implements IFanProcessingTarget {
    @Shadow(remap = false)
    private int timer;

    @Shadow(remap = false)
    private int coolingTime = -1;

    @Shadow(remap = false)
    private ICastingRecipe currentRecipe;

    @Unique
    private double ccf$remainder;

    @Unique
    private void ccf$process(int times) {
        if (timer + times < 0) {
            timer = 0;
        } else if (timer + times > coolingTime) {
            timer = coolingTime;
        } else if (timer > 0) {
            timer += times;
        }
    }

    @Override
    public boolean ccf$canProcess(FanProcessingType processingType) {
        return currentRecipe != null;
    }

    @Override
    public void ccf$process(FanProcessingType processingType, float speed) {
        double factor = 1.0F;
        double tick = Math.sqrt(Math.abs(speed) / 64.0f) * factor;

        int sign = tick < 0 ? -1 : 1;

        tick = Math.abs(tick);

        int integer = (int) tick; // 整数部分
        double remainer = tick - integer; // 小数部分

        ccf$remainder += remainer; // 累加小数部分

        if (ccf$remainder > 1) { // 累计进度超过1，额外处理1次
            ccf$remainder--;
            integer++;
        }

        ccf$process(sign * integer);
    }
}
