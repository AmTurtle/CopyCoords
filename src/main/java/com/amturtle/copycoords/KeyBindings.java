package com.amturtle.copycoords;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Mod.EventBusSubscriber(modid = Copycoords.MODID, value = Dist.CLIENT)
public class KeyBindings {

    private static final String CATEGORY = "key.categories." + Copycoords.MODID;

    public static final KeyMapping COPY_BLOCK_COORDS = new KeyMapping(
            "key." + Copycoords.MODID + ".copy_block_coords",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B, // B key
            CATEGORY
    );

    public static final KeyMapping COPY_PLAYER_COORDS = new KeyMapping(
            "key." + Copycoords.MODID + ".copy_player_coords",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C, // C key
            CATEGORY
    );

    @Mod.EventBusSubscriber(modid = Copycoords.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class KeyMappingRegister {
        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            event.register(COPY_BLOCK_COORDS);
            event.register(COPY_PLAYER_COORDS);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        if (COPY_BLOCK_COORDS.consumeClick()) {
            copyBlockCoordinates(minecraft);
        }

        if (COPY_PLAYER_COORDS.consumeClick()) {
            copyPlayerCoordinates(minecraft);
        }
    }

    private static void copyBlockCoordinates(Minecraft minecraft) {
        Player player = minecraft.player;
        // Get block that player is looking at
        HitResult hitResult = getBlockPlayerIsLookingAt(minecraft, player);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hitResult).getBlockPos();
            String coords = String.format("%d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ());

            // Copy to clipboard
            Minecraft.getInstance().keyboardHandler.setClipboard(coords);

            // Notify player
            player.displayClientMessage(Component.literal("Block coordinates copied to clipboard: " + coords), true);
        } else {
            player.displayClientMessage(Component.literal("No block in sight!"), true);
        }
    }

    private static void copyPlayerCoordinates(Minecraft minecraft) {
        Player player = minecraft.player;

        // For negative coordinates, we need to handle them differently than just using Math.floor
        int x = (int)Math.floor(player.getX());
        int y = (int)Math.floor(player.getY());
        int z = (int)Math.floor(player.getZ());

        // Minecraft actually displays block coordinates this way
        if (player.getX() < 0 && player.getX() != x) x++; // For negative values with decimals
        if (player.getZ() < 0 && player.getZ() != z) z++; // For negative values with decimals

        String coords = String.format("%d %d %d", x, y, z);

        // Copy to clipboard
        Minecraft.getInstance().keyboardHandler.setClipboard(coords);

        // Notify player
        player.displayClientMessage(Component.literal("Player coordinates copied to clipboard: " + coords), true);
    }

    private static HitResult getBlockPlayerIsLookingAt(Minecraft minecraft, Player player) {
        float partialTicks = minecraft.getPartialTick();
        double reach = minecraft.gameMode.getPickRange();

        Vec3 eyePosition = player.getEyePosition(partialTicks);
        Vec3 viewVector = player.getViewVector(partialTicks);
        Vec3 reachVector = eyePosition.add(viewVector.x * reach, viewVector.y * reach, viewVector.z * reach);

        return minecraft.level.clip(new ClipContext(
                eyePosition,
                reachVector,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
    }
}