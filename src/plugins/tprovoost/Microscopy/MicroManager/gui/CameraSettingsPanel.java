package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.component.NumberTextField;
import icy.gui.component.NumberTextField.ValueChangeListener;
import icy.gui.component.button.IcyButton;
import icy.gui.component.button.IcyToggleButton;
import icy.gui.dialog.MessageDialog;
import icy.resource.icon.IcyIcon;
import icy.system.IcyExceptionHandler;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.MMCoreJ;
import mmcorej.StrVector;

import org.micromanager.internal.MMStudio;
import org.micromanager.internal.utils.ReportingUtils;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;

public class CameraSettingsPanel extends JPanel implements Runnable
{
    /**
     * 
     */
    private static final long serialVersionUID = -1799340043444887652L;

    final MMMainFrame mainFrame;

    /** Camera Name */
    String cameraName;
    /** Text Field containing exposure. */
    NumberTextField exposureField;
    /** ComboBox containing current value of binning. */
    JComboBox binningCombo;
    /** ComboBox containing current shutter. */
    JComboBox shuttersCombo;
    /** Shutter state */
    IcyToggleButton shutterOpenBtn;
    /** Auto shutter */
    JCheckBox autoShutterCheckbox;
    /** Auto focus */
    IcyButton autofocusBtn;
    /** Auto focus settings */
    IcyButton autofocusSettingBtn;

    // EXCLUSIVE ACCESS
    boolean modifyingExposure;
    boolean modifyingBinning;
    boolean modifyingShutter;
    private JLabel lblAutoShutter;

    /**
     * Create the panel.
     */
    public CameraSettingsPanel(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;
        modifyingExposure = false;
        modifyingBinning = false;
        modifyingShutter = false;

        initialize();

        exposureField.addValueListener(new ValueChangeListener()
        {
            @Override
            public void valueChanged(double newValue, boolean validate)
            {
                if (validate)
                    setExposureCore(newValue);
            }
        });
        binningCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (binningCombo.getSelectedItem() != null)
                    setBinningCore(getBinning());
            }
        });
        shuttersCombo.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if (shuttersCombo.getSelectedItem() != null)
                    setShutterCore(getShutter());
            }
        });
        shutterOpenBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    final boolean open = shutterOpenBtn.isSelected();

                    MicroManager.setShutterOpen(open);
                    refreshOpenShutterButton(open);
                }
                catch (Throwable t)
                {
                    MessageDialog.showDialog("Cannot change shutter state: " + t, MessageDialog.ERROR_MESSAGE);
                    System.err.println("Cannot change shutter state: " + t);
                }
            }
        });
        lblAutoShutter.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                // forward to checkbox
                autoShutterCheckbox.doClick();
            }
        });
        autoShutterCheckbox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final boolean autoshutter = autoShutterCheckbox.isSelected();

                try
                {
                    // close shutter first
                    MicroManager.setShutterOpen(false);
                    shutterOpenBtn.setSelected(false);
                    refreshOpenShutterButton(false);

                    // then set auto shutter state
                    MicroManager.setAutoShutter(autoshutter);

                    if (StringUtil.isEmpty(MicroManager.getShutter()))
                        shutterOpenBtn.setEnabled(false);
                    else
                        shutterOpenBtn.setEnabled(!autoshutter);
                }
                catch (Throwable t)
                {
                    MessageDialog.showDialog("Cannot set auto shutter: " + t, MessageDialog.ERROR_MESSAGE);
                    System.err.println("Cannot set auto shutter: " + t);
                }
            }
        });
        autofocusBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MicroManager.getMMStudio().autofocusNow();
            }
        });
        autofocusSettingBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MicroManager.getMMStudio().showAutofocusDialog();
            }
        });
    }

    private void initialize()
    {
        setBorder(new TitledBorder(null, "Camera settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {70, 32, 24, 24, 0};
        gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[] {1.0, 0.0, 1.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        exposureField = new NumberTextField();
        GridBagConstraints gbc_lblExposurems = new GridBagConstraints();
        gbc_lblExposurems.gridwidth = 2;
        gbc_lblExposurems.anchor = GridBagConstraints.WEST;
        gbc_lblExposurems.insets = new Insets(0, 0, 5, 5);
        gbc_lblExposurems.gridx = 0;
        gbc_lblExposurems.gridy = 0;
        JLabel lblExposurems = new JLabel("Exposure (ms)");
        add(lblExposurems, gbc_lblExposurems);
        GridBagConstraints gbc_exposureField = new GridBagConstraints();
        gbc_exposureField.gridwidth = 2;
        gbc_exposureField.fill = GridBagConstraints.BOTH;
        gbc_exposureField.insets = new Insets(0, 0, 5, 0);
        gbc_exposureField.gridx = 2;
        gbc_exposureField.gridy = 0;
        add(exposureField, gbc_exposureField);
        GridBagConstraints gbc_lblBinning = new GridBagConstraints();
        gbc_lblBinning.gridwidth = 2;
        gbc_lblBinning.anchor = GridBagConstraints.WEST;
        gbc_lblBinning.insets = new Insets(0, 0, 5, 5);
        gbc_lblBinning.gridx = 0;
        gbc_lblBinning.gridy = 1;
        JLabel lblBinning = new JLabel("Binning");
        add(lblBinning, gbc_lblBinning);

        binningCombo = new JComboBox();
        GridBagConstraints gbc_binningCombo = new GridBagConstraints();
        gbc_binningCombo.gridwidth = 2;
        gbc_binningCombo.fill = GridBagConstraints.BOTH;
        gbc_binningCombo.insets = new Insets(0, 0, 5, 0);
        gbc_binningCombo.gridx = 2;
        gbc_binningCombo.gridy = 1;
        add(binningCombo, gbc_binningCombo);
        GridBagConstraints gbc_lblShutter = new GridBagConstraints();
        gbc_lblShutter.gridwidth = 2;
        gbc_lblShutter.anchor = GridBagConstraints.WEST;
        gbc_lblShutter.insets = new Insets(0, 0, 5, 5);
        gbc_lblShutter.gridx = 0;
        gbc_lblShutter.gridy = 2;
        JLabel lblShutter = new JLabel("Shutter");
        add(lblShutter, gbc_lblShutter);

        shuttersCombo = new JComboBox();
        GridBagConstraints gbc_shuttersCombo = new GridBagConstraints();
        gbc_shuttersCombo.gridwidth = 2;
        gbc_shuttersCombo.insets = new Insets(0, 0, 5, 0);
        gbc_shuttersCombo.fill = GridBagConstraints.BOTH;
        gbc_shuttersCombo.gridx = 2;
        gbc_shuttersCombo.gridy = 2;
        add(shuttersCombo, gbc_shuttersCombo);

        shutterOpenBtn = new IcyToggleButton("Open", new IcyIcon("shutter"));
        shutterOpenBtn.setIconTextGap(10);
        shutterOpenBtn.setToolTipText("Open / close the shutter");

        lblAutoShutter = new JLabel("Auto shutter");
        lblAutoShutter.setToolTipText("Enabled auto open/close shutter");
        GridBagConstraints gbc_lblAutoShutter = new GridBagConstraints();
        gbc_lblAutoShutter.anchor = GridBagConstraints.WEST;
        gbc_lblAutoShutter.insets = new Insets(0, 0, 5, 5);
        gbc_lblAutoShutter.gridx = 0;
        gbc_lblAutoShutter.gridy = 3;
        add(lblAutoShutter, gbc_lblAutoShutter);

        autoShutterCheckbox = new JCheckBox("");
        autoShutterCheckbox.setToolTipText("Enabled auto open/close shutter");
        autoShutterCheckbox.setHorizontalTextPosition(SwingConstants.LEADING);
        GridBagConstraints gbc_autoShutterCheckbox = new GridBagConstraints();
        gbc_autoShutterCheckbox.anchor = GridBagConstraints.WEST;
        gbc_autoShutterCheckbox.insets = new Insets(0, 0, 5, 5);
        gbc_autoShutterCheckbox.gridx = 1;
        gbc_autoShutterCheckbox.gridy = 3;
        add(autoShutterCheckbox, gbc_autoShutterCheckbox);
        GridBagConstraints gbc_shutterBtn = new GridBagConstraints();
        gbc_shutterBtn.fill = GridBagConstraints.BOTH;
        gbc_shutterBtn.gridwidth = 2;
        gbc_shutterBtn.insets = new Insets(0, 0, 5, 0);
        gbc_shutterBtn.gridx = 2;
        gbc_shutterBtn.gridy = 3;
        add(shutterOpenBtn, gbc_shutterBtn);

        JLabel lblAutofocus = new JLabel("Autofocus");
        GridBagConstraints gbc_lblAutofocus = new GridBagConstraints();
        gbc_lblAutofocus.gridwidth = 2;
        gbc_lblAutofocus.anchor = GridBagConstraints.WEST;
        gbc_lblAutofocus.insets = new Insets(0, 0, 0, 5);
        gbc_lblAutofocus.gridx = 0;
        gbc_lblAutofocus.gridy = 4;
        add(lblAutofocus, gbc_lblAutofocus);

        autofocusBtn = new IcyButton(new IcyIcon("autofocus"));
        autofocusBtn.setToolTipText("Perform autofocus now");
        GridBagConstraints gbc_autofocusBtn = new GridBagConstraints();
        gbc_autofocusBtn.fill = GridBagConstraints.BOTH;
        gbc_autofocusBtn.insets = new Insets(0, 0, 0, 5);
        gbc_autofocusBtn.gridx = 2;
        gbc_autofocusBtn.gridy = 4;
        add(autofocusBtn, gbc_autofocusBtn);

        autofocusSettingBtn = new IcyButton(new IcyIcon("af_setting"));
        autofocusSettingBtn.setToolTipText("Autofocus settings");
        GridBagConstraints gbc_autoFocusSettingBtn = new GridBagConstraints();
        gbc_autoFocusSettingBtn.fill = GridBagConstraints.BOTH;
        gbc_autoFocusSettingBtn.gridx = 3;
        gbc_autoFocusSettingBtn.gridy = 4;
        add(autofocusSettingBtn, gbc_autoFocusSettingBtn);
    }

    MMStudio getMMStudio()
    {
        return mainFrame.getMMStudio();
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

    public String getCameraName()
    {
        return cameraName;
    }

    public double getExposure()
    {
        return exposureField.getNumericValue();
    }

    public int getBinning()
    {
        final Object item = binningCombo.getSelectedItem();
        if (item != null)
            return Integer.parseInt((String) item);
        return 1;
    }

    public String getShutter()
    {
        final Object item = shuttersCombo.getSelectedItem();
        if (item != null)
            return (String) item;
        return "";
    }

    void setExposureCore(double value)
    {
        // modified from core --> do nothing
        if (modifyingExposure)
            return;

        final double exposure = Math.max(1, value);

        try
        {
            // safe exposure set
            MicroManager.setExposure(exposure);
        }
        catch (Exception e)
        {
            logError(e, "Couldn't set exposure time.");
            IcyExceptionHandler.showErrorMessage(e, true);
        }
    }

    void setBinningCore(int value)
    {
        // modified from core --> do nothing
        if (modifyingBinning)
            return;

        final int binning = Math.max(1, value);

        try
        {
            // safe binning set
            MicroManager.setBinning(binning);
        }
        catch (Exception e)
        {
            logError(e, "Couldn't set camera binning.");
            IcyExceptionHandler.showErrorMessage(e, true);
        }
    }

    void setShutterCore(String value)
    {
        // modified from core --> do nothing
        if (modifyingShutter)
            return;

        try
        {
            // safe shutter set
            MicroManager.setShutter(value);
        }
        catch (Exception e)
        {
            ReportingUtils.logError(e);
        }
    }

    public void onExposureChanged(final double exposure)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                modifyingExposure = true;
                try
                {
                    exposureField.setText(StringUtil.toString(exposure));
                }
                finally
                {
                    modifyingExposure = false;
                }
            }
        });
    }

    public void onBinningChanged(final String propValue)
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                modifyingBinning = true;
                try
                {
                    binningCombo.setSelectedItem(propValue);
                }
                finally
                {
                    modifyingBinning = false;
                }
            }
        });
    }

    void refreshCameraName()
    {
        cameraName = getCore().getCameraDevice();
    }

    void refreshExposureComponent()
    {
        try
        {
            final double exposure = getCore().getExposure();

            modifyingExposure = true;
            try
            {
                exposureField.setText(String.valueOf(exposure));
            }
            finally
            {
                modifyingExposure = false;
            }
        }
        catch (Exception e)
        {
            logError(e);
        }
    }

    void refreshBinningComponent()
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final StrVector availableBinnings;
        final String binning;

        try
        {
            if (!StringUtil.isEmpty(cameraName))
            {
                lock();
                try
                {
                    availableBinnings = core.getAllowedPropertyValues(cameraName, MMCoreJ.getG_Keyword_Binning());
                    binning = core.getProperty(cameraName, MMCoreJ.getG_Keyword_Binning());
                }
                finally
                {
                    unlock();
                }
            }
            else
            {
                availableBinnings = new StrVector();
                availableBinnings.add("1");
                binning = "1";
            }

            modifyingBinning = true;
            try
            {
                binningCombo.removeAllItems();
                for (String v : availableBinnings)
                    binningCombo.addItem(v);
                binningCombo.setMaximumRowCount((int) availableBinnings.size());
                binningCombo.setEditable(availableBinnings.isEmpty());
                binningCombo.setSelectedItem(binning);
            }
            finally
            {
                modifyingBinning = false;
            }
        }
        catch (Exception e)
        {
            logError(e);
        }
    }

    void refreshShutterComponents()
    {
        final CMMCore core = getCore();
        if (core == null)
            return;

        final StrVector availableShutters;
        final String shutter;
        final boolean autoShutter;
        final boolean shutterOpen;

        try
        {
            lock();
            try
            {
                availableShutters = core.getLoadedDevicesOfType(DeviceType.ShutterDevice);
                shutter = core.getShutterDevice();
                autoShutter = core.getAutoShutter();
                shutterOpen = core.getShutterOpen();
            }
            finally
            {
                unlock();
            }

            modifyingShutter = true;
            try
            {
                shuttersCombo.removeAllItems();
                for (String v : availableShutters)
                    shuttersCombo.addItem(v);
                shuttersCombo.setSelectedItem(shutter);
            }
            finally
            {
                modifyingShutter = false;
            }

            // set open shutter state
            shutterOpenBtn.setSelected(shutterOpen);
            refreshOpenShutterButton(shutterOpen);

            if (StringUtil.isEmpty(shutter))
                shutterOpenBtn.setEnabled(false);
            else
                shutterOpenBtn.setEnabled(!autoShutter);

            autoShutterCheckbox.setSelected(autoShutter);
        }
        catch (Exception e)
        {
            logError(e);
        }
    }

    void refreshOpenShutterButton(boolean open)
    {
        if (open)
        {
            shutterOpenBtn.setText("Close");
            shutterOpenBtn.setToolTipText("Close the shutter (currently opened)");
        }
        else
        {
            shutterOpenBtn.setText("Open");
            shutterOpenBtn.setToolTipText("Open the shutter (currently closed)");
        }
    }

    public void refreshNow()
    {
        refreshCameraName();
        refreshExposureComponent();
        refreshBinningComponent();
        refreshShutterComponents();
    }

    public void refresh()
    {
        // passive refresh
        ThreadUtil.bgRunSingle(this);
    }

    @Override
    public void run()
    {
        // keep it cool :)
        ThreadUtil.sleep(20);

        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                refreshNow();
            }
        });
    }
}
