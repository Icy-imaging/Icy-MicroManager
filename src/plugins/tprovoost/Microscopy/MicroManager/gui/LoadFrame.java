package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.action.IcyAbstractAction;
import icy.file.FileUtil;
import icy.gui.component.button.IcyButton;
import icy.gui.dialog.MessageDialog;
import icy.gui.dialog.OpenDialog;
import icy.main.Icy;
import icy.preferences.PluginsPreferences;
import icy.preferences.XMLPreferences;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicromanagerPlugin;

/**
 * This class is the loading dialog used to select the configuration. Currently, this dialog is only
 * visible the first time the user launches the plugin, and when he hits the "Load Configuration"
 * button in the {@link MMMainFrame}.
 * 
 * @author Irsath Nguyen & Thomas Provoost
 */
public class LoadFrame extends JDialog
{
    private static final long serialVersionUID = 1195697437027678195L;

    // Preference keys for this package
    private static final String FILE = "cfgfile";
    private static final String NB_FILES = "nbfiles";

    // Class variables
    private XMLPreferences _prefs;
    String sysConfigFile;
    DefaultListModel _CFGFiles;
    File _actualfile = null;
    int _retval;

    JList _list_files;
    private JTextArea _list_devices;
    private JScrollPane _scroll_devices;
    private JTextArea _list_configs;
    private JTextArea _list_resume;

    // Actions
    final IcyAbstractAction openAction;
    final IcyAbstractAction cancelAction;
    final IcyAbstractAction addAction;
    final IcyAbstractAction removeAction;

    // Buttons
    IcyButton openButton;
    private IcyButton cancelButton;
    private IcyButton removeButton;
    private IcyButton addButton;

    public LoadFrame()
    {
        super(Icy.getMainInterface().getMainFrame(), "Please choose your configuration file", true);

        _prefs = PluginsPreferences.root(MicromanagerPlugin.class).node("CFGFiles");
        _CFGFiles = new DefaultListModel();

        // load stored config
        loadPrefs();

        openAction = new IcyAbstractAction("Open", new IcyIcon(ResourceUtil.ICON_OPEN), "Open currently selected file",
                KeyEvent.VK_ENTER)
        {
            @Override
            protected boolean doAction(ActionEvent e)
            {
                _retval = 0;
                savePrefs();
                setVisible(false);
                return true;
            }
        };
        cancelAction = new IcyAbstractAction("Cancel", new IcyIcon(ResourceUtil.ICON_DELETE),
                "Cancel and close Micro Manager", KeyEvent.VK_ESCAPE)
        {
            @Override
            protected boolean doAction(ActionEvent e)
            {
                _retval = 1;
                dispose();
                return true;
            }
        };
        addAction = new IcyAbstractAction("", new IcyIcon(ResourceUtil.ICON_PLUS), "Add a new file to the list.")
        {
            @Override
            protected boolean doAction(ActionEvent e)
            {
                loadConfig();
                return true;
            }
        };
        removeAction = new IcyAbstractAction("", new IcyIcon(ResourceUtil.ICON_MINUS),
                "Remove current file from the list.")
        {
            @Override
            protected boolean doAction(ActionEvent e)
            {
                if (!_list_files.isSelectionEmpty())
                    _CFGFiles.remove(_list_files.getSelectedIndex());
                savePrefs();
                repaint();
                return true;
            }
        };

        initialize();

        _list_files.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                if (!_list_files.isSelectionEmpty())
                {
                    sysConfigFile = (String) _list_files.getSelectedValue();
                    _actualfile = new File(sysConfigFile);

                    _list_files.setToolTipText("Selected = " + sysConfigFile);
                    try
                    {
                        loadFileAttribs();
                        openButton.setEnabled(true);
                    }
                    catch (IOException e1)
                    {
                        MessageDialog.showDialog("Error", e1.getMessage(), MessageDialog.ERROR_MESSAGE);
                        e1.printStackTrace();
                    }
                }
                else
                    _list_files
                            .setToolTipText("Double click on a file or select it then click on 'Open File' to open it.");
            }
        });
        _list_files.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(java.awt.event.MouseEvent mouseevent)
            {
                if ((mouseevent.getClickCount() >= 2) && openButton.isEnabled())
                    openButton.doClick();
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                _retval = 1;
                savePrefs();
            }
        });

        // default retval
        _retval = -1;
    }

    private void initialize()
    {
        JSplitPane rightPanel = new JSplitPane();
        rightPanel.setContinuousLayout(true);
        rightPanel.setResizeWeight(0.5);
        getContentPane().add(rightPanel, BorderLayout.CENTER);

        _list_devices = new JTextArea();
        _list_devices.setEditable(false);
        _list_devices.setToolTipText("Devices in the current file.");

        _scroll_devices = new JScrollPane(_list_devices);
        JPanel _panel_devices = new JPanel();
        _panel_devices.setPreferredSize(new Dimension(120, 10));
        rightPanel.setLeftComponent(_panel_devices);
        _panel_devices.setLayout(new BorderLayout());
        _panel_devices.add(_scroll_devices);

        JLabel lbl_devices = new JLabel("Devices");
        _panel_devices.add(lbl_devices, BorderLayout.NORTH);
        lbl_devices.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lbl_config_presets = new JLabel("Config / Main Presets");
        lbl_config_presets.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane _scroll_configs = new JScrollPane();

        JPanel _panel_configs = new JPanel();
        _panel_configs.setPreferredSize(new Dimension(120, 10));
        rightPanel.setRightComponent(_panel_configs);
        _panel_configs.setLayout(new BorderLayout());
        _panel_configs.add(lbl_config_presets, BorderLayout.NORTH);
        _panel_configs.add(_scroll_configs);

        _list_configs = new JTextArea();
        _scroll_configs.setViewportView(_list_configs);
        _list_configs.setEditable(false);
        _list_configs.setToolTipText("Configurations and Presets for the current file.");

        JPanel leftPanel = new JPanel();
        getContentPane().add(leftPanel, BorderLayout.WEST);
        leftPanel.setLayout(new BorderLayout(0, 0));

        _list_resume = new JTextArea("Nb Devices:\r\nNb Groups: \r\nNb Presets: ");
        _list_resume.setEditable(false);
        _list_resume.setToolTipText("Basic information on the current file");

        JLabel _lbl_resume = new JLabel("Resume : ");
        _lbl_resume.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel _panel_resume = new JPanel();
        leftPanel.add(_panel_resume, BorderLayout.SOUTH);
        _panel_resume.setLayout(new BoxLayout(_panel_resume, BoxLayout.Y_AXIS));
        _panel_resume.add(_lbl_resume);
        _panel_resume.add(_list_resume);
        JLabel lbl_files = new JLabel("Files");
        lbl_files.setHorizontalAlignment(SwingConstants.CENTER);
        lbl_files.setFont(lbl_files.getFont().deriveFont(Font.BOLD, 12));

        _list_files = new JList(_CFGFiles);
        _list_files.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        _list_files.setLayoutOrientation(JList.VERTICAL);
        _list_files.setToolTipText("Double click on a file or select it then click on 'Open File' to open it.");

        JScrollPane _scroll_files = new JScrollPane(_list_files);
        JPanel _panel_files = new JPanel();
        _panel_files.setPreferredSize(new Dimension(400, 80));
        _panel_files.setMinimumSize(new Dimension(400, 10));
        leftPanel.add(_panel_files, BorderLayout.CENTER);
        _panel_files.setLayout(new BorderLayout());
        _panel_files.add(lbl_files, BorderLayout.NORTH);
        _panel_files.add(_scroll_files);
        lbl_devices.setFont(lbl_files.getFont().deriveFont(Font.BOLD, 12));
        lbl_config_presets.setFont(lbl_files.getFont().deriveFont(Font.BOLD, 12));

        openButton = new IcyButton(openAction);
        openButton.setEnabled(false);
        cancelButton = new IcyButton(cancelAction);
        addButton = new IcyButton(addAction);
        removeButton = new IcyButton(removeAction);

        JPanel buttonPanel = new JPanel();
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createHorizontalStrut(2));
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(openButton);
        buttonPanel.add(Box.createHorizontalStrut(2));
        buttonPanel.add(cancelButton);

        setPreferredSize(new Dimension(720, 320));

        validate();
    }

    public int showDialog()
    {
        pack();
        setLocationRelativeTo(Icy.getMainInterface().getDesktopPane());
        setVisible(true);

        // clean
        removeAll();

        return _retval;
    }

    public String getConfigFilePath()
    {
        return sysConfigFile;
    }

    void savePrefs()
    {
        _prefs.putInt(NB_FILES, _CFGFiles.getSize());
        for (int i = 0; i < _CFGFiles.getSize(); ++i)
            _prefs.put(FILE + i, _CFGFiles.getElementAt(i).toString());
    }

    private void loadPrefs()
    {
        _CFGFiles.removeAllElements();

        for (int i = 0; i < _prefs.getInt(NB_FILES, 0); ++i)
        {
            String file = _prefs.get(FILE + i, "");
            if (FileUtil.exists(file))
                _CFGFiles.addElement(file);
        }

        if (_CFGFiles.isEmpty() && MMUtils.demoConfigFile != null)
            loadFile(MMUtils.demoConfigFile.getAbsolutePath());
    }

    void loadConfig()
    {
        String path = OpenDialog.chooseFile("Launch Configuration", MMUtils.getMicroManagerFolder(), "*.cfg");

        if (path != null)
        {
            sysConfigFile = path;
            loadFile(sysConfigFile);
        }
    }

    private void loadFile(String path)
    {
        if (!_CFGFiles.contains(path) && FileUtil.exists(path))
            _CFGFiles.addElement(path);
        else
            _list_files.setSelectedValue(path, true);

        savePrefs();
        repaint();
    }

    void loadFileAttribs() throws IOException
    {
        int nb_devices = 0;
        int nb_groups = 0;
        int nb_presets = 0;

        String slist_devices = "";
        String slist_configs = "";

        BufferedReader in = new BufferedReader(new FileReader(_actualfile));
        if (_actualfile != null)
        {
            String actual_line = "";
            while ((actual_line = in.readLine()) != null)
            {
                if (actual_line.isEmpty())
                    continue;
                if (actual_line.charAt(0) == '#')
                {
                    if (actual_line.contains("Group:"))
                    {
                        slist_configs = slist_configs + actual_line.substring(9) + "\n";
                        nb_groups++;
                    }
                    else if (actual_line.contains("Preset:"))
                    {
                        slist_configs = slist_configs + "   " + actual_line.substring(10) + "\n";
                        nb_presets++;
                    }
                }
                else
                {
                    if (actual_line.startsWith("Device"))
                    {
                        actual_line = actual_line.substring(7);
                        int coma_index;
                        while ((coma_index = actual_line.indexOf(',')) != -1)
                            actual_line = actual_line.substring(coma_index + 1);
                        slist_devices = slist_devices + actual_line + "\n";
                        nb_devices++;
                    }
                }
            }
        }
        in.close();

        String slist_resume = "Nb Devices: " + nb_devices;
        slist_resume = slist_resume + "\nNb Groups: " + nb_groups;
        slist_resume = slist_resume + "\nNb Presets: " + nb_presets;
        _list_devices.setText(slist_devices);
        _list_configs.setText(slist_configs);
        _list_resume.setText(slist_resume);
        _list_devices.setCaretPosition(0);
        _list_configs.setCaretPosition(0);
        _list_resume.setCaretPosition(0);
    }
}