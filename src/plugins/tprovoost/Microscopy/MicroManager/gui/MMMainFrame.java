package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.common.MenuCallback;
import icy.file.FileUtil;
import icy.gui.dialog.ActionDialog;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.frame.progress.ToolTipFrame;
import icy.gui.util.ComponentUtil;
import icy.main.Icy;
import icy.preferences.PluginPreferences;
import icy.preferences.XMLPreferences;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.system.IcyExceptionHandler;
import icy.system.SystemUtil;
import icy.system.thread.ThreadUtil;
import icy.util.ReflectionUtil;
import icy.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import mmcorej.MMEventCallback;

import org.micromanager.internal.MMStudio;
import org.micromanager.internal.MMVersion;
import org.micromanager.internal.MainFrame;
import org.micromanager.internal.PropertyEditor;
import org.micromanager.internal.hcwizard.MMConfigFileException;
import org.micromanager.internal.hcwizard.MicroscopeModel;
import org.micromanager.internal.dialogs.AcqControlDlg;
import org.micromanager.internal.dialogs.CalibrationListDlg;
import org.micromanager.internal.dialogs.IntroDlg;
import org.micromanager.internal.dialogs.OptionsDlg;
import org.micromanager.internal.utils.DefaultUserProfile;
import org.micromanager.internal.utils.ReportingUtils;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.core.AcquisitionHandler;
import plugins.tprovoost.Microscopy.MicroManager.tools.FrameUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicromanagerPlugin;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

/**
 * Main frame for Micro Manager Plugin (Singleton pattern)
 * 
 * @author Stephane Dallongeville
 * @author Irsath Nguyen
 */
public class MMMainFrame extends IcyFrame
{
    public static final String ID_STORELASTACQ = "storeLastAcquisition";
    public static final String ID_DISPLAYACQ = "displayAcquisition";

    MMStudio mmstudio;
    AcquisitionHandler acquisitionHandler;

    // extract these fields from original MMStudio object
    Preferences mainPreferences;
    Preferences exposurePrefs;
    Preferences colorPrefs;
    Preferences contrastPrefs;

    // panels composing the main frame
    ActionsPanel actionsPanel;
    ConfigurationPanel configPanel;
    CameraSettingsPanel cameraPanel;
    LiveSettingsPanel livePanel;
    AcquisitionInfoPanel acquisitionInfoPanel;
    PluginsToolbar pluginsPanel;

    // MM event handler
    MMEventCallback mainCallback;

    // EXCLUSIVE ACCESS LOCK
    final ReentrantLock rlock;

    public XMLPreferences preferences;

    // internals
    boolean doNotAskConfigFileSave;
    boolean hideMDADisplaySave;
    boolean closeOnExitSave;

    public MMMainFrame() throws Exception
    {
        super("Micro-Manager For Icy", true, true, false, true);

        // instanced = false;
        preferences = PluginPreferences.getPreferences().node(MicromanagerPlugin.class.getName());
        rlock = new ReentrantLock(true);

        // set to null by default to ensure correct shutdown sequence
        mmstudio = null;
        acquisitionHandler = null;
        mainPreferences = null;
        exposurePrefs = null;
        colorPrefs = null;
        contrastPrefs = null;
        mainCallback = null;

        actionsPanel = null;
        configPanel = null;
        cameraPanel = null;
        livePanel = null;
        acquisitionInfoPanel = null;
        pluginsPanel = null;

        try
        {
        	//TODO
            // we have our own load config frame so we hide the one from MicroManager
//            DefaultUserProfile.getInstance().
//            options.loadSettings();
            doNotAskConfigFileSave = DefaultUserProfile.getShouldAlwaysUseDefaultProfile();
            mmstudio.getUserProfile().syncToDisk();
        }
        catch (Throwable t)
        {
            // ignore
        }

        ThreadUtil.invokeNow(new Callable<Object>()
        {
            @Override
            public Object call() throws Exception
            {
                try
                {
                    mmstudio = new MMStudio(true);
                }
                catch (Throwable t)
                {
                    // shutdown
                    shutdown();
                    // and forward exception
                    throw new Exception("Could not initialize Micro Manager !", t);
                }

                try
                {
                    try
                    {

                        // patch some settings
                        hideMDADisplaySave = mmstudio.getHideMDADisplayOption();
                        closeOnExitSave = OptionsDlg.getShouldCloseOnExit();
                        
                        AcqControlDlg.setShouldHideMDADisplay(true);
                    	OptionsDlg.setShouldCloseOnExit(false);
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Warning: cannot patch options informations from Micro-Manager.");
                    }

                    try
                    {
                        mainPreferences = (Preferences) ReflectionUtil.getFieldObject(mmstudio, "mainPrefs_", true);
                        colorPrefs = (Preferences) ReflectionUtil.getFieldObject(mmstudio, "colorPrefs_", true);
                        exposurePrefs = (Preferences) ReflectionUtil.getFieldObject(mmstudio, "exposurePrefs_", true);
                        contrastPrefs = (Preferences) ReflectionUtil.getFieldObject(mmstudio, "contrastPrefs_", true);
                    }
                    catch (Exception ex)
                    {
                        System.err.println("Warning: cannot retrieve Preferences from Micro-Manager.");
                    }

                    final MainFrame frame = MMStudio.getFrame();
                    if (frame != null)
                    {
                        // force some initialization stuff on micro manager
                        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_OPENED));
                        // hide the main frame of Micro-Manager (we don't want it)
                        frame.setVisible(false);
                    }

                    // build the advanced acquisition handler
                    acquisitionHandler = new AcquisitionHandler(MMMainFrame.this);

                    // ok
                    return null;
                }
                catch (Throwable t)
                {
                    // shutdown
                    shutdown();
                    // and forward exception
                    throw new Exception("Error while initializing Micro Manager !", t);
                }
            }
        });

        final CMMCore core = mmstudio.getCore();

        if (core == null)
        {
            // shutdown
            shutdown();
            // and forward exception
            throw new Exception("Could not retrieve Micro Manager core !");
        }

        // init some core stuff
        core.enableDebugLog(false);
        core.enableStderrLog(false);

        // we need to reference it as core.registerCallback(..) does not retain it !
        mainCallback = new CustomEventCallback();
        core.registerCallback(mainCallback);

        // try
        // {
        // // FIXME: really needed ??
        // mmstudio.setPositionList(new PositionList());
        // }
        // catch (MMScriptException e1)
        // {
        // mmstudio.logError(e1);
        // }

        // _camera_label = mmstudio.getCore().getCameraDevice();
        // if (_camera_label == null)
        // _camera_label = "";

        // instanced = true;
    }

    /**
     * Should be used internally only.
     */
    public void init()
    {
        ThreadUtil.invokeNow(new Runnable()
        {
            @Override
            public void run()
            {
                initializeSystemMenu();
                initializeGUI();

                setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                addFrameListener(new IcyFrameAdapter()
                {
                    @Override
                    public void icyFrameClosing(IcyFrameEvent e)
                    {
                        preClose();
                    }
                });

                // get default packed size
                getInternalFrame().pack();
                getExternalFrame().pack();

                Dimension size = getSize();
                // adjust default packed size which is not correct
                size.height = 400;
                size.width -= 40;
                setMinimumSizeExternal(size);
                setMinimumSizeInternal(size);

                // set preferred size
                size = new Dimension(size);
                size.width += 40;
                size.height += 80;
                setPreferredSizeExternal(size);
                setPreferredSizeInternal(size);

                // repack
                getInternalFrame().pack();
                getExternalFrame().pack();

                addToDesktopPane();
                center();
                setVisible(true);
            }
        });

        final ToolTipFrame tooltip = new ToolTipFrame(
                "<html>You can access more Micro-Manager options from menu by clicking on top left icon:<br>"
                        + "<img src=\"" + getClass().getResource("/res/image/menu_tip.jpg").toString() + "\" /></html>",
                30, "MicroManager.HiddenMenu.Tip");
        tooltip.setSize(264, 192);
    }

    public void preClose()
    {
        if (getInternalFrame().getDefaultCloseOperation() == WindowConstants.DO_NOTHING_ON_CLOSE)
        {
            if (!Icy.isExiting() && (pluginsPanel.getRunningPluginsCount() > 0))
            {
                if (!ConfirmDialog
                        .confirm("Some Micro-Manager plugins are still running.\nClosing this frame will interrupt all Micro-Manager activities. Continue ?"))
                    return;
            }

            MicroManager.shutdown();
        }
    }

    @Override
    public void onClosed()
    {
        shutdown();

        super.onClosed();
    }

    void shutdown()
    {
        // shutdown plugins
        if (pluginsPanel != null)
            pluginsPanel.shutdownPlugins();
        // shutdown the acquisition handler
        if (acquisitionHandler != null)
            acquisitionHandler.shutdown();

        // stop activities
        if (mmstudio != null)
        {
            mmstudio.acquisitions().haltAcquisition();
//            closeAllAcquisitions();
            mmstudio.closeSequence(true);
        }

        // no more reference, can be released
        mainCallback = null;

        // restore patched settings
    	DefaultUserProfile.setShouldAlwaysUseDefaultProfile(doNotAskConfigFileSave);
    	AcqControlDlg.setShouldHideMDADisplay(hideMDADisplaySave);
//    	DefaultUserProfile.closeOnExit_ = closeOnExitSave;
    }

    /**
     * Create all needed graphics components and listeners and show the main frame.
     */
    void initializeGUI()
    {
        // we use the MainPanel class just for easier GUI designing
        final MainPanel mainPanel = new MainPanel(this);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        actionsPanel = mainPanel.actionsPanel;
        configPanel = mainPanel.configPanel;
        cameraPanel = mainPanel.cameraPanel;
        livePanel = mainPanel.livePanel;
        acquisitionInfoPanel = mainPanel.acquisitionInfoPanel;
        pluginsPanel = mainPanel.pluginsPanel;

        // refresh GUI now
        configPanel.refreshConfigsNow(false);
        cameraPanel.refreshNow();
        acquisitionInfoPanel.refreshNow();
    }

    void initializeSystemMenu()
    {
        final int SHORTCUTKEY_MASK = SystemUtil.getMenuCtrlMask();

        setSystemMenuCallback(new MenuCallback()
        {
            @Override
            public JMenu getMenu()
            {
                JMenu toReturn = getDefaultSystemMenu();
                JMenuItem hconfig = new JMenuItem("Configuration Wizard");
                hconfig.setIcon(new IcyIcon("star.png"));
                hconfig.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        // we have some plugins running ?
                        if (pluginsPanel.getRunningPluginsCount() > 0)
                        {
                            // need confirmation
                            if (!ConfirmDialog.confirm("Are you sure ?",
                                    "<html>Loading the Configuration Wizard will unload all the devices"
                                            + " and pause all running acquisitions.</br>"
                                            + " Are you sure you want to continue ?</html>"))
                                return;
                        }

                        try
                        {
                            mmstudio.getCore().unloadAllDevices();
                        }
                        catch (Exception e1)
                        {
                            ReportingUtils.logError(e1);
                        }

                        String sysConfigFile = IntroDlg.getMostRecentlyUsedConfig();
                        IntroDlg configurator = new IntroDlg(mmstudio, sysConfigFile, MMVersion.VERSION_STRING);
                        configurator.setVisible(true);

                        // define new default config file
                        MicroManager.setDefaultConfigFileName(configurator.getConfigFile());
                        // and load it
                        loadDefaultConfig();
                        refreshConfigs();
                        refreshGUI();
                    }
                });

                JMenuItem menuPxSizeConfigItem = new JMenuItem("Pixel Size Config");
                menuPxSizeConfigItem.setIcon(new IcyIcon(ResourceUtil.ICON_PROPERTIES));
                menuPxSizeConfigItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.SHIFT_DOWN_MASK
                        | SHORTCUTKEY_MASK));
                menuPxSizeConfigItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        CalibrationListDlg dlg = new CalibrationListDlg(mmstudio.getCore());
                        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        dlg.setParentGUI(mmstudio);
                        final IcyFrame pixelSizeConfig = FrameUtils.addMMWindowToDesktopPane(dlg);
                        pixelSizeConfig.setSize(320, 260);
                        pixelSizeConfig.center();
                        pixelSizeConfig.setResizable(true);
                    }
                });

                JMenuItem loadConfigItem = new JMenuItem("Load Configuration");
                loadConfigItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK
                        | SHORTCUTKEY_MASK));
                loadConfigItem.setIcon(new IcyIcon(ResourceUtil.ICON_OPEN));
                loadConfigItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        final LoadFrame f = new LoadFrame();

                        if (1 != f.showDialog())
                        {
                            try
                            {
                                // unload all devices
                                mmstudio.getCore().unloadAllDevices();
                            }
                            catch (Exception e1)
                            {
                            	ReportingUtils.logError(e1);
                            }

                            // define new default config file
                            MicroManager.setDefaultConfigFileName(f.getConfigFilePath());
                            // and load it
                            loadDefaultConfig();
                            refreshConfigs();
                            refreshGUI();
                        }
                    }
                });

                JMenuItem saveConfigItem = new JMenuItem("Save Configuration");
                saveConfigItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK
                        | SHORTCUTKEY_MASK));
                saveConfigItem.setIcon(new IcyIcon(ResourceUtil.ICON_SAVE));
                saveConfigItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        saveConfig();
                    }
                });

                JMenuItem aboutItem = new JMenuItem("About");
                aboutItem.setIcon(new IcyIcon(ResourceUtil.ICON_INFO));
                aboutItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        final JDialog dialog = new JDialog(Icy.getMainInterface().getMainFrame(), "About");

                        dialog.getContentPane().setLayout(new BorderLayout());
                        dialog.getContentPane().add(new AboutPanel(new ActionListener()
                        {

                            @Override
                            public void actionPerformed(ActionEvent e)
                            {
                                dialog.dispose();
                            }
                        }), BorderLayout.CENTER);
                        dialog.setResizable(false);
                        dialog.pack();
                        ComponentUtil.center(dialog);
                        dialog.setVisible(true);
                    }
                });

                JMenuItem propertyBrowserItem = new JMenuItem("Property Browser");
                propertyBrowserItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, SHORTCUTKEY_MASK));
                propertyBrowserItem.setIcon(new IcyIcon(ResourceUtil.ICON_DATABASE));
                propertyBrowserItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        PropertyEditor editor = new PropertyEditor(mmstudio);
                        //TODO
//                        editor.setCore(mmstudio.getCore());
                        editor.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        final IcyFrame propertyBrowser = FrameUtils.addMMWindowToDesktopPane(editor);
                        propertyBrowser.setSize(380, 480);
                        propertyBrowser.center();
                        propertyBrowser.setResizable(true);
                    }
                });

                JMenuItem resetMMPath = new JMenuItem("Reset Micro-Manager path");
                resetMMPath.setIcon(new IcyIcon("folder"));
                resetMMPath.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        // so user can change the defined MM folder
                        MMUtils.resetLibrayPath();
                        MessageDialog.showDialog("Information",
                                "You need to restart Icy now to change the defined Micro-Manager folder.",
                                MessageDialog.INFORMATION_MESSAGE);
                    }
                });

                // JMenuItem loadPresetConfigItem = new JMenuItem("Load Core properties from XML");
                // loadPresetConfigItem.setIcon(new IcyIcon(ResourceUtil.ICON_DOC_IMPORT));
                // loadPresetConfigItem.addActionListener(new ActionListener()
                // {
                // @Override
                // public void actionPerformed(ActionEvent e)
                // {
                // loadXMLFile(preferences.node("CoreProperties"));
                // }
                // });
                //
                // JMenuItem savePresetConfigItem = new JMenuItem("Save Core properties to XML");
                // savePresetConfigItem.setIcon(new IcyIcon(ResourceUtil.ICON_DOC_EXPORT));
                // savePresetConfigItem.addActionListener(new ActionListener()
                // {
                // @Override
                // public void actionPerformed(ActionEvent e)
                // {
                // saveToXML(preferences.node("CoreProperties"));
                // }
                // });
                //
                JMenuItem mmSettingItem = new JMenuItem("Micro-Manager options");
                mmSettingItem.setIcon(new IcyIcon(ResourceUtil.ICON_COG, true));
                mmSettingItem.setToolTipText("Set a variety of Micro-Manager configuration options");
                mmSettingItem.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {

                        final OptionsPanel optionsPanel = new OptionsPanel(MMMainFrame.this, mmstudio);
                        final ActionDialog optionsDialog = new ActionDialog("Micro-Manager Options", optionsPanel, Icy
                                .getMainInterface().getMainFrame());
                        optionsDialog.setOkAction(optionsPanel);
                        optionsDialog.pack();
                        optionsDialog.setResizable(false);
                        optionsDialog.setLocationRelativeTo(getFrame());
                        optionsDialog.setVisible(true);
                    }
                });

                int idx = 0;
                toReturn.insert(hconfig, idx++);
                toReturn.insert(loadConfigItem, idx++);
                toReturn.insert(saveConfigItem, idx++);
                toReturn.insert(resetMMPath, idx++);
                toReturn.insertSeparator(idx++);
                // toReturn.insert(loadPresetConfigItem, idx++);
                // toReturn.insert(savePresetConfigItem, idx++);
                // toReturn.insertSeparator(idx++);
                toReturn.insert(propertyBrowserItem, idx++);
                toReturn.insert(menuPxSizeConfigItem, idx++);
                toReturn.insert(mmSettingItem, idx++);
                toReturn.insertSeparator(idx++);
                toReturn.insert(aboutItem, idx++);

                return toReturn;
            }
        });
    }

    // /**
    // * @return Returns if this class is being instanced.
    // */
    // public boolean isInstancing()
    // {
    // return instancing;
    // }

    // public boolean isInstanced()
    // {
    // return instanced;
    // }

    public void lock()
    {
        rlock.lock();
    }

    public boolean lock(long wait) throws InterruptedException
    {
        return rlock.tryLock(wait, TimeUnit.MILLISECONDS);
    }

    public void unlock()
    {
        rlock.unlock();
    }

    /**
     * Blocking method
     * 
     * @param filePath
     */
    void loadConfig(final String filePath)
    {
        // show loading message
        final LoadingFrame loadingFrame = new LoadingFrame(
                "  Please wait while loading Micro-Manager configuration, it may take a while...  ");

        loadingFrame.show();
        try
        {
            mmstudio.getCore().waitForSystem();
            mmstudio.getCore().loadSystemConfiguration(filePath);
        }
        catch (Exception e)
        {
            MessageDialog
                    .showDialog(
                            "Error while initializing the microscope: please check if all devices are correctly turned on "
                                    + "and recognized by the computer and quit any program using those devices. Pleas check also that your configuration file is correct.",
                            MessageDialog.ERROR_MESSAGE);
        }
        finally
        {
            loadingFrame.close();
        }
    }

    void loadDefaultConfig()
    {
        loadConfig(MicroManager.getDefaultConfigFileName());
    }

    /**
     * Save the configuration presets. From Micro-Manager.
     */
    public void saveConfig()
    {
        try
        {
            MicroscopeModel model = new MicroscopeModel();
            model.loadFromFile(MicroManager.getDefaultConfigFileName());
            model.createSetupConfigsFromHardware(mmstudio.getCore());
            model.createResolutionsFromHardware(mmstudio.getCore());
            String path = SaveDialog.chooseFile("Save the configuration file", FileUtil.getApplicationDirectory(),
                    "myConfig", ".cfg");
            if (path != null)
                model.saveToFile(path);
        }
        catch (MMConfigFileException e)
        {
            ReportingUtils.logError(e);
            IcyExceptionHandler.showErrorMessage(e, false);
            new FailedAnnounceFrame("Unable to save configuration file");
        }
    }

    public MMStudio getMMStudio()
    {
        return mmstudio;
    }

    public AcquisitionHandler getAcquisitionHandler()
    {
        return acquisitionHandler;
    }

    public Preferences getMainPreferences()
    {
        return mainPreferences;
    }

    public XMLPreferences getPreferences()
    {
        return preferences;
    }

    /**
     * Returns the "enable storage of last acquisition" state.
     * 
     * @see #setStoreLastAcquisition(boolean)
     * @see MicroManager#getAcquisitionResult()
     */
    public boolean getStoreLastAcquisition()
    {
        return preferences.getBoolean(ID_STORELASTACQ, true);
    }

    /**
     * Enable storage of last acquisition so it can be retrieved with {@link MicroManager#getAcquisitionResult()}.<br>
     * Set to <code>true</code> by default.
     * 
     * @see MicroManager#getAcquisitionResult()
     */
    public void setStoreLastAcquisition(boolean value)
    {
        preferences.putBoolean(ID_STORELASTACQ, value);
    }

    /**
     * Enable immediate display of image acquisition.
     * 
     * @see MicroManager#startAcquisition(int, double)
     */
    public boolean getDisplayAcquisitionSequence()
    {
        return preferences.getBoolean(ID_DISPLAYACQ, true);
    }

    /**
     * Enable immediate display of image acquisition.
     * 
     * @see MicroManager#startAcquisition(int, double)
     */
    public void setDisplayAcquisitionSequence(boolean value)
    {
        preferences.putBoolean(ID_DISPLAYACQ, value);
    }

    void refreshGUI()
    {
        if (cameraPanel != null)
            cameraPanel.refresh();
        if (acquisitionInfoPanel != null)
            acquisitionInfoPanel.refresh();
    }

    void refreshConfigs()
    {
        if (configPanel != null)
            configPanel.refreshConfigs(false);
    }

    /**
     * Adds the plugin to the plugin list of MMMainFrame.
     * 
     * @param plugin
     *        : plugin to be added.
     * @see #removePlugin(MicroscopePlugin)
     */
    public void addPlugin(MicroscopePlugin plugin)
    {
        if (pluginsPanel != null)
            pluginsPanel.addPlugin(plugin);
    }

    /**
     * Removes the plugin from the plugin list of MMMainFrame. If no more plugin
     * using the acquisition is running, acquisition is stopped.
     * 
     * @param plugin
     *        : plugin to be removed.
     * @see #addPlugin(MicroscopePlugin)
     */
    public void removePlugin(MicroscopePlugin plugin)
    {
        if (pluginsPanel != null)
            pluginsPanel.removePlugin(plugin);
    }

    /**
     * Notify the GUI that he need to draw the indeterminate progress bar representing a live
     * acquisition running.
     */
    public void liveStarted()
    {
        // do here specific task when live started

    }

    /**
     * Notify the GUI that he no more need to draw the indeterminate progress bar representing a
     * live acquisition running.
     */
    public void liveStopped()
    {
        // do here specific task when live stopped

    }

    // /**
    // * Save all the properties into an XML file.
    // *
    // * @param root
    // * : file and node where data is saved.
    // */
    // void saveToXML(XMLPreferences root)
    // {
    // StrVector devices = mmstudio.getCore().getLoadedDevices();
    // for (int i = 0; i < devices.size(); i++)
    // {
    // XMLPreferences prefs = root.node(devices.get(i));
    // StrVector properties;
    // try
    // {
    // properties = mmstudio.getCore().getDevicePropertyNames(devices.get(i));
    // }
    // catch (Exception e)
    // {
    // continue;
    // }
    // for (int j = 0; j < properties.size(); j++)
    // {
    // PropertyItem item = new PropertyItem();
    // item.readFromCore(mmstudio.getCore(), devices.get(i), properties.get(j), false);
    // prefs.put(properties.get(j), item.value);
    // }
    // }
    // }
    //
    // /**
    // * Load all the properties into a file.
    // *
    // * @param root
    // * : file and node where data is saved.
    // */
    // void loadXMLFile(XMLPreferences root)
    // {
    // for (XMLPreferences device : root.getChildren())
    // {
    // for (String propName : device.keys())
    // {
    // String value = device.get(propName, "");
    //
    // if (!StringUtil.isEmpty(value))
    // {
    // try
    // {
    // mmstudio.getCore().setProperty(device.name(), propName, value);
    // mmstudio.getCore().waitForSystem();
    // }
    // catch (Exception e)
    // {
    // continue;
    // }
    // }
    // }
    // }
    // }

    class CustomEventCallback extends MMEventCallback
    {
        @Override
        public void onPropertiesChanged()
        {
            refreshGUI();
        }

        @Override
        public void onConfigGroupChanged(String groupName, String newConfig)
        {
            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
        }

        @Override
        public void onExposureChanged(String deviceName, double exposure)
        {
            // only if device name match current set camera name
            if ((cameraPanel != null) && StringUtil.equals(deviceName, cameraPanel.getCameraName()))
            {
                cameraPanel.onExposureChanged(exposure);
                if (pluginsPanel != null)
                    pluginsPanel.onExposureChanged(exposure);
            }

            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
        }

        @Override
        public void onPropertyChanged(String deviceName, String propName, String propValue)
        {
            // only if device name match current set camera name
            if ((cameraPanel != null) && StringUtil.equals(deviceName, cameraPanel.getCameraName()))
            {
                if (propName.equals(MMCoreJ.getG_Keyword_Binning()))
                    cameraPanel.onBinningChanged(propValue);
            }

            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
            if (pluginsPanel != null)
                pluginsPanel.onCorePropertyChanged(deviceName, propName, propValue);
        }

        @Override
        public void onStagePositionChanged(String deviceName, double pos)
        {
            StageMover.onStagePositionChanged(deviceName, pos);
            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
        }

        @Override
        public void onXYStagePositionChanged(String deviceName, double xPos, double yPos)
        {
            StageMover.onXYStagePositionChanged(deviceName, xPos, yPos);
            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
        };

        @Override
        public void onSystemConfigurationLoaded()
        {
            if (pluginsPanel != null)
                pluginsPanel.onSystemConfigurationLoaded();
            if (acquisitionInfoPanel != null)
                acquisitionInfoPanel.refresh();
        }
    }
}