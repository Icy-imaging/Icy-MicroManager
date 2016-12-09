package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.component.button.IcyButton;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.MessageDialog;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import mmcorej.CMMCore;

import org.micromanager.internal.ConfigGroupPad;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.dialogs.GroupEditor;
import org.micromanager.internal.dialogs.PresetEditor;
import org.micromanager.internal.utils.ReportingUtils;

import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;

public class ConfigurationPanel extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = 7458866395274126587L;

    final MMMainFrame mainFrame;

    /** Configuration panel. */
    ConfigGroupPad groupPad;
    final Runnable configsRefresher;
    final Runnable groupRefresher;
    String groupNameRefresh;
    String configNameRefresh;
    boolean fromCacheRefresh;

    /**
     * Create the panel.
     */
    public ConfigurationPanel(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;

        initialize();

        configsRefresher = new Runnable()
        {
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
                        refreshConfigsNow(fromCacheRefresh);
                    }
                });
            }
        };
        groupRefresher = new Runnable()
        {
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
                        refresgGroupNow(groupNameRefresh, configNameRefresh);
                    }
                });
            }
        };
    }

    private void initialize()
    {
        setBorder(new TitledBorder(null, "Configuration settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        // SETUP
        groupPad = new ConfigGroupPad();
        groupPad.setFont(new Font("", 0, 10));
        groupPad.setCore(getCore());
        groupPad.setParentGUI(getMMStudio());

        final JPanel configButtonPanel = new JPanel();
        configButtonPanel.setBorder(new EmptyBorder(1, 1, 1, 1));
        GridBagLayout gbl_configButtonPanel = new GridBagLayout();
        gbl_configButtonPanel.columnWidths = new int[] {50, 0, 0, 0, 16, 56, 50, 0, 0, 0, 16, 0, 0};
        gbl_configButtonPanel.rowHeights = new int[] {29, 0};
        gbl_configButtonPanel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
                Double.MIN_VALUE};
        gbl_configButtonPanel.rowWeights = new double[] {0.0, Double.MIN_VALUE};
        configButtonPanel.setLayout(gbl_configButtonPanel);

        final IcyButton addGroupBtn = new IcyButton(new IcyIcon("sq_plus"));
        addGroupBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final GroupEditor ge = new GroupEditor("", "", getMMStudio(), getCore(), true);
                if (ge.getWidth() < 580)
                    ge.setSize(580, ge.getHeight());
                ge.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosed(WindowEvent e)
                    {
                        refreshConfigs(false);
                    };
                });
            }
        });

        final JLabel groupLabel = new JLabel(" Group ");

        GridBagConstraints gbc_groupLabel = new GridBagConstraints();
        gbc_groupLabel.anchor = GridBagConstraints.WEST;
        gbc_groupLabel.insets = new Insets(0, 0, 0, 5);
        gbc_groupLabel.gridx = 0;
        gbc_groupLabel.gridy = 0;
        configButtonPanel.add(groupLabel, gbc_groupLabel);
        GridBagConstraints gbc_addGroupBtn = new GridBagConstraints();
        gbc_addGroupBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_addGroupBtn.anchor = GridBagConstraints.NORTH;
        gbc_addGroupBtn.insets = new Insets(0, 0, 0, 5);
        gbc_addGroupBtn.gridx = 1;
        gbc_addGroupBtn.gridy = 0;
        configButtonPanel.add(addGroupBtn, gbc_addGroupBtn);
        final IcyButton editGroupBtn = new IcyButton(new IcyIcon("doc_edit"));
        editGroupBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final String groupName = MMUtils.getSelectedGroupName(groupPad);

                if (StringUtil.isEmpty(groupName))
                    MessageDialog
                            .showDialog("To edit a group, please select it first then press the edit group button.");
                else
                {
                    final GroupEditor ge = new GroupEditor(groupName, MMUtils.getSelectedPresetName(groupPad),
                            getMMStudio(), getCore(), false);
                    if (ge.getWidth() < 580)
                        ge.setSize(580, ge.getHeight());
                    ge.addWindowListener(new WindowAdapter()
                    {
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            refreshConfigs(false);
                        };
                    });
                }
            }
        });
        final IcyButton removeGroupBtn = new IcyButton(new IcyIcon("sq_minus"));
        removeGroupBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final String groupName = MMUtils.getSelectedGroupName(groupPad);

                if (StringUtil.isEmpty(groupName))
                    MessageDialog
                            .showDialog("To remove a group, please select it first then press the remove group button.");
                else
                {
                    if (ConfirmDialog.confirm("Remove group", "Are you sure you want to remove group " + groupName
                            + " and all associated presets ?"))
                    {
                        try
                        {
                            getCore().deleteConfigGroup(groupName);
                        }
                        catch (Exception e1)
                        {
                            getMMStudio().logError(e1);
                        }

                        refreshConfigs(false);
                    }
                }
            }
        });
        GridBagConstraints gbc_removeGroupBtn = new GridBagConstraints();
        gbc_removeGroupBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_removeGroupBtn.anchor = GridBagConstraints.NORTH;
        gbc_removeGroupBtn.insets = new Insets(0, 0, 0, 5);
        gbc_removeGroupBtn.gridx = 2;
        gbc_removeGroupBtn.gridy = 0;
        configButtonPanel.add(removeGroupBtn, gbc_removeGroupBtn);
        GridBagConstraints gbc_editGroupBtn = new GridBagConstraints();
        gbc_editGroupBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_editGroupBtn.anchor = GridBagConstraints.NORTH;
        gbc_editGroupBtn.insets = new Insets(0, 0, 0, 5);
        gbc_editGroupBtn.gridx = 3;
        gbc_editGroupBtn.gridy = 0;
        configButtonPanel.add(editGroupBtn, gbc_editGroupBtn);
        final JLabel presetLabel = new JLabel(" Preset ");
        GridBagConstraints gbc_presetLabel = new GridBagConstraints();
        gbc_presetLabel.anchor = GridBagConstraints.WEST;
        gbc_presetLabel.insets = new Insets(0, 0, 0, 5);
        gbc_presetLabel.gridx = 6;
        gbc_presetLabel.gridy = 0;
        configButtonPanel.add(presetLabel, gbc_presetLabel);

        final IcyButton addPresetBtn = new IcyButton(new IcyIcon("sq_plus"));
        addPresetBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final String groupName = MMUtils.getSelectedGroupName(groupPad);

                if (StringUtil.isEmpty(groupName))
                    MessageDialog
                            .showDialog("To add a preset to a group, please select the group first then press the add preset button.");
                else
                {
                    final PresetEditor pe = new PresetEditor(groupName, "", getMMStudio(), getCore(), true);
                    pe.addWindowListener(new WindowAdapter()
                    {
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            refreshConfigs(false);
                        };
                    });
                }
            }
        });
        GridBagConstraints gbc_addPresetBtn = new GridBagConstraints();
        gbc_addPresetBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_addPresetBtn.anchor = GridBagConstraints.NORTH;
        gbc_addPresetBtn.insets = new Insets(0, 0, 0, 5);
        gbc_addPresetBtn.gridx = 7;
        gbc_addPresetBtn.gridy = 0;
        configButtonPanel.add(addPresetBtn, gbc_addPresetBtn);
        final IcyButton editPresetBtn = new IcyButton(new IcyIcon("doc_edit"));
        editPresetBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final String groupName = MMUtils.getSelectedGroupName(groupPad);
                final String presetName = MMUtils.getSelectedPresetName(groupPad);

                if (StringUtil.isEmpty(groupName) || StringUtil.isEmpty(presetName))
                    MessageDialog
                            .showDialog("To edit a preset, please select the preset first then press the edit preset button.");
                else
                {
                    final PresetEditor pe = new PresetEditor(groupName, presetName, getMMStudio(), getCore(), false);
                    pe.addWindowListener(new WindowAdapter()
                    {
                        @Override
                        public void windowClosed(WindowEvent e)
                        {
                            refreshConfigs(false);
                        };
                    });
                }
            }
        });
        final IcyButton removePresetBtn = new IcyButton(new IcyIcon("sq_minus"));
        removePresetBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final String groupName = MMUtils.getSelectedGroupName(groupPad);
                final String presetName = MMUtils.getSelectedPresetName(groupPad);

                if (StringUtil.isEmpty(groupName) || StringUtil.isEmpty(presetName))
                    MessageDialog
                            .showDialog("To remove a preset from a group, please select preset first then press the remove button.");
                else
                {
                    if (getCore().getAvailableConfigs(groupName).size() == 1L)
                    {
                        if (ConfirmDialog.confirm("Remove last preset in group", "'" + presetName
                                + "' is the last preset for the '" + groupName
                                + "' group.\nDelete both preset and group ?"))
                        {
                            try
                            {
                                getCore().deleteConfig(groupName, presetName);
                                getCore().deleteConfigGroup(groupName);
                            }
                            catch (Exception e1)
                            {
                                getMMStudio().logError(e1);
                            }
                        }
                    }
                    else
                    {
                        if (ConfirmDialog.confirm("Remove preset", "Are you sure you want to remove preset '"
                                + presetName + "' from the '" + groupName + "' group ?"))
                        {
                            try
                            {
                                getCore().deleteConfig(groupName, presetName);
                            }
                            catch (Exception e1)
                            {
                                getMMStudio().logError(e1);
                            }
                        }
                    }

                    refreshConfigs(false);
                }
            }
        });
        GridBagConstraints gbc_removePresetBtn = new GridBagConstraints();
        gbc_removePresetBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_removePresetBtn.anchor = GridBagConstraints.NORTH;
        gbc_removePresetBtn.insets = new Insets(0, 0, 0, 5);
        gbc_removePresetBtn.gridx = 8;
        gbc_removePresetBtn.gridy = 0;
        configButtonPanel.add(removePresetBtn, gbc_removePresetBtn);
        GridBagConstraints gbc_editPresetBtn = new GridBagConstraints();
        gbc_editPresetBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_editPresetBtn.anchor = GridBagConstraints.NORTH;
        gbc_editPresetBtn.insets = new Insets(0, 0, 0, 5);
        gbc_editPresetBtn.gridx = 9;
        gbc_editPresetBtn.gridy = 0;
        configButtonPanel.add(editPresetBtn, gbc_editPresetBtn);

        final IcyButton saveBtn = new IcyButton(new IcyIcon(ResourceUtil.ICON_SAVE));
        saveBtn.setToolTipText("Save current presets to configuration file...");
        saveBtn.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                mainFrame.saveConfig();
            }
        });
        GridBagConstraints gbc_saveBtn = new GridBagConstraints();
        gbc_saveBtn.fill = GridBagConstraints.HORIZONTAL;
        gbc_saveBtn.anchor = GridBagConstraints.NORTH;
        gbc_saveBtn.gridx = 11;
        gbc_saveBtn.gridy = 0;
        configButtonPanel.add(saveBtn, gbc_saveBtn);

        // LEFT PART OF INTERFACE
        setLayout(new BorderLayout());
        add(groupPad, BorderLayout.CENTER);
        // add(new JPanel(), BorderLayout.CENTER);
        add(configButtonPanel, BorderLayout.SOUTH);
    }

    MMStudio getMMStudio()
    {
        return mainFrame.mmstudio;
    }

    CMMCore getCore()
    {
        return getMMStudio().getCore();
    }

    public void refresgGroupNow(String groupName, String configName)
    {
        groupPad.refreshGroup(groupNameRefresh, configNameRefresh);
    }

    public void refreshGroup(String groupName, String configName)
    {
        groupNameRefresh = groupName;
        configNameRefresh = configName;
        ThreadUtil.bgRunSingle(groupRefresher);
    }

    public void refreshConfigsNow(boolean fromCache)
    {
        groupPad.refreshStructure(fromCache);
        groupPad.repaint();
    }

    public void refreshConfigs(boolean fromCache)
    {
        fromCacheRefresh = fromCache;
        ThreadUtil.bgRunSingle(configsRefresher);
    }
}
