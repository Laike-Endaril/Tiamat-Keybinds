package com.fantasticsource.tiamatgamesettings;

import com.fantasticsource.mctools.MCTools;
import com.fantasticsource.mctools.event.GametypeChangedEvent;
import com.fantasticsource.tools.ReflectionTool;
import com.fantasticsource.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import static com.fantasticsource.tiamatgamesettings.TiamatGameSettings.MODID;

public class OptionsHandler
{
    public static final GameSettings DEFAULT_GAME_SETTINGS = Minecraft.getMinecraft().gameSettings;
    public static final File DEFAULT_OPTIONS_FILE = new File(MCTools.getConfigDir() + ".." + File.separator + "options.txt");

    protected static final Field
            SOUND_HANDLER_SND_MANAGER_FIELD = ReflectionTool.getField(SoundHandler.class, "field_147694_f", "sndManager"),
            SOUND_MANAGER_OPTIONS_FIELD = ReflectionTool.getField(SoundManager.class, "field_148619_d", "options");

    @SubscribeEvent
    public static void gametypeChanged(GametypeChangedEvent event)
    {
        new GameSettingData(event.newGameType).apply();
    }

    public static class GameSettingData
    {
        public final GameType gameType;
        public final GameSettings gameSettings;

        public GameSettingData(GameType gameType)
        {
            Minecraft mc = Minecraft.getMinecraft();
            this.gameType = gameType;
            File dir = new File(MCTools.getConfigDir() + MODID + File.separator + gameType.getName());
            dir.mkdirs();
            File file = new File(dir, "options.txt");
            if (!file.exists())
            {
                if (!DEFAULT_OPTIONS_FILE.exists()) DEFAULT_GAME_SETTINGS.saveOptions();

                try
                {
                    Tools.copyFile(DEFAULT_OPTIONS_FILE, file);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            gameSettings = new GameSettings(mc, dir);
            ArrayList<KeyBinding> keyBindings = new ArrayList<>(Arrays.asList(gameSettings.keyBindings));
            for (KeyBinding keyBinding : DEFAULT_GAME_SETTINGS.keyBindings)
            {
                if (!containsKeybind(gameSettings.keyBindings, keyBinding))
                {
                    keyBindings.add(keyBinding);
                }
            }
            gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
            gameSettings.loadOptions();
        }

        public void apply()
        {
            Minecraft mc = Minecraft.getMinecraft();

            mc.gameSettings = gameSettings;

            mc.getRenderManager().options = gameSettings;

            SoundManager soundManager = (SoundManager) ReflectionTool.get(SOUND_HANDLER_SND_MANAGER_FIELD, mc.getSoundHandler());
            ReflectionTool.set(SOUND_MANAGER_OPTIONS_FIELD, soundManager, gameSettings);
            soundManager.reloadSoundSystem();


            EntityPlayerSP player = mc.player;
            if (player != null) player.movementInput = new MovementInputFromOptions(gameSettings);
        }

        protected boolean containsKeybind(KeyBinding[] keyBindings, KeyBinding keyBinding)
        {
            String desc = keyBinding.getKeyDescription();
            for (KeyBinding kb : keyBindings)
            {
                if (desc.equals(kb.getKeyDescription())) return true;
            }
            return false;
        }
    }
}
