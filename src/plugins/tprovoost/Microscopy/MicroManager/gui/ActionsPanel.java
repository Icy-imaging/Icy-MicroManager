package plugins.tprovoost.Microscopy.MicroManager.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.json.JSONObject;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.utils.ReportingUtils;

import icy.gui.component.button.IcyButton;
import icy.gui.component.button.IcyToggleButton;
import icy.main.Icy;
import icy.math.FPSMeter;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import icy.system.IcyExceptionHandler;
import icy.util.DateUtil;
import icy.util.StringUtil;
import mmcorej.CMMCore;
import mmcorej.TaggedImage;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.event.LiveListener;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

public class ActionsPanel extends JPanel implements LiveListener, SequenceListener
{
    /**
     * 
     */
    private static final long serialVersionUID = -1799340043444887652L;

    final MMMainFrame mainFrame;

    // internals sequence
    Sequence liveSequence;
    Sequence albumSequence;

    // internals
    final FPSMeter fpsMeter;
    StackAcquisitionProcessor stackAcquisitionProcessor;
    long liveDate;
    long albumDate;

    // GUI
    private IcyButton snapBtn;
    private IcyToggleButton liveBtn;
    private IcyButton albumBtn;
    private IcyButton advAcqBtn;
    private IcyButton refreshBtn;

    /**
     * Create the panel.
     */
    public ActionsPanel(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;

        // use lazy creation (nice for the windows designer)
        liveSequence = null;
        liveDate = 0L;
        albumSequence = null;
        albumDate = 0L;

        fpsMeter = new FPSMeter();
        stackAcquisitionProcessor = null;

        initialize();

        snapBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                doSnap();
            }
        });
        liveBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setLiveMode(((IcyToggleButton) e.getSource()).isSelected());
            }
        });
        albumBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                doAlbumSnap();
            }
        });
        advAcqBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                startAdvAcq();
            }
        });
        refreshBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                refreshGUI();
            }
        });

        MicroManager.addLiveListener(this);
    }

    private void initialize()
    {
        setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Actions", TitledBorder.LEADING,
                TitledBorder.TOP, null, new Color(0, 0, 0)));

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {80, 0};
        gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[] {1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        snapBtn = new IcyButton("Snap", new IcyIcon(ResourceUtil.ICON_PHOTO));
        snapBtn.setToolTipText("Snap an image");
        snapBtn.setIconTextGap(12);
        GridBagConstraints gbc_snapBtn = new GridBagConstraints();
        gbc_snapBtn.fill = GridBagConstraints.BOTH;
        gbc_snapBtn.insets = new Insets(0, 0, 5, 0);
        gbc_snapBtn.gridx = 0;
        gbc_snapBtn.gridy = 0;
        add(snapBtn, gbc_snapBtn);

        liveBtn = new IcyToggleButton("Live", new IcyIcon("camera"));
        liveBtn.setToolTipText("Enable / Disable the live display");
        liveBtn.setIconTextGap(12);
        GridBagConstraints gbc_liveBtn = new GridBagConstraints();
        gbc_liveBtn.fill = GridBagConstraints.BOTH;
        gbc_liveBtn.insets = new Insets(0, 0, 5, 0);
        gbc_liveBtn.gridx = 0;
        gbc_liveBtn.gridy = 1;
        add(liveBtn, gbc_liveBtn);

        albumBtn = new IcyButton("Album", new IcyIcon("movie"));
        albumBtn.setToolTipText("Snap an image and store it in the album");
        albumBtn.setIconTextGap(12);
        GridBagConstraints gbc_albumBtn = new GridBagConstraints();
        gbc_albumBtn.fill = GridBagConstraints.BOTH;
        gbc_albumBtn.insets = new Insets(0, 0, 5, 0);
        gbc_albumBtn.gridx = 0;
        gbc_albumBtn.gridy = 2;
        add(albumBtn, gbc_albumBtn);

        advAcqBtn = new IcyButton("Multi-D Acq.", new IcyIcon(ResourceUtil.ICON_LAYER_V1));
        advAcqBtn.setToolTipText("Multi dimension acquisition");
        advAcqBtn.setIconTextGap(8);
        GridBagConstraints gbc_advAcqBtn = new GridBagConstraints();
        gbc_advAcqBtn.insets = new Insets(0, 0, 5, 0);
        gbc_advAcqBtn.fill = GridBagConstraints.BOTH;
        gbc_advAcqBtn.gridx = 0;
        gbc_advAcqBtn.gridy = 3;
        add(advAcqBtn, gbc_advAcqBtn);

        refreshBtn = new IcyButton("Refresh", new IcyIcon(ResourceUtil.ICON_RELOAD));
        refreshBtn.setToolTipText("Force GUI refresh");
        refreshBtn.setIconTextGap(12);
        GridBagConstraints gbc_refreshBtn = new GridBagConstraints();
        gbc_refreshBtn.fill = GridBagConstraints.BOTH;
        gbc_refreshBtn.gridx = 0;
        gbc_refreshBtn.gridy = 4;
        add(refreshBtn, gbc_refreshBtn);
    }

    public void shutdown()
    {
        MicroManager.removeLiveListener(this);
    }

    MMStudio getMMStudio()
    {
        return mainFrame.mmstudio;
    }

    CMMCore getCore()
    {
        return getMMStudio().getCore();
    }

    public void lock()
    {
        mainFrame.lock();
    }

    public void unlock()
    {
        mainFrame.unlock();
    }

    public void logError(Exception e)
    {
        ReportingUtils.logError(e);
    }

    public void logError(Exception e, String msg)
    {
        ReportingUtils.logError(e, msg);
    }

    public void setLiveMode(boolean enable)
    {
        prepareSequences();

        if (enable)
        {
            try
            {
                MicroManager.startLiveMode();
                if (!Icy.getMainInterface().isOpened(liveSequence))
                    Icy.getMainInterface().addSequence(liveSequence);
            }
            catch (Throwable t)
            {
                IcyExceptionHandler.handleException(t, true);
            }
        }
        else
        {
            try
            {
                MicroManager.stopLiveMode();
            }
            catch (Throwable t)
            {
                IcyExceptionHandler.handleException(t, true);
            }
        }
    }

    public void doSnap()
    {
        final boolean liveRunning = MicroManager.isLiveRunning();
        final Sequence sequence = new Sequence("Image snap - " + getDateString());
        final LiveSettingsPanel livePanel = mainFrame.livePanel;

        try
        {
            if (liveRunning)
                MicroManager.stopLiveMode();

            // 3D acquisition ?
            if (livePanel.isZStackAcquisition())
            {
                final double start = livePanel.getZStart();
                final double end = livePanel.getZEnd();
                final double step = livePanel.getZStep();

                // previous acquisition not yet done --> interrupt
                if ((stackAcquisitionProcessor != null) && stackAcquisitionProcessor.isAlive())
                    return;

                // start a new acquisition to set in the new Sequence
                stackAcquisitionProcessor = new StackAcquisitionProcessor(sequence, start, end, step,
                        Calendar.getInstance().getTimeInMillis());
            }
            else
            {
                final long tstart = Calendar.getInstance().getTimeInMillis();

                // set image and associated metadata
                for (TaggedImage image : MicroManager.snapTaggedImage())
                    MMUtils.setImage(sequence, image, tstart);
            }

            // get acquisition metadata
            final JSONObject metadata = MicroManager.getAcquisitionMetaData();
            // exist ? --> use them to fill some extras informations
            if (metadata != null)
                MMUtils.setMetadata(sequence, metadata);

            Icy.getMainInterface().addSequence(sequence);
        }
        catch (Throwable t)
        {
            IcyExceptionHandler.handleException(t, true);
        }
        finally
        {
            try
            {
                if (liveRunning)
                    MicroManager.startLiveMode();
            }
            catch (Throwable t)
            {
                IcyExceptionHandler.handleException(t, true);
            }
        }
    }

    public void doAlbumSnap()
    {
        prepareSequences();

        final LiveSettingsPanel livePanel = mainFrame.livePanel;
        final boolean liveRunning = MicroManager.isLiveRunning();

        if (albumSequence.isEmpty())
        {
            albumSequence.setName("Album - " + getDateString());
            albumDate = Calendar.getInstance().getTimeInMillis();
        }
        try
        {
            if (liveRunning)
                MicroManager.stopLiveMode();

            // 3D acquisition ?
            if (livePanel.isZStackAcquisition())
            {
                final double start = livePanel.getZStart();
                final double end = livePanel.getZEnd();
                final double step = livePanel.getZStep();

                // previous acquisition not yet done --> interrupt
                if ((stackAcquisitionProcessor != null) && stackAcquisitionProcessor.isAlive())
                    return;

                // start a new acquisition to set in album Sequence
                stackAcquisitionProcessor = new StackAcquisitionProcessor(albumSequence, start, end, step, albumDate);
            }
            else
            {
                // snap an image and put it on next frame of Album
                for (TaggedImage image : MicroManager.snapTaggedImage())
                {
                    final int t = albumSequence.getSizeT();
                    // set image position metadata
                    MMUtils.setImageMetadata(image, t, 0, -1, 1, t + 1, -1);
                    // set image in album
                    MMUtils.setImage(albumSequence, image, albumDate);
                }
            }
        }
        catch (Throwable t)
        {
            IcyExceptionHandler.handleException(t, true);
        }
        finally
        {
            try
            {
                if (liveRunning)
                    MicroManager.startLiveMode();
            }
            catch (Throwable t)
            {
                IcyExceptionHandler.handleException(t, true);
            }
        }

        if (!Icy.getMainInterface().isOpened(albumSequence))
            Icy.getMainInterface().addSequence(albumSequence);
    }

    public void startAdvAcq()
    {
        // just show advanced acquisition frame
        mainFrame.getAcquisitionHandler().getAcquisitionFrame().setVisible(true);
    }

    public void refreshGUI()
    {
        mainFrame.refreshConfigs();
        mainFrame.refreshGUI();
    }

    static String getDateString()
    {
        return DateUtil.now("yyyy-MM-dd HH'h'mm'm'ss's'");
    }

    void prepareSequences()
    {
        // lazy creation
        if (liveSequence == null)
        {
            liveSequence = new Sequence("Live mode");
            liveSequence.addListener(this);
        }
        if (albumSequence == null)
        {
            albumSequence = new Sequence("Album");
            albumSequence.addListener(this);
        }
    }

    @Override
    public void liveImgReceived(List<TaggedImage> images)
    {
        prepareSequences();

        liveSequence.beginUpdate();
        try
        {
            // format not anymore compatible --> need to clear sequence first
            if (!MMUtils.isCompatible(liveSequence, images.get(0).tags))
                liveSequence.removeAllImages();

            final LiveSettingsPanel livePanel = mainFrame.livePanel;

            // 3D live ?
            if (livePanel.isZStackAcquisition())
            {
                final double start = livePanel.getZStart();
                final double end = livePanel.getZEnd();
                final double step = livePanel.getZStep();

                // be sure the Z stage device finish its movement
                StageMover.waitZToRespond();
                StageMover.waitZMoving();
                final double curZ = StageMover.getZ();

                // no more than 100 slices
                final int numSlice = Math.min(1 + (int) Math.floor((end - start) / step), 100);

                // number of slices changed ? --> clear the sequence
                if (liveSequence.getSizeZ() != numSlice)
                    liveSequence.removeAllImages();

                // get current Z position
                final int z = (int) Math.round((curZ - start) / step);

                // we limit the Z position
                if ((z >= 0) && (z < 100))
                {
                    // update live image
                    for (TaggedImage img : images)
                    {
                        // set image position metadata
                        MMUtils.setImageMetadata(img, 0, z, -1, 1, numSlice, -1);
                        // set image in live sequence
                        MMUtils.setImage(liveSequence, img, liveDate);
                    }
                }

                final double limit = end - (step / 2d);
                final boolean done = (step < 0) ^ (curZ > limit);

                if (done)
                    StageMover.moveZAbsolute(start, false);
                else
                    StageMover.moveZAbsolute(curZ + step, false);

                // only on first slice
                if (z == 0)
                {
                    // refresh FPS
                    fpsMeter.update();
                    liveSequence.setName("Live mode - " + ((int) (fpsMeter.getRate() * 60d)) + " stack(s) per minute");
                }
            }
            else
            {
                // number of slices changed ? --> clear the sequence
                if (liveSequence.getSizeZ() > 1)
                    liveSequence.removeAllImages();

                // update live image
                for (TaggedImage img : images)
                {
                    // set image position metadata
                    MMUtils.setImageMetadata(img, 0, 0, -1, -1, -1, -1);
                    // set image in live sequence
                    MMUtils.setImage(liveSequence, img, liveDate);
                }

                // refresh FPS
                fpsMeter.update();
                liveSequence.setName("Live mode - " + fpsMeter.getFPS() + " frame(s) per second");
            }
        }
        catch (Exception e)
        {
            System.err.println("MicroManager: cannot update live image.");
            IcyExceptionHandler.showErrorMessage(e, false, true);
        }
        finally
        {
            liveSequence.endUpdate();
        }
    }

    @Override
    public void liveStarted()
    {
        liveDate = Calendar.getInstance().getTimeInMillis();

        // refresh live button state
        liveBtn.setSelected(true);
    }

    @Override
    public void liveStopped()
    {
        // refresh live button state
        liveBtn.setSelected(false);
    }

    @Override
    public void sequenceChanged(SequenceEvent sequenceEvent)
    {
        // ignore this event

    }

    @Override
    public void sequenceClosed(Sequence sequence)
    {
        // stop live mode if user closed sequence
        if (sequence == liveSequence)
            setLiveMode(false);

        if (sequence == albumSequence)
        {
            // album not saved ?
            if (StringUtil.isEmpty(sequence.getFilename()))
            {
                // ask user if he want to save it

            }

            // clear it
            sequence.removeAllImages();
        }
    }

    static class StackAcquisitionProcessor extends Thread
    {
        final Sequence dest;
        final double zstart;
        final double zend;
        final double zstep;
        final long tstart;

        public StackAcquisitionProcessor(Sequence dest, double zstart, double zend, double zstep, long tstart)
        {
            super("Z-Stack acquisition");

            this.dest = dest;
            this.zstart = zstart;
            this.zend = zend;
            this.zstep = zstep;
            this.tstart = tstart;

            start();
        }

        @Override
        public void run()
        {
            final int t = dest.getSizeT();
            // no more than 100 slices
            final int numSlice = Math.min(1 + (int) Math.floor((zend - zstart) / zstep), 100);

            try
            {
                double zpos = zstart;
                for (int z = 0; z < numSlice; z++)
                {
                    StageMover.moveZAbsolute(zpos, true);
                    StageMover.waitZMoving();

                    for (TaggedImage img : MicroManager.snapTaggedImage())
                    {
                        // set image metadata
                        MMUtils.setImageMetadata(img, t, z, -1, t + 1, numSlice, -1);
                        // then set image in sequence
                        MMUtils.setImage(dest, img, tstart);
                    }

                    zpos += zstep;
                }
            }
            catch (Exception e)
            {
                System.err.println("MicroManager: cannot process stack acquisition.");
                IcyExceptionHandler.handleException(e, true);
            }
        }
    }
}
