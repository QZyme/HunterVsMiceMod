package com.huntervsmicemod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Random;

public class PlayerData {
    public enum Role {
        HUNTER, MOUSE
    }

    private static final String ROLE_KEY = "hm_role";
    private static final String DEAD_KEY = "hm_dead";
    private static final String INIT_KEY = "hm_init";

    private Role role = Role.HUNTER;
    private boolean mouseDead = false;
    private boolean initialized = false;

    public static PlayerData get(PlayerEntity player) {
        return ((PlayerDataAccessor) player).huntervsmicemod_template_1_20_1$getPlayerData();
    }

    public static void set(PlayerEntity player, PlayerData data) {
        ((PlayerDataAccessor) player).huntervsmicemod_template_1_20_1$setPlayerData(data);
    }

    public void randomizeRole() {
        this.role  = (new Random().nextBoolean()) ? Role.HUNTER : Role.MOUSE;
    }

    public void swapRole() {
        if (this.role  == Role.HUNTER) {
            this.role  = Role.MOUSE;
            this.mouseDead  = false;
        } else {
            this.role  = Role.HUNTER;
        }
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt(ROLE_KEY,  role.ordinal());
        nbt.putBoolean(DEAD_KEY,  mouseDead);
        nbt.putBoolean(INIT_KEY,  initialized);
    }

    public void readNbt(NbtCompound nbt) {
        if (nbt.contains(ROLE_KEY))  {
            role = Role.values()[nbt.getInt(ROLE_KEY)];
        }
        if (nbt.contains(DEAD_KEY))  {
            mouseDead = nbt.getBoolean(DEAD_KEY);
        }
        if (nbt.contains(INIT_KEY))  {
            initialized = nbt.getBoolean(INIT_KEY);
        }
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role  = role;
    }

    public boolean isMouseDead() {
        return mouseDead;
    }

    public void setMouseDead(boolean mouseDead) {
        this.mouseDead  = mouseDead;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized  = initialized;
    }
}