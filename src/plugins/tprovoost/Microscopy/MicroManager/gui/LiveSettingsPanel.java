package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.component.NumberTextField;
import icy.util.StringUtil;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class LiveSettingsPanel extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = 3710457297904488668L;

    MMMainFrame mainFrame;

    private JLabel lblZStart;
    private NumberTextField zStartField;
    private NumberTextField zEndField;
    private JLabel lblNewLabel;
    private NumberTextField zStepField;

    public LiveSettingsPanel(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;

        initialize();
    }

    private void initialize()
    {
        setBorder(new TitledBorder(null, "Snap / Live / Album settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {60, 0, 60, 0, 60, 0, 0};
        gridBagLayout.rowHeights = new int[] {0, 0};
        gridBagLayout.columnWeights = new double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        lblZStart = new JLabel("Z start");
        lblZStart.setToolTipText("Z start position (um) for stack acquisition (used only if Z step is not 0)");
        GridBagConstraints gbc_lblZStart = new GridBagConstraints();
        gbc_lblZStart.fill = GridBagConstraints.VERTICAL;
        gbc_lblZStart.anchor = GridBagConstraints.WEST;
        gbc_lblZStart.insets = new Insets(0, 0, 0, 5);
        gbc_lblZStart.gridx = 0;
        gbc_lblZStart.gridy = 0;
        add(lblZStart, gbc_lblZStart);

        zStartField = new NumberTextField();
        zStartField.setText("0");
        GridBagConstraints gbc_zStartField = new GridBagConstraints();
        gbc_zStartField.insets = new Insets(0, 0, 0, 5);
        gbc_zStartField.fill = GridBagConstraints.BOTH;
        gbc_zStartField.gridx = 1;
        gbc_zStartField.gridy = 0;
        add(zStartField, gbc_zStartField);
        zStartField.setColumns(4);

        lblNewLabel = new JLabel("Z end");
        lblNewLabel.setToolTipText("Z end position (um) for stack acquisition (used only if Z step is not 0)");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.gridx = 2;
        gbc_lblNewLabel.gridy = 0;
        add(lblNewLabel, gbc_lblNewLabel);

        zEndField = new NumberTextField();
        zEndField.setText("0");
        GridBagConstraints gbc_zEndField = new GridBagConstraints();
        gbc_zEndField.insets = new Insets(0, 0, 0, 5);
        gbc_zEndField.fill = GridBagConstraints.BOTH;
        gbc_zEndField.gridx = 3;
        gbc_zEndField.gridy = 0;
        add(zEndField, gbc_zEndField);
        zEndField.setColumns(4);

        JLabel lblZSlices = new JLabel("Z step");
        lblZSlices.setToolTipText("Z step (um) between each Z slice (keep 0 for 2D acquisition)");
        GridBagConstraints gbc_lblZSlices = new GridBagConstraints();
        gbc_lblZSlices.fill = GridBagConstraints.VERTICAL;
        gbc_lblZSlices.insets = new Insets(0, 0, 0, 5);
        gbc_lblZSlices.anchor = GridBagConstraints.WEST;
        gbc_lblZSlices.gridx = 4;
        gbc_lblZSlices.gridy = 0;
        add(lblZSlices, gbc_lblZSlices);

        zStepField = new NumberTextField();
        zStepField.setText("0");
        GridBagConstraints gbc_zStepField = new GridBagConstraints();
        gbc_zStepField.fill = GridBagConstraints.BOTH;
        gbc_zStepField.gridx = 5;
        gbc_zStepField.gridy = 0;
        add(zStepField, gbc_zStepField);
        zStepField.setColumns(4);
    }

    public boolean isZStackAcquisition()
    {
        return getZStep() != 0d;
    }

    public double getZStart()
    {
        return StringUtil.parseDouble(zStartField.getText(), 0d);
    }

    public void setZStart(double value)
    {
        zStartField.setText(StringUtil.toString(value));
    }

    public double getZEnd()
    {
        return StringUtil.parseDouble(zEndField.getText(), 0d);
    }

    public void setZEnd(double value)
    {
        zEndField.setText(StringUtil.toString(value));
    }

    public double getZStep()
    {
        return StringUtil.parseDouble(zStepField.getText(), 0d);
    }

    public void setZStep(double value)
    {
        zStepField.setText(StringUtil.toString(value));
    }

}
