package gecko10000.advancementrenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class AdvancementRenderer implements ModInitializer {

    private static final String IMAGE_DIR = "renders";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("renderadvancements").then(ClientCommandManager.argument("size", integer()).executes(context -> {
            ClientPlayerEntity player = context.getSource().getPlayer();
            if (player == null) return 0;
            render(player, getInteger(context, "size"));
            return 1;
        })))));
    }

    private void render(ClientPlayerEntity player, int size) {
        ClientAdvancementManager manager = player.networkHandler.getAdvancementHandler();
        for (PlacedAdvancement pa : manager.getManager().getAdvancements()) {
            String id = pa.getAdvancementEntry().id().getPath();
            logger.info(id);
            AdvancementDisplay display = pa.getAdvancement().display().orElse(null);
            if (display == null) continue;
            ItemStack icon = display.getIcon();
            renderToImageFile(id, icon, size);
        }
    }

    // from https://github.com/TechReborn/TechReborn/blob/7cfc8fb513bc34ddf27cfb7a0549ad3420b4623b/RebornCore/src/client/java/reborncore/client/ItemStackRenderer.java
    private void renderToImageFile(String id, ItemStack item, int size) {
        File dir = FabricLoader.getInstance().getGameDir().resolve(IMAGE_DIR).toFile();
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, id + ".png");
        if (file.exists()) file.delete();
        MinecraftClient client = MinecraftClient.getInstance();

        Matrix4f matrix4f = new Matrix4f().setOrtho(0, 16, 16, 0, 1000, 3000);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorter.BY_Z);
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.identity();
        stack.translate(0, 0, -2000);
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.applyModelViewMatrix();

        Framebuffer framebuffer = new SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        try (NativeImage nativeImage = new NativeImage(size, size, true)) {
            framebuffer.setClearColor(0, 0, 0, 0);
            framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

            {
                framebuffer.beginWrite(true);
                DrawContext drawContext = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());
                drawContext.drawItem(item, 0, 0);
                drawContext.draw();
                framebuffer.endWrite();
            }

            {
                framebuffer.beginRead();
                nativeImage.loadFromTextureImage(0, false);
                nativeImage.mirrorVertically();
                framebuffer.endRead();
            }

            try {
                nativeImage.writeTo(file.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        framebuffer.delete();
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
