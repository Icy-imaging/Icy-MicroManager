package plugins.tprovoost.Microscopy.MicroManager.event;

import icy.sequence.Sequence;

import java.util.List;

import mmcorej.TaggedImage;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;

/**
 * @see MicroManager#addLiveListener(LiveListener)
 * @author Irsath Nguyen
 */
public interface LiveListener
{
    /**
     * Called when a new image is captured by the core when live mode is on.<br/>
     * Note that the new image properties may have changed (such as the image size, binning,
     * exposure...)<br/>
     * Note that you can easily convert TaggedImage to Icy image by using
     * {@link MMUtils#setImage(Sequence, TaggedImage)} or {@link MMUtils#convertToIcyImage(List)} methods.
     * 
     * @param images
     *        the last images received from live (one image per camera channel)
     * @see MicroManager#startLiveMode()
     * @see MicroManager#stopLiveMode()
     * @see MicroManager#isLiveRunning()
     * @see MMUtils#setImage(Sequence, TaggedImage)
     * @see MMUtils#convertToIcyImage(List)
     */
    public void liveImgReceived(List<TaggedImage> images);

    /**
     * Notify that live have just been started.
     * 
     * @see MicroManager#startLiveMode()
     */
    public void liveStarted();

    /**
     * Notify that live have just been stopped.
     * 
     * @see MicroManager#stopLiveMode()
     */
    public void liveStopped();
}