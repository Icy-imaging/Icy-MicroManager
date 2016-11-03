package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.dialog.MessageDialog;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import plugins.tprovoost.Microscopy.MicroManager.tools.FrameUtils;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

public class PluginsToolbar extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = -8211241234002062845L;

    final MMMainFrame mainFrame;

    JToolBar pluginToolbar;
    /** List of all plugins added to the main plugin via addPlugin method. */
    List<MicroscopePlugin> pluginList;

    /**
     * Create the panel.
     */
    public PluginsToolbar(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;
        pluginList = new ArrayList<MicroscopePlugin>();

        initialize(PluginLoader.getPlugins(MicroscopePlugin.class));
    }

    private void initialize(List<PluginDescriptor> plugins)
    {
        pluginToolbar = new JToolBar(SwingConstants.HORIZONTAL);
        pluginToolbar.setRollover(true);
        pluginToolbar.setFloatable(false);
        pluginToolbar.setToolTipText("Micro Manager plugins for Icy");

        for (final PluginDescriptor plugin : plugins)
        {
            pluginToolbar.add(FrameUtils.createPluginButton(plugin, new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // create the microscope plugin
                    final MicroscopePlugin microscopePlugin = (MicroscopePlugin) PluginLauncher.start(plugin);

                    if (microscopePlugin != null)
                    {
                        // register and start
                        addPlugin(microscopePlugin);
                        microscopePlugin.start();
                    }
                }
            }));
        }

//        if (plugins.size() == 0)
//        {
//            MessageDialog.showDialog("Information",
//                    "You don't have any Micro manager plugins installed, use the search bar to install some",
//                    MessageDialog.INFORMATION_MESSAGE);
//
//            // search for micro manager plugin
//            // Icy.getMainInterface().getSearchEngine().search("micro-manager");
//        }

        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Plugins"));
        add(pluginToolbar, BorderLayout.CENTER);
    }

    public void onExposureChanged(double exposure)
    {
        for (MicroscopePlugin plugin : pluginList)
            plugin.onExposureChanged(exposure);
    }

    public void onCorePropertyChanged(String deviceName, String propName, String propValue)
    {
        for (MicroscopePlugin plugin : pluginList)
            plugin.onCorePropertyChanged(deviceName, propName, propValue);
    }

    public void onSystemConfigurationLoaded()
    {
        for (MicroscopePlugin plugin : pluginList)
            plugin.onSystemConfigurationLoaded();
    }

    /**
     * Adds the plugin to the active plugin list of MMMainFrame.
     * 
     * @param plugin
     *        : plugin to be added.
     * @see #removePlugin(MicroscopePlugin)
     */
    public void addPlugin(MicroscopePlugin plugin)
    {
        if (plugin != null)
            pluginList.add(plugin);
    }

    /**
     * Removes the plugin from the active plugin list of MMMainFrame.
     * 
     * @param plugin
     *        : plugin to be removed.
     * @see #addPlugin(MicroscopePlugin)
     */
    public void removePlugin(MicroscopePlugin plugin)
    {
        if (plugin != null)
            pluginList.remove(plugin);
    }

    /**
     * Returns list of active Microscope Plugin
     */
    public List<MicroscopePlugin> getRunningPlugins()
    {
        return new ArrayList<MicroscopePlugin>(pluginList);
    }

    /**
     * Returns number of active Microscope Plugin
     */
    public int getRunningPluginsCount()
    {
        return pluginList.size();
    }

    /**
     * Shutdown all running plugins (should automatically call removePlugin(..))
     */
    public void shutdownPlugins()
    {
        for (MicroscopePlugin plugin : getRunningPlugins())
            plugin.shutdown();
    }
}
