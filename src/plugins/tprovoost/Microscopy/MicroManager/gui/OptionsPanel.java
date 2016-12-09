package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.dialog.ConfirmDialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mmcorej.CMMCore;

import org.micromanager.data.internal.multipagetiff.StorageMultipageTiff;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.dialogs.AcqControlDlg;
import org.micromanager.internal.logging.LogFileManager;
import org.micromanager.internal.utils.DefaultUserProfile;
import org.micromanager.internal.utils.NumberUtils;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.internal.utils.UIMonitor;

public class OptionsPanel extends JPanel implements ActionListener
{
    /**
     * 
     */
    private static final long serialVersionUID = -6444295509604671195L;

    // GUI
    JTextField bufSizeField_;
    JTextField logDeleteDaysField_;
    JCheckBox storeAcquisition;
    JCheckBox hideMDAdisplay;
    JCheckBox metadataFileWithMultipageTiffCheckBox;
    JCheckBox separateFilesForPositionsMPTiffCheckBox;
    JCheckBox syncExposureMainAndMDA;
    JCheckBox debugLogEnabledCheckBox;
    JCheckBox deleteLogCheckBox;

    // internals
    MMMainFrame mainFrame_;
    MMOptions opts_;
    CMMCore core_;

    /**
     * Create the dialog
     * 
     * @param mainFrame
     *        - Main frame
     * @param opts
     *        - Application wide preferences
     * @param core
     *        - The Micro-Manager Core object
     */
    public OptionsPanel(MMMainFrame mainFrame, MMOptions opts, CMMCore core)
    {
        super();

        mainFrame_ = mainFrame;
        opts_ = opts;
        core_ = core;

        initialize();
        refresh();
    }

    private void initialize()
    {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {16, 0, 0, 40, 0, 16, 0};
        gridBagLayout.rowHeights = new int[] {20, 23, 0, 0, 23, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[] {1.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        GridBagConstraints gbc_lblSequenceBufferSize = new GridBagConstraints();
        gbc_lblSequenceBufferSize.fill = GridBagConstraints.VERTICAL;
        gbc_lblSequenceBufferSize.anchor = GridBagConstraints.EAST;
        gbc_lblSequenceBufferSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblSequenceBufferSize.gridx = 1;
        gbc_lblSequenceBufferSize.gridy = 0;
        JLabel lblSequenceBufferSize = new JLabel("Sequence buffer size");
        add(lblSequenceBufferSize, gbc_lblSequenceBufferSize);

        bufSizeField_ = new JTextField(Integer.toString(opts_.circularBufferSizeMB_), 5);
        GridBagConstraints gbc_bufSizeField_ = new GridBagConstraints();
        gbc_bufSizeField_.fill = GridBagConstraints.BOTH;
        gbc_bufSizeField_.insets = new Insets(0, 0, 5, 5);
        gbc_bufSizeField_.gridx = 2;
        gbc_bufSizeField_.gridy = 0;
        add(bufSizeField_, gbc_bufSizeField_);
        GridBagConstraints gbc_4 = new GridBagConstraints();
        gbc_4.fill = GridBagConstraints.VERTICAL;
        gbc_4.anchor = GridBagConstraints.WEST;
        gbc_4.insets = new Insets(0, 0, 5, 5);
        gbc_4.gridx = 3;
        gbc_4.gridy = 0;
        JLabel label_4 = new JLabel("MB");
        add(label_4, gbc_4);

        separateFilesForPositionsMPTiffCheckBox = new JCheckBox();
        separateFilesForPositionsMPTiffCheckBox.setText("Save XY positions in separate Image stack files");
        separateFilesForPositionsMPTiffCheckBox.setSelected(opts_.mpTiffSeparateFilesForPositions_);

        metadataFileWithMultipageTiffCheckBox = new JCheckBox();
        metadataFileWithMultipageTiffCheckBox.setText("Create metadata.txt file with Image stack files");
        metadataFileWithMultipageTiffCheckBox.setSelected(opts_.mpTiffMetadataFile_);

        GridBagConstraints gbc_metadataFileWithMultipageTiffCheckBox = new GridBagConstraints();
        gbc_metadataFileWithMultipageTiffCheckBox.fill = GridBagConstraints.VERTICAL;
        gbc_metadataFileWithMultipageTiffCheckBox.anchor = GridBagConstraints.WEST;
        gbc_metadataFileWithMultipageTiffCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_metadataFileWithMultipageTiffCheckBox.gridwidth = 4;
        gbc_metadataFileWithMultipageTiffCheckBox.gridx = 1;
        gbc_metadataFileWithMultipageTiffCheckBox.gridy = 1;
        add(metadataFileWithMultipageTiffCheckBox, gbc_metadataFileWithMultipageTiffCheckBox);
        GridBagConstraints gbc_separateFilesForPositionsMPTiffCheckBox = new GridBagConstraints();
        gbc_separateFilesForPositionsMPTiffCheckBox.fill = GridBagConstraints.VERTICAL;
        gbc_separateFilesForPositionsMPTiffCheckBox.anchor = GridBagConstraints.WEST;
        gbc_separateFilesForPositionsMPTiffCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_separateFilesForPositionsMPTiffCheckBox.gridwidth = 4;
        gbc_separateFilesForPositionsMPTiffCheckBox.gridx = 1;
        gbc_separateFilesForPositionsMPTiffCheckBox.gridy = 2;
        add(separateFilesForPositionsMPTiffCheckBox, gbc_separateFilesForPositionsMPTiffCheckBox);

        syncExposureMainAndMDA = new JCheckBox();
        syncExposureMainAndMDA.setText("Sync exposure between Main and MDA windows");
        syncExposureMainAndMDA.setSelected(opts_.syncExposureMainAndMDA_);

        hideMDAdisplay = new JCheckBox();
        hideMDAdisplay.setToolTipText("Checking this option will disable image display when doing MDA");
        hideMDAdisplay.setText("Hide MDA (Multi Dimension Acquisition) display");
        hideMDAdisplay.setSelected(!mainFrame_.getDisplayAcquisitionSequence());
        hideMDAdisplay.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                refresh();
            }
        });
        GridBagConstraints gbc_hideMDAdisplay = new GridBagConstraints();
        gbc_hideMDAdisplay.fill = GridBagConstraints.VERTICAL;
        gbc_hideMDAdisplay.anchor = GridBagConstraints.WEST;
        gbc_hideMDAdisplay.insets = new Insets(0, 0, 5, 5);
        gbc_hideMDAdisplay.gridwidth = 4;
        gbc_hideMDAdisplay.gridx = 1;
        gbc_hideMDAdisplay.gridy = 3;
        add(hideMDAdisplay, gbc_hideMDAdisplay);

        storeAcquisition = new JCheckBox("Keep last acquisition in memory");
        storeAcquisition
                .setToolTipText("Keep last acquisition in memory so it can be retrieved later (disable it for large acquisition you want to stream in file)");
        GridBagConstraints gbc_storeAcquisition = new GridBagConstraints();
        gbc_storeAcquisition.anchor = GridBagConstraints.WEST;
        gbc_storeAcquisition.gridwidth = 4;
        gbc_storeAcquisition.insets = new Insets(0, 0, 5, 5);
        gbc_storeAcquisition.gridx = 1;
        gbc_storeAcquisition.gridy = 4;
        add(storeAcquisition, gbc_storeAcquisition);

        GridBagConstraints gbc_syncExposureMainAndMDA = new GridBagConstraints();
        gbc_syncExposureMainAndMDA.fill = GridBagConstraints.VERTICAL;
        gbc_syncExposureMainAndMDA.anchor = GridBagConstraints.WEST;
        gbc_syncExposureMainAndMDA.insets = new Insets(0, 0, 5, 5);
        gbc_syncExposureMainAndMDA.gridwidth = 4;
        gbc_syncExposureMainAndMDA.gridx = 1;
        gbc_syncExposureMainAndMDA.gridy = 5;
        add(syncExposureMainAndMDA, gbc_syncExposureMainAndMDA);

        deleteLogCheckBox = new JCheckBox();
        deleteLogCheckBox.setText("Delete log files after");
        deleteLogCheckBox.setSelected(opts_.deleteOldCoreLogs_);

        debugLogEnabledCheckBox = new JCheckBox();
        debugLogEnabledCheckBox.setText("Enable debug logging");
        debugLogEnabledCheckBox.setToolTipText("Enable verbose logging for troubleshooting and debugging");
        debugLogEnabledCheckBox.setSelected(opts_.debugLogEnabled_);

        GridBagConstraints gbc_debugLogEnabledCheckBox = new GridBagConstraints();
        gbc_debugLogEnabledCheckBox.fill = GridBagConstraints.VERTICAL;
        gbc_debugLogEnabledCheckBox.gridwidth = 4;
        gbc_debugLogEnabledCheckBox.anchor = GridBagConstraints.WEST;
        gbc_debugLogEnabledCheckBox.insets = new Insets(0, 0, 5, 5);
        gbc_debugLogEnabledCheckBox.gridx = 1;
        gbc_debugLogEnabledCheckBox.gridy = 6;
        add(debugLogEnabledCheckBox, gbc_debugLogEnabledCheckBox);

        GridBagConstraints gbc_deleteLogCheckBox = new GridBagConstraints();
        gbc_deleteLogCheckBox.fill = GridBagConstraints.VERTICAL;
        gbc_deleteLogCheckBox.anchor = GridBagConstraints.WEST;
        gbc_deleteLogCheckBox.insets = new Insets(0, 0, 0, 5);
        gbc_deleteLogCheckBox.gridx = 1;
        gbc_deleteLogCheckBox.gridy = 7;
        add(deleteLogCheckBox, gbc_deleteLogCheckBox);

        logDeleteDaysField_ = new JTextField(Integer.toString(opts_.deleteCoreLogAfterDays_), 3);
        GridBagConstraints gbc_logDeleteDaysField_ = new GridBagConstraints();
        gbc_logDeleteDaysField_.fill = GridBagConstraints.BOTH;
        gbc_logDeleteDaysField_.insets = new Insets(0, 0, 0, 5);
        gbc_logDeleteDaysField_.gridx = 2;
        gbc_logDeleteDaysField_.gridy = 7;
        add(logDeleteDaysField_, gbc_logDeleteDaysField_);
        GridBagConstraints gbc_7 = new GridBagConstraints();
        gbc_7.fill = GridBagConstraints.VERTICAL;
        gbc_7.anchor = GridBagConstraints.WEST;
        gbc_7.insets = new Insets(0, 0, 0, 5);
        gbc_7.gridx = 3;
        gbc_7.gridy = 7;
        JLabel label = new JLabel("days");
        add(label, gbc_7);

        final JButton deleteLogFilesButton = new JButton();
        deleteLogFilesButton.setText("Delete Log Files Now");
        deleteLogFilesButton.setToolTipText("Delete all CoreLog files except " + "for the current one");
        deleteLogFilesButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                String dir1 = LogFileManager.getLogFileDirectory().getAbsolutePath();
                String dir2 = LogFileManager.getLegacyLogFileDirectory().getAbsolutePath();
                String dirs;

                if (dir1.equals(dir2))
                    dirs = dir1;
                else
                    dirs = dir1 + " and " + dir2;

                final boolean confirm = ConfirmDialog.confirm("Delete Log Files",
                        "<html><body><p style='width: 400px;'>" + "Delete all CoreLog files in " + dirs + "?"
                                + "</p></body></html>", JOptionPane.YES_NO_OPTION);
                if (confirm)
                    LogFileManager.deleteLogFilesDaysOld(0, core_.getPrimaryLogFile());
            }
        });

        GridBagConstraints gbc_deleteLogFilesButton = new GridBagConstraints();
        gbc_deleteLogFilesButton.insets = new Insets(0, 0, 0, 5);
        gbc_deleteLogFilesButton.fill = GridBagConstraints.VERTICAL;
        gbc_deleteLogFilesButton.gridx = 4;
        gbc_deleteLogFilesButton.gridy = 7;
        add(deleteLogFilesButton, gbc_deleteLogFilesButton);
    }

    protected void refresh()
    {
        if (hideMDAdisplay.isSelected())
        {
            storeAcquisition.setSelected(mainFrame_.getStoreLastAcquisition());
            storeAcquisition.setEnabled(true);
        }
        else
        {
            storeAcquisition.setSelected(true);
            storeAcquisition.setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        int seqBufSize;
        int deleteLogDays;
        final int oldBufsize = opts_.circularBufferSizeMB_;

        try
        {
            seqBufSize = NumberUtils.displayStringToInt(bufSizeField_.getText());
            deleteLogDays = NumberUtils.displayStringToInt(logDeleteDaysField_.getText());
        }
        catch (ParseException ex)
        {
            ReportingUtils.showError(ex);
            return;
        }

        opts_.circularBufferSizeMB_ = seqBufSize;
        opts_.deleteCoreLogAfterDays_ = deleteLogDays;
        opts_.mpTiffSeparateFilesForPositions_ = separateFilesForPositionsMPTiffCheckBox.isSelected();
        opts_.mpTiffMetadataFile_ = metadataFileWithMultipageTiffCheckBox.isSelected();
        opts_.syncExposureMainAndMDA_ = syncExposureMainAndMDA.isSelected();
        opts_.deleteOldCoreLogs_ = deleteLogCheckBox.isSelected();
        opts_.debugLogEnabled_ = debugLogEnabledCheckBox.isSelected();
        core_.enableDebugLog(opts_.debugLogEnabled_);
        UIMonitor.enable(opts_.debugLogEnabled_);

        opts_.saveSettings();

        // adjust memory footprint if necessary
        if (oldBufsize != opts_.circularBufferSizeMB_)
        {
            try
            {
                core_.setCircularBufferMemoryFootprint(opts_.circularBufferSizeMB_);
            }
            catch (Exception exc)
            {
                ReportingUtils.showError(exc);
            }
        }

        // specific Icy Micro-Manager preferences
        mainFrame_.setDisplayAcquisitionSequence(!hideMDAdisplay.isSelected());
        if (storeAcquisition.isEnabled())
            mainFrame_.setStoreLastAcquisition(storeAcquisition.isSelected());
        else
            mainFrame_.setStoreLastAcquisition(true);
    }
}
