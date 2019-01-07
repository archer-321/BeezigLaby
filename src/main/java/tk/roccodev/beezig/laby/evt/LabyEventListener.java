package tk.roccodev.beezig.laby.evt;

import eu.the5zig.mod.The5zigAPI;
import eu.the5zig.mod.event.ChatEvent;
import eu.the5zig.mod.event.ChatSendEvent;
import eu.the5zig.mod.server.AbstractGameListener;
import eu.the5zig.mod.server.GameListenerRegistry;
import eu.the5zig.mod.server.GameMode;
import net.labymod.api.EventManager;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.api.events.PluginMessageEvent;
import tk.roccodev.beezig.laby.LabyMain;

/**
 * Created by Rocco on 05/01/2019.
 */
public class LabyEventListener {

    public static void init() {
        EventManager mgr = LabyMain.LABY.getEventManager();

        mgr.register((s, s1) -> {
            boolean bool = false;
            boolean apply = true;
            if(The5zigAPI.getAPI().getActiveServer() != null) {
                for (AbstractGameListener list : GameListenerRegistry.gameListeners) {
                    GameMode gm = The5zigAPI.getAPI().getActiveServer().getGameListener().getCurrentGameMode();
                    try {
                        boolean result = list.onServerChat(gm, s.replace("§r", ""));
                        if (apply && result) {
                            bool = result;
                            apply = false;
                        }
                    }
                    catch(Exception ignored) {}
                }
                The5zigAPI.getAPI().getActiveServer().getGameListener().match(s1);
                return bool || The5zigAPI.getAPI().getPluginManager().fireEvent(new ChatEvent(s.replace("§r", ""), s1)).isCancelled();
            }
            else return false;
        });

        mgr.register((MessageSendEvent) s -> The5zigAPI.getAPI().getPluginManager().fireEvent(new ChatSendEvent(s)).isCancelled());

        mgr.registerOnJoin(serverData -> {

        });

        mgr.registerOnQuit(serverData -> {

        });

        mgr.register((PluginMessageEvent) (s, packetBuffer) -> {
            if(s.equals("MC|Brand")) { // Switched servers
                if (The5zigAPI.getAPI().getActiveServer() != null) {
                    for (AbstractGameListener list : GameListenerRegistry.gameListeners) {
                        GameMode gm = The5zigAPI.getAPI().getActiveServer().getGameListener().getCurrentGameMode();
                        try {
                            list.onServerConnect(gm);
                        } catch (Exception ignored) {}
                    }
                }
            }
        });

    }
}