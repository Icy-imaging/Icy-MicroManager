/**
 * 
 */
package plugins.tprovoost.Microscopy.MicroManager.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * @author Stephane
 */
public class MainPanel extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = -510939587633033910L;

    public ActionsPanel actionsPanel;
    public ConfigurationPanel configPanel;
    public CameraSettingsPanel cameraPanel;
    public LiveSettingsPanel livePanel;
    public AcquisitionInfoPanel acquisitionInfoPanel;
    public PluginsToolbar pluginsPanel;

    public MainPanel(MMMainFrame mainFrame)
    {
        super();

        actionsPanel = new ActionsPanel(mainFrame);
        cameraPanel = new CameraSettingsPanel(mainFrame);
        livePanel = new LiveSettingsPanel(mainFrame);
        configPanel = new ConfigurationPanel(mainFrame);
        acquisitionInfoPanel = new AcquisitionInfoPanel(mainFrame);
        pluginsPanel = new PluginsToolbar(mainFrame);

        initialize();
    }

    /**
     * Create all needed graphics components and listeners and show the main frame.
     */
    void initialize()
    {
        JPanel leftPanel = new JPanel();
        leftPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        leftPanel.setLayout(new BorderLayout());

        leftPanel.add(actionsPanel, BorderLayout.WEST);
        leftPanel.add(cameraPanel, BorderLayout.CENTER);
        leftPanel.add(livePanel, BorderLayout.SOUTH);
        
        JPanel rightPanel = new JPanel();
        rightPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        rightPanel.setLayout(new BorderLayout());
        
        rightPanel.add(acquisitionInfoPanel, BorderLayout.CENTER);
        rightPanel.add(pluginsPanel, BorderLayout.SOUTH);
        
        JPanel southPanel = new JPanel();
        southPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        southPanel.setLayout(new BorderLayout());

        southPanel.add(leftPanel, BorderLayout.WEST);
        southPanel.add(rightPanel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(configPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }
}
