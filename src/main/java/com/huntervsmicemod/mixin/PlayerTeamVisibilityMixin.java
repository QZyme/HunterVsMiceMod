package com.huntervsmicemod.mixin;

import com.huntervsmicemod.PlayerData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerTeamVisibilityMixin extends LivingEntity {
    protected PlayerTeamVisibilityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        if (!this.getWorld().isClient)  {
            PlayerEntity target = (PlayerEntity) (Object) this;
            PlayerEntity viewer = getWorld().getClosestPlayer(this,  64);

            if (viewer != null) {
                PlayerData targetData = PlayerData.get(target);
                PlayerData viewerData = PlayerData.get(viewer);

                if (targetData.getRole()  != viewerData.getRole())  {
                    cir.setReturnValue(Text.empty());
                }
            }
        }
    }
}