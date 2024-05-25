package gecko10000.advancementrenderer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
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
            ClientAdvancementManager manager = player.networkHandler.getAdvancementHandler();
            for (PlacedAdvancement pa : manager.getManager().getAdvancements()) {
                String id = pa.getAdvancementEntry().id().getPath();
                logger.info(id);
                AdvancementDisplay display = pa.getAdvancement().display().orElse(null);
                if (display == null) continue;
                ItemStack icon = display.getIcon();
                int size = getInteger(context, "size");
                renderToImageFile(id, icon, size);
            }
            return 1;
        })))));
    }

    // from https://github.com/TechReborn/RebornCore/blob/1.16/src/main/java/reborncore/client/ItemStackRenderer.java
    private void renderToImageFile(String id, ItemStack icon, int size) {
        File dir = FabricLoader.getInstance().getGameDir().resolve(IMAGE_DIR).toFile();
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, id + ".png");
        if (file.exists()) file.delete();

        MinecraftClient minecraft = MinecraftClient.getInstance();

        if (minecraft.getItemRenderer() == null || minecraft.world == null) return;

        final Framebuffer framebuffer = new SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);
    }
}
