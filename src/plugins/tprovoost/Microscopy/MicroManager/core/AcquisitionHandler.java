package plugins.tprovoost.Microscopy.MicroManager.core;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.util.GuiUtil;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.system.thread.ThreadUtil;
import icy.util.ReflectionUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import mmcorej.TaggedImage;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.api.SequenceSettings;
import org.micromanager.dialogs.AcqControlDlg;
import org.micromanager.utils.MDUtils;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.event.AcquisitionListener;
import plugins.tprovoost.Microscopy.MicroManager.gui.MMMainFrame;
import plugins.tprovoost.Microscopy.MicroManager.tools.FrameUtils;

public class AcquisitionHandler implements AcquisitionListener
{
    // advanced acquisition frame
    AcqControlDlg advAcqDialog;
    IcyFrame advAcqFrame;

    // internals
    JLabel progressLabel;
    JProgressBar progressBar;
    JButton startButton;
    int progressIndex;
    boolean liveModeWasRunning;

    public AcquisitionHandler(MMMainFrame mainFrame)
    {
        super();

        initDialog(mainFrame);

        // we want to listen acquisition events
        MicroManager.addAcquisitionListener(this);
    }

    private void initDialog(MMMainFrame mainFrame)
    {
        final MMStudio mmstudio = mainFrame.getMMStudio();

        try
        {
            advAcqDialog = (AcqControlDlg) ReflectionUtil.getFieldObject(mmstudio, "acqControlWin_", true);
        }
        catch (Exception ex)
        {
            System.err.println("Warning: cannot retrieve AcqControlDlg from Micro-Manager.");
        }

        // not existing yet ? --> create it now
        if (advAcqDialog == null)
            advAcqDialog = new AcqControlDlg(mmstudio.getAcquisitionEngine(), mainFrame.getMainPreferences(), mmstudio,
                    mainFrame.getOptions());

        // remove some of the default window listener
        final WindowListener[] winListeners = advAcqDialog.getWindowListeners();
        // start from 1 as we want to keep first listener
        for (int i = 1; i < winListeners.length; i++)
            advAcqDialog.removeWindowListener(winListeners[i]);

        // create the Icy frame version
        advAcqFrame = GuiUtil.createIcyFrameFromWindow(advAcqDialog);
        // just hide on close
        advAcqFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        // add custom ui component
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setSize(450, 30);
        progressLabel = new JLabel("No acquisition started : ");

        final JPanel southPanel = new JPanel();
        southPanel.add(progressLabel);
        southPanel.add(progressBar);
        // add a south panel for progress bar
        advAcqFrame.add(southPanel, BorderLayout.SOUTH);

        advAcqFrame.addFrameListener(new IcyFrameAdapter()
        {
            @Override
            public void icyFrameClosing(IcyFrameEvent e)
            {
                try
                {
                    // stop the acquisition (need to check that we are still initialized)
                    if (MicroManager.isInitialized())
                        MicroManager.stopAcquisition();
                }
                catch (Throwable t)
                {
                    // may fail if MM core shutdown at same time
                }
            }
        });

        // find original 'start' button (take from dialog as frame has a sub panel)
        startButton = FrameUtils.findButtonComponents(advAcqDialog.getContentPane(), "Acquire!");

        // found the original start button
        if (startButton != null)
        {
            // get original listeners
            final ActionListener[] listeners = startButton.getActionListeners();

            // remove them
            for (ActionListener l : listeners)
                startButton.removeActionListener(l);

            // add our own listener on start action
            startButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(final ActionEvent arg0)
                {
                    // first do our own stuff
                    advancedAcquisitionStartPressed();

                    // then forward originals events
                    for (ActionListener l : listeners)
                        l.actionPerformed(arg0);
                }
            });
        }

        // find original 'close' button (take from dialog as frame has a sub panel)
        final JButton closeButton = FrameUtils.findButtonComponents(advAcqDialog.getContentPane(), "Close");

        // found the original start button
        if (closeButton != null)
        {
            // remove previous listener
            for (ActionListener l : closeButton.getActionListeners())
                closeButton.removeActionListener(l);

            // add our own listener on start action
            closeButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(final ActionEvent arg0)
                {
                    try
                    {
                        // stop the acquisition (need to check that we are still initialized)
                        if (MicroManager.isInitialized())
                            MicroManager.stopAcquisition();
                    }
                    catch (Throwable t)
                    {
                        // may fail if MM core shutdown at same time
                    }

                    // just hide the frame
                    advAcqFrame.setVisible(false);
                }
            });
        }

        // We are forced to put manually the size because of the way the frame is
        // created by Micro-Manager, original size (521,690)
        advAcqFrame.getIcyInternalFrame().setSize(524, 716);
        advAcqFrame.getIcyExternalFrame().setSize(524, 716);
        // put it in desktop pane
        advAcqFrame.addToDesktopPane();
    }

    public void shutdown()
    {
        // remove listener
        MicroManager.removeAcquisitionListener(this);
    }

    /**
     * called when the 'start' button from advanced acquisition has been pressed
     */
    void advancedAcquisitionStartPressed()
    {
        try
        {
            liveModeWasRunning = MicroManager.isLiveRunning();

            if (liveModeWasRunning)
                MicroManager.stopLiveMode();
        }
        catch (Exception e)
        {
            IcyExceptionHandler.showErrorMessage(e, false);
        }
    }

    public AcqControlDlg getMMAcquisitionDialog()
    {
        return advAcqDialog;
    }

    public IcyFrame getAcquisitionFrame()
    {
        return advAcqFrame;
    }

    @Override
    public void acquisitionStarted(SequenceSettings settings, JSONObject metadata)
    {
        try
        {
            // process on acquisition start
            final int numImage = MDUtils.getNumPositions(metadata) * MDUtils.getNumFrames(metadata)
                    * MDUtils.getNumSlices(metadata) * MDUtils.getNumChannels(metadata);

            ThreadUtil.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    progressIndex = 0;
                    progressLabel.setText("Acquisition running : ");
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(numImage);

                    if (startButton != null)
                        startButton.setEnabled(false);
                }
            });
        }
        catch (JSONException e)
        {
            IcyExceptionHandler.showErrorMessage(e, true);
        }
    }

    @Override
    public void acqImgReveived(TaggedImage newImg)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressBar.setValue(progressIndex++);
            }
        });
    }

    @Override
    public void acquisitionFinished(List<Sequence> result)
    {
        // restore live mode if needed
        if (liveModeWasRunning)
        {
            try
            {
                MicroManager.startLiveMode();
            }
            catch (Exception e)
            {
                IcyExceptionHandler.showErrorMessage(e, true);
            }

            liveModeWasRunning = false;
        }

        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                progressBar.setValue(0);
                progressLabel.setText("Acquisition completed : ");

                if (startButton != null)
                    startButton.setEnabled(true);
            }
        });
    }
}
