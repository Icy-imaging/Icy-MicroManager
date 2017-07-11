package plugins.tprovoost.Microscopy.MicroManagerForIcy;

import icy.plugin.abstract_.Plugin;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;

/**
 * This is the class to inherit in order to create a Microscope Plugin.<br/>
 * Any {@link MicroscopePlugin} has to implement the start() method. That
 * way, you will have access to the main interface and the core, and your plugin
 * will automatically wait for the Micro-Manager For Icy to be running.<br/>
 * <b>At the end of life of your plugin you should manually call shutdown()</b>
 * <p>
 * <b>Example: </b></br> start() { <br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;System.out.println(core.getAPIVersionInfo());<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;shutdown();<br/>
 * }
 * </p>
 * 
 * @see #start()
 * @author Irsath Nguyen
 * @author Stephane Dallongeville
 */
public abstract class MicroscopePlugin extends Plugin
{
    public MicroscopePlugin()
    {
        super();

        MicromanagerPlugin.init();

        if (!MicroManager.isInitialized())
            throw new RuntimeException("Cannot initialize Micro-Manager !");
    }

    /**
     * Method to define in sub-classes in replacement of usual run() method of
     * Icy plugin's
     * It is called only <b>AFTER</b> Micro-Manager For Icy is launched.
     */
    public abstract void start();

    /**
     * Called when main Micro-Manager GUI is closed.<br/>
     * Override this method to compute some special operation / data saving before exiting (like image saving, frame
     * position saving...)<br/>
     * <b>You should dispose all your threads / resources using MicroManager in this method, remove all listeners and
     * not use them anymore.</b><br/>
     * Don't delete the call to super.shutdown();
     */
    public void shutdown()
    {
        MicroManager.getInstance().removePlugin(this);
    }

    /**
     * @deprecated No more supported (not really useful)
     */
    @Deprecated
    public void showProgressBar(boolean value)
    {
        //
    }

    /**
     * @deprecated No more supported (not really useful)
     */
    @Deprecated
    public void removeProgressBar()
    {
        //
    }

    /**
     * @deprecated No more supported (not really useful)
     */
    @Deprecated
    public void notifyProgress(int progress)
    {
        //
    }

    /**
     * Override this method in order to listen to exposure changes.
     * 
     * @param newExposure
     */
    public void onExposureChanged(double newExposure)
    {
        //
    }

    /**
     * Override this method in order to listen to core properties changes.
     * 
     * @param deviceName
     * @param propName
     *        Defined as string's in MMCoreJ.getG_Keyword_[....]
     * @param propValue
     */
    public void onCorePropertyChanged(String deviceName, String propName, String propValue)
    {
        //

    }

    /**
     * Ovveride this method in order to be notified when an configuration (cfg file) is loaded is
     * Micro-Manager
     */
    public void onSystemConfigurationLoaded()
    {
        //

    }
}