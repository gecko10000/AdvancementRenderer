package gecko10000.advancementrenderer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.LoggerFactory;

public class AdvancementRenderer implements ModInitializer {
    @Override
    public void onInitialize() {
        LoggerFactory.getLogger(this.getClass()).info("Hello world");
    }
}
