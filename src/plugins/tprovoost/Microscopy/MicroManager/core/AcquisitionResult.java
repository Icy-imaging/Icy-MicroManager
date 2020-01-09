package plugins.tprovoost.Microscopy.MicroManager.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.api.SequenceSettings;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.MMScriptException;

import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.util.DateUtil;
import icy.util.StringUtil;
import mmcorej.TaggedImage;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;

/**
 * This class is to be used by AcquisitionListener.</br>
 * It initializes an empty icy sequence you need to feed with the {@link #imageReceived(TaggedImage)} method so the
 * sequence will be updated with those images.
 * 
 * @author Stephane Dallongeville
 */
public class AcquisitionResult
{
    protected final Map<Integer, Sequence> sequences;
    protected final SequenceSettings settings;
    protected final JSONObject summaryMetadata;
    protected final long startTime;
    protected boolean done;

    public AcquisitionResult(SequenceSettings settings, JSONObject summaryMetadata)
    {
        this.settings = settings;
        this.summaryMetadata = summaryMetadata;
        sequences = new HashMap<Integer, Sequence>();
        startTime = Calendar.getInstance().getTimeInMillis();
        done = false;
    }

    public List<Sequence> getSequences()
    {
        return new ArrayList<Sequence>(sequences.values());
    }

    public void imageReceived(TaggedImage taggedImage) throws JSONException, MMScriptException
    {
        final JSONObject tags = taggedImage.tags;
        final Integer position = Integer.valueOf(MDUtils.getPositionIndex(tags));

        Sequence seq = sequences.get(position);
        if (seq == null)
        {
            final String positionName = MDUtils.getPositionName(tags);

            // create a new sequence
            seq = new Sequence("Acquisition - " + (!StringUtil.isEmpty(positionName) ? positionName + " - " : "")
                    + DateUtil.now("yyyy-MM-dd HH'h'mm'm'ss's'"));
            sequences.put(position, seq);

            // display enabled ?
            if (MicroManager.getDisplayAcquisitionSequence())
                Icy.getMainInterface().addSequence(seq);
        }

        // set image
        MMUtils.setImage(seq, taggedImage, startTime);

        // first image ? --> try to get more informations from summary metadata
        if (seq.getNumImage() == 1)
            MMUtils.setMetadata(seq, summaryMetadata);

        // get the viewer
        final Viewer viewer = seq.getFirstViewer();

        if (viewer != null)
        {
            final int t = MDUtils.getFrameIndex(tags);
            final int z = MDUtils.getSliceIndex(tags);

            // synchronize viewer on acquisition
            viewer.setPositionT(t);
            viewer.setPositionZ(z);
        }
    }

    public boolean isDone()
    {
        return done;
    }

    public void done()
    {
        done = true;

        // complete some metadata
        for (Sequence seq : sequences.values())
        {
            // no time interval info ? -> set it from time position
            // if (seq.getTimeInterval() == 0d)
            // seq.setTimeInterval(MetaDataUtil.getTimeintervalFromTimePositions());
        }
    }
}