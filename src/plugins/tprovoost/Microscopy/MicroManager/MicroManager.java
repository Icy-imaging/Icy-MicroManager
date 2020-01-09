package plugins.tprovoost.Microscopy.MicroManager;

import icy.common.Version;
import icy.file.FileUtil;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.system.thread.ThreadUtil;
import icy.util.ClassUtil;
import icy.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.WindowConstants;

import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.MMVersion;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.acquisition.AcquisitionWrapperEngine;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.IAcquisitionEngine2010;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.ReportingUtils;

import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import mmcorej.StrVector;
import mmcorej.TaggedImage;
import plugins.tprovoost.Microscopy.MicroManager.core.AcquisitionResult;
import plugins.tprovoost.Microscopy.MicroManager.event.AcquisitionListener;
import plugins.tprovoost.Microscopy.MicroManager.event.LiveListener;
import plugins.tprovoost.Microscopy.MicroManager.gui.LoadFrame;
import plugins.tprovoost.Microscopy.MicroManager.gui.LoadingFrame;
import plugins.tprovoost.Microscopy.MicroManager.gui.MMMainFrame;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicromanagerPlugin;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

public class MicroManager
{
    /**
     * Metadata associated to last retrieved image.
     */
    private static final Map<Integer, JSONObject> metadatas = new HashMap<Integer, JSONObject>(4);

    static final List<AcquisitionListener> acqListeners = new ArrayList<AcquisitionListener>();
    static final List<LiveListener> liveListeners = new ArrayList<LiveListener>();

    static AcquisitionResult acquisitionManager = null;
    static MMMainFrame instance = null;
    static Thread liveManager = null;

    /**
     * Retrieve the MicroManager main frame instance.
     */
    public static MMMainFrame getInstance()
    {
        // initialize micro manager if needed
        // if (instance == null)
        // MicromanagerPlugin.init();

        return instance;
    }

    /**
     * Retrieve the MicroManager version
     */
    public static Version getMMVersion()
    {
        // is this method really safe ??
        return new Version(MMVersion.VERSION_STRING);
    }

    /**
     * @return the acquisition listener list.
     */
    public static List<AcquisitionListener> getAcquisitionListeners()
    {
        final List<AcquisitionListener> result;

        // create safe copy
        synchronized (acqListeners)
        {
            result = new ArrayList<AcquisitionListener>(acqListeners);
        }

        return result;
    };

    /**
     * Every {@link MicroscopePlugin} shares the same core, so you will receive every acquisition
     * even if it was not asked by you.<br/>
     * You should register your listener only when you need image and remove it when your done.
     * 
     * @param listener
     *        Your listener
     */
    public static void addAcquisitionListener(AcquisitionListener listener)
    {
        synchronized (acqListeners)
        {
            if (!acqListeners.contains(listener))
                acqListeners.add(listener);
        }
    }

    public static void removeAcquisitionListener(AcquisitionListener listener)
    {
        synchronized (acqListeners)
        {
            acqListeners.remove(listener);
        }
    }

    /**
     * @return the live listener list.
     */
    public static List<LiveListener> getLiveListeners()
    {
        final List<LiveListener> result;

        // create safe copy
        synchronized (liveListeners)
        {
            result = new ArrayList<LiveListener>(liveListeners);
        }

        return result;
    }

    /**
     * Every {@link MicroscopePlugin} shares the same core, so the same live,<br/>
     * when your plugin start, a live may have been already started and your {@link LiveListener}
     * <br/>
     * could receive images at the moment you call this method. <br />
     * You should register your listener only when you need image and remove it when your done.
     */
    public static void addLiveListener(LiveListener listener)
    {
        synchronized (liveListeners)
        {
            if (!liveListeners.contains(listener))
                liveListeners.add(listener);
        }
    }

    public static void removeLiveListener(LiveListener listener)
    {
        synchronized (liveListeners)
        {
            liveListeners.remove(listener);
        }
    }

    /**
     * @deprecated Use {@link #addAcquisitionListener(AcquisitionListener)} instead.
     */
    @Deprecated
    public static void registerListener(AcquisitionListener listener)
    {
        addAcquisitionListener(listener);
    }

    /**
     * @deprecated Use {@link #removeAcquisitionListener(AcquisitionListener)} instead.
     */
    @Deprecated
    public static void removeListener(AcquisitionListener listener)
    {
        removeAcquisitionListener(listener);
    }

    /**
     * @deprecated Use {@link #addLiveListener(LiveListener)} instead.
     */
    @Deprecated
    public static void registerListener(LiveListener listener)
    {
        addLiveListener(listener);
    }

    /**
     * @deprecated Use {@link #removeLiveListener(LiveListener)} instead.
     */
    @Deprecated
    public static void removeListener(LiveListener listener)
    {
        removeLiveListener(listener);
    }

    /**
     * Use this to access micro-manager main object to access low level function that
     * are not been implemented in Icy's micro-Manager.
     * 
     * @return The micro-manager main studio object.
     */
    public static MMStudio getMMStudio()
    {
        final MMMainFrame inst = getInstance();
        if (inst == null)
            return null;

        return inst.getMMStudio();
    }

    /**
     * Use this to access micro-manager core and to be able to use function that
     * are not been implemented in Micro-Manger for Icy. </br>
     * </br>
     * Be careful, if you use core functions instead of the integrated {@link MicroManager} methods
     * or utility class as {@link StageMover} you will have to handle synchronization of the
     * core and catch all exception. </br>
     * In most of case you don't need to use it, but only if you want to make fancy things.
     */
    public static CMMCore getCore()
    {
        final MMStudio mmstudio = getMMStudio();
        if (mmstudio == null)
            return null;

        return mmstudio.getCore();
    }

    /**
     * Get exclusive access to micro manager.</br>
     * 
     * @param wait
     *        number of milli second to wait to retrieve exclusive access if micro manager is
     *        already locked by another thread. If set to 0 then it returns immediately if already
     *        locked.
     * @return <code>true</code> if you obtained exclusive access and <code>false</code> if micro
     *         manager is already locked by another thread and wait time elapsed.
     * @throws InterruptedException
     * @see #lock()
     * @see #unlock()
     */
    public static boolean lock(long wait) throws InterruptedException
    {
        final MMMainFrame inst = getInstance();
        if (inst == null)
            return false;

        return inst.lock(wait);
    }

    /**
     * Get exclusive access to micro manager.</br>
     * If another thread already has exclusive access then it will wait until it release it.
     * 
     * @see #lock(long)
     * @see #unlock()
     */
    public static void lock()
    {
        final MMMainFrame inst = getInstance();
        if (inst == null)
            return;

        inst.lock();
    }

    /**
     * Release exclusive access to micro manager.
     * 
     * @see #lock()
     * @see #lock(long)
     */
    public static void unlock()
    {
        final MMMainFrame inst = getInstance();
        if (inst == null)
            return;

        inst.unlock();
    }
    
    /**
     * Wait for a MM device to be ready.
     * 
     * @throws Exception
     *         if an error occurs
     */
    public static void waitForDevice(String device) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final long start = System.currentTimeMillis();

        if (!StringUtil.isEmpty(device))
        {
            boolean busy = true;

            // we wait for 3 seconds max
            while (busy && ((System.currentTimeMillis() - start) < 3000))
            {
                lock();
                try
                {
                    busy = core.deviceBusy(device);
                    // wait a bit if device is busy
                    if (busy)
                        Thread.yield();
                }
                finally
                {
                    unlock();
                }
            }

            // just for safety we also use "waitForDevice" afterward
            lock();
            try
            {
                core.waitForDevice(device);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Get the default config file name
     */
    public static String getDefaultConfigFileName()
    {
        return System.getProperty("org.micromanager.default.config.file", "MMConfig_demo.cfg");
    }

    /**
     * Set the default config file name
     */
    public static void setDefaultConfigFileName(String fileName)
    {
        if (fileName != null)
            System.setProperty("org.micromanager.default.config.file", fileName);
    }

    /**
     * @return The acquisition engine wrapper from MicroManager.
     */
    public static AcquisitionWrapperEngine getAcquisitionEngine()
    {
        final MMStudio mmstudio = getMMStudio();
        if (mmstudio == null)
            return null;

        return mmstudio.getAcquisitionEngine();
    }

    /**
     * @return The internal new acquisition engine from MicroManager.
     */
    public static IAcquisitionEngine2010 getAcquisitionEngine2010()
    {
        final MMStudio mmstudio = getMMStudio();
        if (mmstudio == null)
            return null;

        return mmstudio.getAcquisitionEngine2010();
    }

    /**
     * @return The engine settings for the most recently started acquisition sequence, or return
     *         null if you never started an acquisition.
     */
    public static SequenceSettings getAcquisitionSettings()
    {
        final AcquisitionWrapperEngine acqEngine = getAcquisitionEngine();
        if (acqEngine == null)
            return null;

        return acqEngine.getSequenceSettings();
    }

    /**
     * @return The summaryMetadata for the most recently started acquisition sequence, or return
     *         null if you never started an acquisition.
     */
    public static JSONObject getAcquisitionMetaData()
    {
        final AcquisitionWrapperEngine acqEngine = getAcquisitionEngine();
        if (acqEngine == null)
            return null;

        return acqEngine.getSummaryMetadata();
    }

    /**
     * @return Returns the number of channel of the camera device (usually 3 for color camera and 1
     *         in other case).</br>
     *         Just a shortcut for <code>getCore().getNumberOfCameraChannels()</code>
     */
    public static long getCameraChannelCount()
    {
        final CMMCore core = MicroManager.getCore();
        if (core == null)
            return 0L;

        return core.getNumberOfCameraChannels();
    }

    /**
     * Returns the metadata object associated to the last image retrieved with
     * {@link #getLastImage()} or
     * {@link #snapImage()}.</br>
     * Returns <code>null</code> if there is no metadata associated to the specified channel.
     * 
     * @param channel
     *        channel index for multi channel camera
     * @see #getLastImage()
     * @see #snapImage()
     * @see #getMetadata()
     * @see MDUtils
     */
    public static JSONObject getMetadata(int channel)
    {
        return metadatas.get(Integer.valueOf(channel));
    }

    /**
     * Returns the metadata object associated to the last image retrieved with
     * {@link #getLastImage()} or
     * {@link #snapImage()}.</br>
     * Returns <code>null</code> if there is no image has been retrieved yet.
     * 
     * @see #getLastImage()
     * @see #snapImage()
     * @see #getMetadata(int)
     * @see MDUtils
     */
    public static JSONObject getMetadata()
    {
        return getMetadata(0);
    }

    /**
     * Returns the last image captured from the micro manager continuous acquisition.</br>
     * Returns <code>null</code> if the continuous acquisition is not running.</br>
     * </br>
     * You can listen new image event from acquisition or live mode by using these methods:</br>
     * {@link #addLiveListener(LiveListener)}</br>
     * {@link #addAcquisitionListener(AcquisitionListener)}</br>
     * </br>
     * You can retrieve the associated metadata by using {@link #getMetadata(int)} method.
     * 
     * @throws Exception
     */
    public static IcyBufferedImage getLastImage() throws Exception
    {
        return MMUtils.convertToIcyImage(getLastTaggedImage());
    }

    /**
     * Returns a list of {@link TaggedImage} representing the last image captured from the micro
     * manager continuous acquisition. The list contains as many image than camera channel (see
     * {@link #getCameraChannelCount()}).</br>
     * Returns an empty list if the continuous acquisition is not running or if image buffer is
     * empty</br>
     * </br>
     * You can listen new image event from acquisition or live mode by using these methods:</br>
     * {@link #addLiveListener(LiveListener)}</br>
     * {@link #addAcquisitionListener(AcquisitionListener)}</br>
     * 
     * @throws Exception
     */
    public static List<TaggedImage> getLastTaggedImage() throws Exception
    {
        final CMMCore core = MicroManager.getCore();

        if (core == null || !core.isSequenceRunning())
            return new ArrayList<TaggedImage>();

        final int numChannel = (int) core.getNumberOfCameraChannels();
        final List<TaggedImage> result = new ArrayList<TaggedImage>(numChannel);

        lock();
        try
        {
            for (int c = 0; c < numChannel; c++)
            {
                final TaggedImage image = core.getLastTaggedImage(c);

                result.add(image);

                // set channel index & number
                if (image != null)
                {
                    MDUtils.setChannelIndex(image.tags, c);
                    MDUtils.setNumChannels(image.tags, numChannel);
                }
            }
        }
        finally
        {
            unlock();
        }

        // check that we don't have poison image
        if (MMUtils.hasNullOrPoison(result))
            return new ArrayList<TaggedImage>();

        // assign metadata
        for (int c = 0; c < numChannel; c++)
            metadatas.put(Integer.valueOf(c), result.get(c).tags);

        return result;
    }

    /**
     * This method waits for the next image captured from the micro manager continuous acquisition
     * and returns it.</br>
     * Returns <code>null</code> if the continuous acquisition is not running.</br>
     * </br>
     * You can listen new image event from acquisition or live mode by using these methods:</br>
     * {@link #addLiveListener(LiveListener)}</br>
     * {@link #addAcquisitionListener(AcquisitionListener)}</br>
     * </br>
     * You can retrieve the associated metadata by using {@link #getMetadata(int)} method.
     */
    public static IcyBufferedImage getNextImage() throws Exception
    {
        return MMUtils.convertToIcyImage(getNextTaggedImage());
    }

    /**
     * This method waits for the next image captured from the micro manager continuous acquisition
     * and returns the result as a list of {@link TaggedImage}. The list contains as many image than
     * camera channel (see {@link #getCameraChannelCount()}).</br>
     * Returns an empty list if the continuous acquisition is not running.</br>
     * </br>
     * You can listen new image event from acquisition or live mode by using these methods:</br>
     * {@link #addLiveListener(LiveListener)}</br>
     * {@link #addAcquisitionListener(AcquisitionListener)}</br>
     */
    public static List<TaggedImage> getNextTaggedImage() throws Exception
    {
        final CMMCore core = MicroManager.getCore();

        if (core == null || !core.isSequenceRunning())
            return new ArrayList<TaggedImage>();

        final boolean done[] = new boolean[] {false};
        final LiveListener listener = new LiveListener()
        {
            @Override
            public void liveStopped()
            {
                done[0] = true;
            }

            @Override
            public void liveStarted()
            {
                // nothing here
            }

            @Override
            public void liveImgReceived(List<TaggedImage> images)
            {
                done[0] = true;
            }
        };

        // listen live events
        addLiveListener(listener);
        try
        {
            // wait (exposure * 2) or 2 sec max
            long maxWait = System.currentTimeMillis();
            maxWait = Math.max(maxWait + 2000L, maxWait + (((long) core.getExposure()) * 2));

            // wait to get a new image from continuous acquisition
            while (System.currentTimeMillis() < maxWait)
            {
                // image received --> stop waiting
                if (done[0])
                    break;
                // sleep a bit
                ThreadUtil.sleep(1L);
            }
        }
        finally
        {
            // can remove live events listener
            removeLiveListener(listener);
        }

        // return last image
        return getLastTaggedImage();
    }

    /**
     * Capture and return an image (or <code>null</code> if an error occurred).<br>
     * If an acquisition is currently in process (live mode or standard acquisition) the method
     * will wait for and return the next image from the image acquisition buffer.<br>
     * In other case it just snaps a new image from the camera device and returns it.<br>
     * You can retrieve the associated metadata by using {@link #getMetadata(int)} method.
     */
    public static IcyBufferedImage snapImage() throws Exception
    {
        return MMUtils.convertToIcyImage(snapTaggedImage());
    }

    /**
     * Capture and return an image (or an empty list if an error occurred).<br>
     * If an acquisition is currently in process (live mode or standard acquisition) the method will
     * will wait for and return the next image from the image acquisition buffer.<br>
     * In other case it just snaps a new image from the camera device and returns it.<br>
     * This function return a list of image as the camera device can have several channel (see
     * {@link #getCameraChannelCount()}) in which case we have one image per channel.<br>
     * You can retrieve the associated metadata by using {@link #getMetadata(int)} method.
     */
    public static List<TaggedImage> snapTaggedImage() throws Exception
    {
        final CMMCore core = MicroManager.getCore();
        if (core == null)
            return new ArrayList<TaggedImage>();

        // continuous acquisition --> retrieve next image acquired
        if (core.isSequenceRunning())
            return getNextTaggedImage();

        final int numChannel = (int) core.getNumberOfCameraChannels();
        final List<TaggedImage> result = new ArrayList<TaggedImage>();

        lock();
        try
        {
            // wait for camera to be ready
            core.waitForDevice(core.getCameraDevice());
            // manual snap
            core.snapImage();

            // get result
            for (int c = 0; c < numChannel; c++)
            {
                // this should not be poison image
                final TaggedImage image = core.getTaggedImage(c);

                result.add(image);

                if (image != null)
                {
                    // set channel index & number
                    MDUtils.setChannelIndex(image.tags, c);
                    MDUtils.setNumChannels(image.tags, numChannel);
                    // store metadata
                    metadatas.put(Integer.valueOf(c), image.tags);
                }
                else
                    metadatas.put(Integer.valueOf(c), null);
            }
        }
        finally
        {
            unlock();
        }

        return result;
    }

    /**
     * Use this method to know if the continuous acquisition (live mode) is running.
     */
    public static boolean isLiveRunning()
    {
        final CMMCore core = getCore();
        if (core == null)
            return false;

        return core.isSequenceRunning();
    }

    /**
     * Use this method to start the continuous acquisition mode (live mode) and retrieve images with
     * {@link LiveListener}.</br>
     * This command does not block the calling thread for the duration of the acquisition.
     * The GUI will draw the indeterminate progress bar representing a live acquisition running,
     * in the Running Acquisition tab panel.<br/>
     * <b>There is only one "live" progress bar for all plugin<b/>.<br/>
     * 
     * @throws Exception
     *         If a sequence acquisition is running (see {@link #startAcquisition(int, double)}) or
     *         if the core
     *         doesn't respond.
     * @see #stopLiveMode()
     * @see #addLiveListener(LiveListener)
     * @see #isLiveRunning()
     * @return <code>true</code> if the method actually started the live mode and <code>false</code>
     *         if it was already
     *         running.
     */
    public static boolean startLiveMode() throws Exception
    {
        final CMMCore core = MicroManager.getCore();
        if (core == null)
            return false;

        if (!core.isSequenceRunning())
        {
            lock();
            try
            {
                // start continuous acquisition (need to clear circular buffer first)
                core.clearCircularBuffer();
                core.startContinuousSequenceAcquisition(0d);
            }
            finally
            {
                unlock();
            }

            // notify about it
            getInstance().liveStarted();
            for (LiveListener l : getLiveListeners())
                l.liveStarted();

            return true;
        }

        return false;
    }

    /**
     * Use this method to stop the continuous acquisition mode (live mode).<br/>
     * The GUI will remove indeterminate progress bar representing a live acquisition running,
     * in the Running Acquisition tab panel, if live have been stopped. <br/>
     * 
     * @throws Exception
     *         If the core can't stop the continuous acquisition mode.
     */
    public static void stopLiveMode() throws Exception
    {
        final CMMCore core = MicroManager.getCore();
        if (core == null)
            return;

        if (core.isSequenceRunning())
        {
            lock();
            try
            {
                // stop continuous acquisition
                core.stopSequenceAcquisition();
            }
            finally
            {
                unlock();
            }

            // notify about it
            instance.liveStopped();
            for (LiveListener l : getLiveListeners())
                l.liveStopped();
        }
    }

    /**
     * Use this method to know if an acquisition is currently in process.
     * 
     * @see #startAcquisition(int, double)
     * @see #stopAcquisition()
     */
    public static boolean isAcquisitionRunning()
    {
        final AcquisitionEngine eng = getAcquisitionEngine();
        if (eng == null)
            return false;

        return eng.isAcquisitionRunning();
    }

    /**
     * Use this method to start an acquisition on the current camera device and retrieve images with
     * {@link AcquisitionListener}.</br>
     * This command does not block the calling thread for the duration of the acquisition.</br>
     * Note that you have to stop the live mode (see {@link #stopLiveMode()}) before calling this
     * method.
     * 
     * @param numImages
     *        Number of images requested from the camera
     * @param intervalMs
     *        The interval between images
     * @throws Exception
     *         if live is running or if a sequence acquisition have been started and is not finished
     *         yet.
     * @see #isAcquisitionRunning()
     * @see #getAcquisitionResult()
     * @see #stopAcquisition()
     */
    public static void startAcquisition(int numImages, double intervalMs) throws Exception
    {
        lock();
        try
        {
            /*
             * stopOnOverflow is manually set to false as we don't care if the circular buffer
             * is rewritten, because we capture each frame at the moment they have been put on the
             * buffer and we don't need them anymore.
             */
            getCore().startSequenceAcquisition(numImages, intervalMs, false);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * Use this method to interruption the current acquisition.
     * 
     * @see #isAcquisitionRunning()
     * @see #startAcquisition(int, double)
     */
    public static void stopAcquisition()
    {
        final AcquisitionEngine eng = getAcquisitionEngine();
        if (eng == null)
            return;

        if (eng.isAcquisitionRunning())
        {
            lock();
            try
            {
                eng.stop(true);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * If acquisition storage is enabled (see {@link #setStoreLastAcquisition(boolean)} method) then
     * this method will
     * return the list of sequence corresponding to the last sequence acquisition or
     * <code>null</code> if no acquisition
     * was done.</br>
     * Note you can have severals sequence for acquisition using different XY position.<br>
     * 
     * @see #startAcquisition(int, double)
     */
    public static List<Sequence> getAcquisitionResult()
    {
        if ((acquisitionManager == null) || acquisitionManager.getSequences().isEmpty())
            return null;

        return acquisitionManager.getSequences();
    }

    /**
     * Returns the current camera device name
     */
    public static String getCamera() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return "";

        return core.getCameraDevice();
    }

    /**
     * Get the exposure of the current camera
     */
    public static double getExposure() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return 0d;

        return core.getExposure();
    }

    /**
     * Set the exposure of the current camera
     */
    public static void setExposure(double exposure) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final MMStudio mmstudio = getMMStudio();
        if (mmstudio == null)
            return;

        // exposure actually changed ?
        if (core.getExposure() != exposure)
        {
            lock();
            try
            {
                // stop acquisition if needed
                stopAcquisition();

                // save continuous acquisition state
                final boolean liveRunning = isLiveRunning();
                // stop live
                if (liveRunning)
                    stopLiveMode();

                // better to use mmstudio method so it handles exposure synchronization
                mmstudio.setExposure(exposure);
                // // set new exposure
                // core.setExposure(exposure);

                // restore continuous acquisition
                if (liveRunning)
                    startLiveMode();
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Get the current camera binning mode (String format)
     */
    private static String getBinningAsString(CMMCore core, String camera) throws Exception
    {
        return core.getProperty(camera, MMCoreJ.getG_Keyword_Binning());
    }

    /**
     * Get the current camera binning mode (String format)
     */
    public static String getBinningAsString() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return "";

        final String camera = core.getCameraDevice();
        if (!StringUtil.isEmpty(camera))
            return getBinningAsString(core, camera);

        // default
        return "";
    }

    /**
     * Set the current camera binning mode (String format)
     */
    public static void setBinning(String value) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final String camera = core.getCameraDevice();
        if (!StringUtil.isEmpty(camera))
        {
            // binning changed ?
            if (getBinningAsString(core, camera) != value)
            {
                lock();
                try
                {
                    // stop acquisition if needed
                    stopAcquisition();

                    // save continuous acquisition state
                    final boolean liveRunning = isLiveRunning();
                    // stop live
                    if (liveRunning)
                        stopLiveMode();

                    // set new binning
                    core.waitForDevice(camera);
                    core.setProperty(camera, MMCoreJ.getG_Keyword_Binning(), value);

                    // restore continuous acquisition
                    if (liveRunning)
                        startLiveMode();
                }
                finally
                {
                    unlock();
                }
            }
        }
    }

    /**
     * Get the binning in integer format
     */
    private static int getBinningAsInt(String value, int def) throws Exception
    {
        // binning can be in "1;2;4" or "1x1;2x2;4x4" format...
        if (!StringUtil.isEmpty(value))
            // only use the first digit to get the binning as int
            return StringUtil.parseInt(value.substring(0, 1), 1);

        // default
        return def;
    }

    /**
     * Get the current camera binning mode
     */
    public static int getBinning() throws Exception
    {
        return getBinningAsInt(getBinningAsString(), 1);
    }

    /**
     * Set the current camera binning mode
     */
    public static void setBinning(int value) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final String camera = core.getCameraDevice();
        if (!StringUtil.isEmpty(camera))
        {
            // binning changed ?
            if (getBinningAsInt(getBinningAsString(core, camera), value) != value)
            {
                // get possible values
                final StrVector availableBinnings = core.getAllowedPropertyValues(camera,
                        MMCoreJ.getG_Keyword_Binning());

                lock();
                try
                {
                    // stop acquisition if needed
                    stopAcquisition();

                    // save continuous acquisition state
                    final boolean liveRunning = isLiveRunning();
                    // stop live
                    if (liveRunning)
                        stopLiveMode();

                    // set new binning
                    core.waitForDevice(camera);
                    for (String binningStr : availableBinnings)
                    {
                        // this is the String format for wanted int value ?
                        if (getBinningAsInt(binningStr, 0) == value)
                        {
                            core.setProperty(camera, MMCoreJ.getG_Keyword_Binning(), binningStr);
                            break;
                        }
                    }

                    // restore continuous acquisition
                    if (liveRunning)
                        startLiveMode();
                }
                finally
                {
                    unlock();
                }
            }
        }
    }

    /**
     * Returns the current shutter device.
     */
    public static String getShutter() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return "";

        return core.getShutterDevice();
    }

    /**
     * Sets the current shutter device.
     */
    public static void setShutter(String value) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        // value changed ?
        if (!StringUtil.equals(value, getShutter()))
        {
            lock();
            try
            {
                core.setShutterDevice(value);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Returns the current shutter device open state.
     */
    public static boolean isShutterOpen() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return false;

        return core.getShutterOpen();
    }

    /**
     * Open / close the current shutter device.
     */
    public static void setShutterOpen(boolean value) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        // value changed ?
        if (value != isShutterOpen())
        {
            lock();
            try
            {
                core.setShutterOpen(value);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Returns the current auto shutter state.
     */
    public static boolean getAutoShutter() throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return false;

        return core.getAutoShutter();
    }

    /**
     * Sets the auto shutter state.
     */
    public static void setAutoShutter(boolean value) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        // value changed ?
        if (value != getAutoShutter())
        {
            lock();
            try
            {
                // close shutter first then set auto shutter state
                core.setShutterOpen(false);
                core.setAutoShutter(value);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Returns current pixel configured pixel size in micro meter.<br/>
     * Returns 0d if pixel size is not defined
     */
    public static double getPixelSize()
    {
        final CMMCore core = getCore();
        if (core == null)
            return 0d;

        return core.getPixelSizeUm();
    }

    /**
     * Get all available config group
     */
    public static List<String> getConfigGroups()
    {
        final CMMCore core = getCore();
        if (core == null)
            return new ArrayList<String>();

        final StrVector list = core.getAvailableConfigGroups();
        final List<String> result = new ArrayList<String>((int) list.size());

        for (int i = 0; i < list.size(); i++)
            result.add(list.get(i));

        return result;
    }

    /**
     * Get all available config preset for the specified group
     */
    public static List<String> getConfigs(String group)
    {
        final CMMCore core = getCore();
        if (core == null)
            return new ArrayList<String>();

        if (StringUtil.isEmpty(group))
            return new ArrayList<String>();

        final StrVector list = core.getAvailableConfigs(group);
        final List<String> result = new ArrayList<String>((int) list.size());

        for (int i = 0; i < list.size(); i++)
            result.add(list.get(i));

        return result;
    }

    /**
     * Get the current config preset for the specified group
     */
    public static String getCurrentConfig(String group) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return "";

        if (StringUtil.isEmpty(group))
            return "";

        return getCore().getCurrentConfig(group);
    }

    /**
     * Set the specified preset for the given config group
     */
    public static void setConfigForGroup(String group, String preset, boolean wait) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        if (StringUtil.isEmpty(group) || StringUtil.isEmpty(preset))
            return;

        // config changed ?
        if (!getCurrentConfig(group).equals(preset))
        {
            lock();
            try
            {
                // stop acquisition if needed
                stopAcquisition();

                // save continuous acquisition state
                final boolean liveRunning = isLiveRunning();
                // stop live
                if (liveRunning)
                    stopLiveMode();

                core.setConfig(group, preset);
                if (wait)
                    core.waitForConfig(group, preset);

                // restore continuous acquisition
                if (liveRunning)
                    startLiveMode();
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Returns current channel group (camera / channel / objective...)
     */
    public static String getChannelGroup()
    {
        final CMMCore core = getCore();
        if (core == null)
            return "";

        return core.getChannelGroup();
    }

    /**
     * Set the channel group (camera / channel / objective...)
     */
    public static void setChannelGroup(String group) throws Exception
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        if (StringUtil.isEmpty(group))
            return;

        // channel group changed ?
        if (!getChannelGroup().equals(group))
        {
            lock();
            try
            {
                // stop acquisition if needed
                stopAcquisition();

                // save continuous acquisition state
                final boolean liveRunning = isLiveRunning();
                // stop live
                if (liveRunning)
                    stopLiveMode();

                core.setChannelGroup(group);
            }
            finally
            {
                unlock();
            }
        }
    }

    /**
     * Returns configurations for current channel group.
     * 
     * @see #getChannelGroup()
     * @see #getConfigs(String)
     */
    public static List<String> getChannelConfigs()
    {
        return getConfigs(MicroManager.getChannelGroup());
    }

    /**
     * Returns the "enable storage of last acquisition" state.
     * 
     * @see #setStoreLastAcquisition(boolean)
     * @see #getAcquisitionResult()
     */
    public static boolean getStoreLastAcquisition()
    {
        if (isInitialized())
            return instance.getStoreLastAcquisition();

        return false;
    }

    /**
     * Enable storage of last acquisition so it can be retrieved with
     * {@link #getAcquisitionResult()}.<br>
     * Set to <code>true</code> by default.
     * 
     * @see #getAcquisitionResult()
     */
    public static void setStoreLastAcquisition(boolean value)
    {
        if (isInitialized())
            instance.setStoreLastAcquisition(value);
    }

    /**
     * Enable immediate display of image acquisition.
     * 
     * @see #startAcquisition(int, double)
     */
    public static boolean getDisplayAcquisitionSequence()
    {
        if (isInitialized())
            return instance.getDisplayAcquisitionSequence();

        return false;
    }

    /**
     * Enable immediate display of image acquisition.
     * 
     * @see #startAcquisition(int, double)
     */
    public static void setDisplayAcquisitionSequence(boolean value)
    {
        if (isInitialized())
            instance.setDisplayAcquisitionSequence(value);
    }

    /**
     * Use this method to enable logging (disabled by default).<br>
     * Log file are in Icy folder and are named this way : CoreLog20140515.txt ; 20140515 is the
     * date of debugging (2014/05/15)<br/>
     * <b>Be careful, log files can be very huge and take easily 1Gb.</b>
     * 
     * @param enable
     */
    public static void enableDebugLogging(boolean enable)
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        core.enableDebugLog(enable);
        core.enableStderrLog(enable);
    }

    /**
     * Returns <code>true</code> if Micro-Manager is initialized / loaded.<br>
     * 
     * @see MicromanagerPlugin#init()
     */
    public static boolean isInitialized()
    {
        return instance != null;
    }

    // /**
    // * Returns <code>true</code> if currently initializing / loading the Micro-Manager library
    // *
    // * @see #isInitialized()
    // * @see #init()
    // */
    // public static boolean isInitializing()
    // {
    // return initializing;
    // }

    /**
     * For internal use only (this method should never be called directly).
     * Use {@link MicromanagerPlugin#init()} instead.
     */
    public static synchronized void init()
    {
        // sometime a frame can ask to re-init when Icy is exiting, just ignore...
        if (Icy.isExiting())
            return;

        // already initialized --> show the frame and return it
        if (instance != null)
        {
            instance.setVisible(true);
            instance.toFront();
            return;
        }

        try
        {
            Version version = null;

            try
            {
                // try to get version
                version = getMMVersion();
                // force to load MMStudio class
                ClassUtil.findClass(MMStudio.class.getName());
            }
            catch (Throwable t)
            {
                // an fatal error occurred, force error on version checking then...
                version = new Version("1");
            }

            // cannot get version or wrong version ?
            if ((version == null) || version.isLower(new Version("1.4.19")))
            // || version.isGreater(new Version("1.4.22")))
            {
                MessageDialog.showDialog("Error while loading Micro-Manager",
                        "Your version of Micro-Manager seems to not be compatible !\n"
                                + "This plugin is only compatible with version 1.4.19 or above.\n"
                                + "Also check that you are using the same architecture for Icy and Micro-Manager (32/64 bits)\n"
                                + "You need to restart Icy to redefine the Micro-Manager folder.",
                        MessageDialog.ERROR_MESSAGE);
                // so user can change the defined MM folder
                MMUtils.resetLibrayPath();
                return;
            }

            // show config selection dialog and exit if user canceled operation
            if (!showConfigSelection())
                return;

            // show loading message
            final LoadingFrame loadingFrame = new LoadingFrame(
                    "  Please wait while loading Micro-Manager, Icy interface may not respond...  ");
            loadingFrame.show();
            try
            {
                try
                {
                    // create main frame (here we are initialized)
                    instance = new MMMainFrame();
                }
                catch (Throwable e)
                {
                    IcyExceptionHandler.showErrorMessage(e, true, true);
                    MessageDialog.showDialog("Error while loading Micro-Manager",
                            e.getMessage() + "\nYou may try to restart Icy to fix the issue.",
                            MessageDialog.ERROR_MESSAGE);
                    return;
                }

                // get the MM core
                final CMMCore core = getCore();

                // set core for reporting
                ReportingUtils.setCore(core);

                try
                {
                    // initialize circular buffer (only if a camera is present)
                    if (!StringUtil.isEmpty(core.getCameraDevice()))
                        core.initializeCircularBuffer();
                }
                catch (Throwable e)
                {
                    throw new Exception("Error while initializing circular buffer of Micro Manager", e);
                }

                ThreadUtil.invokeNow(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // In AWT Thread because it creates a JComponent
                        getAcquisitionEngine().addImageProcessor(new ImageAnalyser());
                    }
                });

                // load StageMover preferences
                StageMover.loadPreferences(instance.getPreferences().node("StageMover"));

                // separate initialization which may require getInstance()
                instance.init();
                // this happen when micro manager cannot correctly load config file
                if (!instance.isVisible())
                    throw new Exception("Could not load configuration file.");

                // live event thread
                liveManager = new LiveListenerThread();
                liveManager.start();

                // put it on front
                instance.toFront();

                // cleanup old MM plugins
                cleanOldMM();
            }
            catch (Throwable e)
            {
                IcyExceptionHandler.showErrorMessage(e, true, true);
                new FailedAnnounceFrame(
                        "An error occured while initializing Micro-Manager (see console output for more details).");

                // shutdown everything
                shutdown();
            }
            finally
            {
                loadingFrame.close();
            }
        }
        catch (Throwable t)
        {
            // cannot load class --> version mismatch probably
            MessageDialog.showDialog("Cannot load Micro-Manager",
                    "Your version of Micro-Manager seems to not be compatible !\n"
                            + "This plugin is only compatible with version 1.4.19 or above.\n"
                            + "Also check that you are using the same architecture for Icy and Micro-Manager (32/64 bits).",
                    MessageDialog.ERROR_MESSAGE);
        }
    }

    private static void cleanOldMM()
    {
        final String baseFolder = FileUtil.getApplicationDirectory() + "/plugins/tprovoost/Microscopy";

        if (FileUtil.exists(baseFolder))
        {
            // remove advanced acquisition plugin
            FileUtil.delete(baseFolder + "/MicroscopeAdvancedAcquisition", true);
            // remove live 2D plugin
            FileUtil.delete(baseFolder + "/MicroscopeLive", true);
            // remove live 3D plugin
            FileUtil.delete(baseFolder + "/microscopelive3d", true);
            // remove snapper plugin
            FileUtil.delete(baseFolder + "/MicroscopeSnapper", true);
            // remove old blocks plugin
            FileUtil.delete(baseFolder + "/blocks", true);
        }
    }

    private static boolean showConfigSelection()
    {
        final AtomicBoolean configLoaded = new AtomicBoolean(false);

        // need to be
        ThreadUtil.invokeNow(new Runnable()
        {
            @Override
            public void run()
            {
                final LoadFrame f = new LoadFrame();
                final int res = f.showDialog();

                switch (res)
                {
                    // no error ?
                    case 0:
                        setDefaultConfigFileName(f.getConfigFilePath());
                        Preferences.userNodeForPackage(MMStudio.class).put("sysconfig_file", f.getConfigFilePath());
                        configLoaded.set(true);
                        break;

                    // cancel
                    case 1:
                        break;

                    // error
                    default:
                        new FailedAnnounceFrame("Error while loading configuration file, please restart Micro-Manager.",
                                2);
                        break;
                }
            }
        });

        // return true if config was correctly loaded
        return configLoaded.get();
    }

    /**
     * Stop all Micro-Manager activities
     */
    public static void shutdown()
    {
        // do that only if instance if initialized (otherwise it will init again)
        if (isInitialized())
        {
            try
            {
                // stop acquisition and live mode...
                stopAcquisition();
                stopLiveMode();
            }
            catch (Throwable t)
            {
                IcyExceptionHandler.showErrorMessage(t, true);
            }
        }

        if (liveListeners != null)
            liveListeners.clear();
        if (acqListeners != null)
            acqListeners.clear();
        StageMover.clearListener();
        if (metadatas != null)
            metadatas.clear();

        // stop live listener
        if (liveManager != null)
        {
            liveManager.interrupt();

            try
            {
                // wait until thread ended
                liveManager.join();
            }
            catch (InterruptedException e)
            {
                // ignore
            }

            liveManager = null;
        }

        // need to release everything
        if (instance != null)
        {
            // force main frame close (will call 'mainFrame.onClosed()' method which do internal
            // shutdown)
            instance.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            instance.close();
        }

        instance = null;
        acquisitionManager = null;
    }

    // custom TaggedImageAnalyzer so we have events for new image
    private static class ImageAnalyser extends TaggedImageAnalyzer
    {
        ImageAnalyser()
        {
            super();
        }

        @Override
        protected void analyze(TaggedImage image)
        {
            final List<AcquisitionListener> listeners = getAcquisitionListeners();

            try
            {
                // no more image or last one ?
                if ((image == null) || TaggedImageQueue.isPoison(image))
                {
                    if (acquisitionManager != null)
                        acquisitionManager.done();

                    // send acquisition ended event
                    for (AcquisitionListener l : listeners)
                        l.acquisitionFinished(getAcquisitionResult());

                    // done
                    return;
                }

                final JSONObject tags = image.tags;

                boolean firstImage = (MDUtils.getPositionIndex(tags) == 0) && (MDUtils.getFrameIndex(tags) == 0)
                        && (MDUtils.getChannelIndex(tags) == 0) && (MDUtils.getSliceIndex(tags) == 0);
                boolean newAcquisition = (acquisitionManager == null) || acquisitionManager.isDone();

                // first acquisition image or new acquisition --> create the new acquisition
                if (firstImage || newAcquisition)
                {
                    // end previous acquisition
                    if (!newAcquisition)
                    {
                        acquisitionManager.done();

                        // send acquisition ended event
                        for (AcquisitionListener l : listeners)
                            l.acquisitionFinished(getAcquisitionResult());
                    }

                    final SequenceSettings settings = getAcquisitionSettings();
                    final JSONObject metadata = getAcquisitionMetaData();

                    // create the acquisition manager
                    acquisitionManager = new AcquisitionResult(settings, metadata);

                    // send acquisition started event
                    for (AcquisitionListener l : listeners)
                        l.acquisitionStarted(settings, metadata);
                }

                // store image in acquisition manager only if storage is enabled
                if (getStoreLastAcquisition())
                    acquisitionManager.imageReceived(image);

                // send image received event
                for (AcquisitionListener l : listeners)
                    l.acqImgReveived(image);
            }
            catch (Exception e)
            {
                IcyExceptionHandler.showErrorMessage(e, true);
            }
        }
    }

    private static class LiveListenerThread extends Thread
    {
        public LiveListenerThread()
        {
            super("uManager - LiveListener");
        }

        @Override
        public void run()
        {
            while (!isInterrupted())
            {
                final CMMCore core = getCore();

                // running and we have a new image in the queue ?
                while (core.isSequenceRunning() && (core.getRemainingImageCount() > 0))
                {
                    try
                    {
                        final List<TaggedImage> taggedImages;

                        lock();
                        try
                        {
                            // retrieve the last image
                            taggedImages = getLastTaggedImage();

                            // not empty --> remove it from the queue
                            if (!taggedImages.isEmpty())
                            {
                                try
                                {
                                    // acquisition may consume the image in the mean time
                                    if (!isAcquisitionRunning() && core.getRemainingImageCount() > 0)
                                        core.popNextImage();
                                }
                                catch (Exception e)
                                {
                                    // can happen with advanced acquisition set with a lower time
                                    // interval than current exposure time
                                    IcyExceptionHandler.showErrorMessage(e, true);
                                }
                            }
                        }
                        finally
                        {
                            unlock();
                        }

                        if (!taggedImages.isEmpty())
                        {
                            // send image received event
                            for (LiveListener l : getLiveListeners())
                                l.liveImgReceived(taggedImages);
                        }
                    }
                    catch (Exception e)
                    {
                        // should not happen
                        IcyExceptionHandler.showErrorMessage(e, true);
                    }
                }

                // sleep a bit to free some CPU time
                ThreadUtil.sleep(1);
            }
        }
    }
}
