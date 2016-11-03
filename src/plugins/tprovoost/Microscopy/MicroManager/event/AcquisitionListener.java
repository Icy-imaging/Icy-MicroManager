package plugins.tprovoost.Microscopy.MicroManager.event;

import icy.sequence.Sequence;

import java.util.List;

import mmcorej.TaggedImage;

import org.json.JSONObject;
import org.micromanager.api.SequenceSettings;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

/**
 * @see MicroManager#addAcquisitionListener(AcquisitionListener)
 * @author Irsath Nguyen
 */
public interface AcquisitionListener
{
    /**
     * Called when a new image is captured from a core sequence acquisition (not continuous
     * sequence acquisition).</br>
     * You can retrieve the current global acquisition result by using {@link MicroManager#getAcquisitionResult()} if
     * acquisition storage is enabled.
     * 
     * @param image
     *        The tagged image retrieved from the core.
     * @see MMUtils#setImage(Sequence, TaggedImage)
     * @see MMUtils#convertToIcyImage(TaggedImage)
     * @see MicroManager#getAcquisitionResult()
     * @see MicroManager#setStoreLastAcquisition(boolean)
     */
    public void acqImgReveived(TaggedImage image);

    /**
     * Callback when {@link MicroManager#startAcquisition(int, double)} have
     * been called by a {@link MicroscopePlugin}
     */
    public void acquisitionStarted(SequenceSettings settings, JSONObject metadata);

    /**
     * Callback when an acquisition started by {@link MicroManager#startAcquisition(int, double)} have been completed
     * 
     * @param result
     *        Sequences which were acquired.<br>
     *        You can have several sequence if the acquisition was multiple XY position.<br>
     *        Note that result is <code>null</code> if acquisition storage is disabled (see
     *        {@link MicroManager#setStoreLastAcquisition(boolean)} )
     * @see MicroManager#getAcquisitionResult()
     * @see MicroManager#setStoreLastAcquisition(boolean)
     */
    public void acquisitionFinished(List<Sequence> result);
}