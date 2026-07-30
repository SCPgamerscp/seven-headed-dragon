package com.sevenheadeddragon.client.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sevenheadeddragon.entity.dragon.TimedGimmickCreeperEntity;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renders the 10秒時限爆発クリーパー (Timed Gimmick Creeper).
 * <p>
 * Reuses vanilla's creeper model and texture so the player instantly reads it
 * as "a creeper that is about to blow up", but drives the classic white
 * swell-flash off this entity's own 10-second fuse instead of vanilla's
 * 30-tick one. The flash accelerates as the fuse runs out, giving a clear
 * visual countdown for the "10秒以内に7体すべてを倒す" challenge.
 */
public class TimedGimmickCreeperRenderer
        extends MobRenderer<TimedGimmickCreeperEntity, CreeperModel<TimedGimmickCreeperEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/entity/creeper/creeper.png");

    public TimedGimmickCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(TimedGimmickCreeperEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(TimedGimmickCreeperEntity entity, PoseStack poseStack, float partialTick) {
        // Swell up towards detonation, exactly like a primed vanilla creeper.
        float progress = entity.getFuseProgress();
        float flash = Mth.clamp(progress, 0.0F, 1.0F);
        float swell = 1.0F + Mth.sin(flash * 100.0F) * flash * 0.01F;
        float widen = flash * flash;
        widen = widen * widen;
        float x = (1.0F + widen * 0.4F) * swell;
        float y = (1.0F + widen * 0.1F) / swell;
        poseStack.scale(x, y, x);
    }

    @Override
    protected float getWhiteOverlayProgress(TimedGimmickCreeperEntity entity, float partialTick) {
        float progress = entity.getFuseProgress();
        // Blink faster and faster as the 10 second fuse burns down.
        float blinkRate = 4.0F + progress * 26.0F;
        boolean lit = ((int) (progress * blinkRate)) % 2 != 0;
        return lit ? Mth.clamp(progress, 0.4F, 1.0F) : 0.0F;
    }
}
