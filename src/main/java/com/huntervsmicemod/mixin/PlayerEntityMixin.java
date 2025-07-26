package com.huntervsmicemod.mixin;

import com.huntervsmicemod.PlayerData;
import com.huntervsmicemod.PlayerDataAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerDataAccessor {
	@Unique
	private PlayerData huntervsmicemod_template_1_20_1$playerData = new PlayerData();

	@Override
	public PlayerData huntervsmicemod_template_1_20_1$getPlayerData() {
		return this.huntervsmicemod_template_1_20_1$playerData;
	}

	@Override
	public void huntervsmicemod_template_1_20_1$setPlayerData(PlayerData data) {
		this.huntervsmicemod_template_1_20_1$playerData  = data;
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
		NbtCompound data = new NbtCompound();
		this.huntervsmicemod_template_1_20_1$playerData.writeNbt(data);
		nbt.put("HunterMouseData",  data);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("HunterMouseData",  10)) {
			this.huntervsmicemod_template_1_20_1$playerData.readNbt(nbt.getCompound("HunterMouseData"));
		}
	}
}