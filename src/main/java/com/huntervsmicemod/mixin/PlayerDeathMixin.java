package com.huntervsmicemod.mixin;

import com.huntervsmicemod.PlayerData;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;  // 关键导入
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;  // 添加GameMode导入
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Random;

import static com.huntervsmicemod.HunterVsMiceMod.getConfig;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)

    public void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerData data = PlayerData.get(player);

        if (data.getRole()  == PlayerData.Role.HUNTER) {
            ci.cancel();
            BlockPos deathPos = player.getBlockPos();
            World world = player.getWorld();
            Random random = (Random) world.getRandom();

            player.sendMessage(Text.translatable("huntervsmicemod.message.respawning"),  false);

            for (int i = 0; i < 20; i++) {
                int x = deathPos.getX()  + random.nextInt(getConfig().respawnRadius  * 2) - getConfig().respawnRadius;
                int z = deathPos.getZ()  + random.nextInt(getConfig().respawnRadius  * 2) - getConfig().respawnRadius;
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING,  x, z);

                BlockPos respawnPos = new BlockPos(x, y, z);
                BlockPos belowPos = respawnPos.down();

                if (world.getBlockState(belowPos).isSolidBlock(world,  belowPos)) {
                    teleportPlayer(player, (ServerWorld) world, respawnPos);
                    return;
                }
            }

            BlockPos respawnPos = deathPos.up();
            teleportPlayer(player, (ServerWorld) world, respawnPos);

        } else {
            data.setMouseDead(true);
            player.changeGameMode(GameMode.SPECTATOR);
            player.sendMessage(Text.translatable("huntervsmicemod.message.mouse_dead"),  true);
        }
    }

    @Unique
    private void teleportPlayer(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        player.teleport(world,
                pos.getX()  + 0.5,
                pos.getY(),
                pos.getZ()  + 0.5,
                player.getYaw(),
                player.getPitch());

        player.setHealth(player.getMaxHealth()  * 0.5f);
        player.getHungerManager().setFoodLevel(10);

        player.addStatusEffect(new  StatusEffectInstance(
                StatusEffects.RESISTANCE,
                200,
                5,
                false,
                false,
                true
        ));
    }
}