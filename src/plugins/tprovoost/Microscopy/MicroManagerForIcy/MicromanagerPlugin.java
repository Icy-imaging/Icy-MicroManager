package plugins.tprovoost.Microscopy.MicroManagerForIcy;

import icy.plugin.abstract_.PluginActionable;
import icy.plugin.interface_.PluginThreaded;
import mmcorej.CMMCore;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;

public final class MicromanagerPlugin extends PluginActionable implements PluginThreaded
{
    /**
     * Initialize Micro-Manager sub system.
     * This method has to be outside 'MicroManager' class to load JAR files before calling any methods from MicroManager
     * class.
     */
    public static void init()
    {
        // verify that system libraries are loaded
        if (!MMUtils.isSystemLibrairiesLoaded())
        {
            // load micro manager libraries
            if (!MMUtils.fixSystemLibrairies())
                return;
        }

        MicroManager.init();
    }

    @Override
    public final void run()
    {
        init();
    }

    /**
     * @deprecated Only provided for backward compatibility. Use {@link MicroManager#getCore()} instead.
     */
    @Deprecated
    public static CMMCore getCore()
    {
        return MicroManager.getCore();
    }
}