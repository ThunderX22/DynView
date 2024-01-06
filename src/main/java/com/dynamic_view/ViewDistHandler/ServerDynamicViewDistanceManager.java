package com.dynamic_view.ViewDistHandler;

import javax.lang.model.util.ElementScanner14;

import com.dynamic_view.DynView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ServerDynamicViewDistanceManager implements IDynamicViewDistanceManager
{
    private static final int                              UPDATE_LEEWAY = 3;
    private static       ServerDynamicViewDistanceManager instance;
    public static        int                              minChunkViewDist;
    public static        int                              maxChunkViewDist;
    public static        int                              minChunkUpdateDist;
    public static        int                              maxChunkUpdateDist;
    public static        double                           meanTickToStayBelow;
    public static        int                              meanChunkViewDist;
    public static        int                              meanChunkUpdateDist;

    private int currentChunkViewDist   = 0;
    private int currentChunkUpdateDist = 0;

    private ServerDynamicViewDistanceManager()
    {
    }

    public static IDynamicViewDistanceManager getInstance()
    {
        if (instance == null)
        {
            instance = new ServerDynamicViewDistanceManager();
        }
        return instance;
    }

    @Override
    public void initViewDist()
    {
        // Set starting distances equal to minimum specified in the config.
        currentChunkViewDist = minChunkViewDist;
        currentChunkUpdateDist = minChunkUpdateDist;

        // Set mean values.
        meanChunkViewDist = (minChunkViewDist + maxChunkViewDist) / 2;
        meanChunkUpdateDist = (minChunkUpdateDist + maxChunkUpdateDist) / 2;

        ServerLifecycleHooks.getCurrentServer().getPlayerList().setViewDistance(minChunkViewDist);
        if (DynView.getConfig().getCommonConfig().adjustSimulationDistance.get())
        {
            ServerLifecycleHooks.getCurrentServer().getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(currentChunkUpdateDist));
        }
    }

    @Override
    public void updateViewDistForMeanTick(final int meanTickTime)
    {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server.getPlayerList().getPlayers().isEmpty())
        {
            return;
        }

        if (meanTickTime - UPDATE_LEEWAY > meanTickToStayBelow)
        { // Server tick time is greater than specified in config, lower distances.
            if (currentChunkUpdateDist > meanChunkUpdateDist)
            { // Simulation distance is above mean, lower this first because it has a larger performance impact than view distance.
                currentChunkUpdateDist--;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms decreasing simulation distance to: " + currentChunkUpdateDist);
                }
                server.getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(currentChunkUpdateDist));
                return;
            }

            if(currentChunkViewDist > meanChunkViewDist)
            { // View distance is above mean, try lowering this next.
                currentChunkViewDist--;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms decreasing chunk view distance to: " + currentChunkViewDist);
                }
                server.getPlayerList().setViewDistance(currentChunkViewDist);
                return;
            }

            if(currentChunkUpdateDist > minChunkUpdateDist)
            { // Simulation distance is above minimum, lower this until minimum value has been reached.
                currentChunkUpdateDist--;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms decreasing simulation distance to: " + currentChunkUpdateDist);
                }
                server.getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(currentChunkUpdateDist));
                return;
            }

            if(currentChunkViewDist > minChunkViewDist)
            { // All else has failed, lower the view distance until the minimum value has been reached.
                currentChunkViewDist--;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms decreasing chunk view distance to: " + currentChunkViewDist);
                }
                server.getPlayerList().setViewDistance(currentChunkViewDist);
                return;
            }
        }

        if (meanTickTime + UPDATE_LEEWAY < meanTickToStayBelow)
        { // Server tick time is less than specified in config, raise distances.
            if(currentChunkViewDist < meanChunkViewDist)
            { // Raise view distance first because it will be felt more than simulation distance, and has a smaller performance impact than simulation distance.
                currentChunkViewDist++;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms increasing chunk view distance to: " + currentChunkViewDist);
                }
                server.getPlayerList().setViewDistance(currentChunkViewDist);
                return;
            }

            if(currentChunkUpdateDist < meanChunkUpdateDist)
            { // Raise simulation distance until at or above mean.
                currentChunkUpdateDist++;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms increasing simulation distance to: " + currentChunkUpdateDist);
                }
                server.getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(currentChunkUpdateDist));
                return;
            }

            if(currentChunkViewDist < maxChunkViewDist)
            { // Raise view distance until maximum value has been reached.
                currentChunkViewDist++;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms increasing chunk view distance to: " + currentChunkViewDist);
                }
                server.getPlayerList().setViewDistance(currentChunkViewDist);
                return;
            }

            if(currentChunkUpdateDist < maxChunkUpdateDist)
            { // Raise simulation distance until maximum value has been reached.
                currentChunkUpdateDist++;
                if (DynView.getConfig().getCommonConfig().logMessages.get())
                {
                    DynView.LOGGER.info("Mean tick: " + meanTickTime + "ms increasing simulation distance to: " + currentChunkUpdateDist);
                }
                server.getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(currentChunkUpdateDist));
                return;
            }
        }
    }

    @Override
    public void setCurrentChunkViewDist(final int currentChunkViewDist)
    {
        this.currentChunkViewDist = Mth.clamp(currentChunkViewDist, 0, 200);
        ServerLifecycleHooks.getCurrentServer().getPlayerList().setViewDistance(this.currentChunkViewDist);
    }

    @Override
    public void setCurrentChunkUpdateDist(final int currentChunkUpdateDist)
    {
        this.currentChunkUpdateDist = Mth.clamp(currentChunkUpdateDist, 0, 200);
        ServerLifecycleHooks.getCurrentServer().getAllLevels().forEach(level -> level.getChunkSource().setSimulationDistance(this.currentChunkUpdateDist));
    }
}
