package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.math.MathUtil;
import icy.system.thread.ThreadUtil;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Point2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import mmcorej.CMMCore;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

public class AcquisitionInfoPanel extends JPanel implements Runnable
{
    /**
     * 
     */
    private static final long serialVersionUID = -6615066602497363425L;

    final MMMainFrame mainFrame;

    private JLabel imgResField;
    private JLabel depthField;
    private JLabel pixelSizeField;
    private JLabel posXYField;
    private JLabel posZField;
    private JLabel lblNewLabel;
    private JLabel lblPixel;
    private JLabel lblUm;
    private JLabel lblUm_1;
    private JLabel lblUm_2;

    /**
     * Create the panel.
     */
    public AcquisitionInfoPanel(MMMainFrame mainFrame)
    {
        super();

        this.mainFrame = mainFrame;

        initialize();
    }

    private void initialize()
    {
        setBorder(new TitledBorder(null, "Informations", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] {70, 90, 44, 0};
        gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[] {1.0, 1.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[] {1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        JLabel lblImageResolution = new JLabel("Image res. ");
        lblImageResolution.setToolTipText("Image resolution (pixel)");
        GridBagConstraints gbc_lblImageResolution = new GridBagConstraints();
        gbc_lblImageResolution.anchor = GridBagConstraints.WEST;
        gbc_lblImageResolution.insets = new Insets(0, 0, 5, 5);
        gbc_lblImageResolution.gridx = 0;
        gbc_lblImageResolution.gridy = 0;
        add(lblImageResolution, gbc_lblImageResolution);

        imgResField = new JLabel("512 x 512");
        GridBagConstraints gbc_imgResField = new GridBagConstraints();
        gbc_imgResField.anchor = GridBagConstraints.EAST;
        gbc_imgResField.insets = new Insets(0, 0, 5, 5);
        gbc_imgResField.gridx = 1;
        gbc_imgResField.gridy = 0;
        add(imgResField, gbc_imgResField);

        lblPixel = new JLabel("pixel");
        GridBagConstraints gbc_lblPixel = new GridBagConstraints();
        gbc_lblPixel.anchor = GridBagConstraints.WEST;
        gbc_lblPixel.insets = new Insets(0, 0, 5, 0);
        gbc_lblPixel.gridx = 2;
        gbc_lblPixel.gridy = 0;
        add(lblPixel, gbc_lblPixel);

        JLabel lblDepth = new JLabel("Depth ");
        lblDepth.setToolTipText("Image depth");
        GridBagConstraints gbc_lblDepth = new GridBagConstraints();
        gbc_lblDepth.anchor = GridBagConstraints.WEST;
        gbc_lblDepth.insets = new Insets(0, 0, 5, 5);
        gbc_lblDepth.gridx = 0;
        gbc_lblDepth.gridy = 1;
        add(lblDepth, gbc_lblDepth);

        depthField = new JLabel("8");
        GridBagConstraints gbc_depthField = new GridBagConstraints();
        gbc_depthField.anchor = GridBagConstraints.EAST;
        gbc_depthField.insets = new Insets(0, 0, 5, 5);
        gbc_depthField.gridx = 1;
        gbc_depthField.gridy = 1;
        add(depthField, gbc_depthField);

        JLabel lblBit = new JLabel("bit");
        GridBagConstraints gbc_lblBit = new GridBagConstraints();
        gbc_lblBit.anchor = GridBagConstraints.WEST;
        gbc_lblBit.insets = new Insets(0, 0, 5, 0);
        gbc_lblBit.gridx = 2;
        gbc_lblBit.gridy = 1;
        add(lblBit, gbc_lblBit);

        JLabel lblPixelSize = new JLabel("Pixel size");
        lblPixelSize.setToolTipText("Pixel size (micro meter)");
        GridBagConstraints gbc_lblPixelSize = new GridBagConstraints();
        gbc_lblPixelSize.anchor = GridBagConstraints.WEST;
        gbc_lblPixelSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblPixelSize.gridx = 0;
        gbc_lblPixelSize.gridy = 2;
        add(lblPixelSize, gbc_lblPixelSize);

        pixelSizeField = new JLabel("10");
        GridBagConstraints gbc_pixelSizeField = new GridBagConstraints();
        gbc_pixelSizeField.anchor = GridBagConstraints.EAST;
        gbc_pixelSizeField.insets = new Insets(0, 0, 5, 5);
        gbc_pixelSizeField.gridx = 1;
        gbc_pixelSizeField.gridy = 2;
        add(pixelSizeField, gbc_pixelSizeField);

        lblUm = new JLabel("um");
        GridBagConstraints gbc_lblUm = new GridBagConstraints();
        gbc_lblUm.anchor = GridBagConstraints.WEST;
        gbc_lblUm.insets = new Insets(0, 0, 5, 0);
        gbc_lblUm.gridx = 2;
        gbc_lblUm.gridy = 2;
        add(lblUm, gbc_lblUm);

        lblNewLabel = new JLabel("Position XY ");
        lblNewLabel.setToolTipText("Stage position XY (micro meter)");
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 3;
        add(lblNewLabel, gbc_lblNewLabel);

        posXYField = new JLabel("0.00 , 0.00");
        GridBagConstraints gbc_posXYField = new GridBagConstraints();
        gbc_posXYField.anchor = GridBagConstraints.EAST;
        gbc_posXYField.insets = new Insets(0, 0, 5, 5);
        gbc_posXYField.gridx = 1;
        gbc_posXYField.gridy = 3;
        add(posXYField, gbc_posXYField);

        lblUm_1 = new JLabel("um");
        GridBagConstraints gbc_lblUm_1 = new GridBagConstraints();
        gbc_lblUm_1.anchor = GridBagConstraints.WEST;
        gbc_lblUm_1.insets = new Insets(0, 0, 5, 0);
        gbc_lblUm_1.gridx = 2;
        gbc_lblUm_1.gridy = 3;
        add(lblUm_1, gbc_lblUm_1);

        JLabel lblNewLabel_1 = new JLabel("Position Z");
        lblNewLabel_1.setToolTipText("Stage position Z (micro meter)");
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 5);
        gbc_lblNewLabel_1.gridx = 0;
        gbc_lblNewLabel_1.gridy = 4;
        add(lblNewLabel_1, gbc_lblNewLabel_1);

        posZField = new JLabel("0.00");
        GridBagConstraints gbc_posZField = new GridBagConstraints();
        gbc_posZField.insets = new Insets(0, 0, 0, 5);
        gbc_posZField.anchor = GridBagConstraints.EAST;
        gbc_posZField.gridx = 1;
        gbc_posZField.gridy = 4;
        add(posZField, gbc_posZField);

        lblUm_2 = new JLabel("um");
        GridBagConstraints gbc_lblUm_2 = new GridBagConstraints();
        gbc_lblUm_2.anchor = GridBagConstraints.WEST;
        gbc_lblUm_2.gridx = 2;
        gbc_lblUm_2.gridy = 4;
        add(lblUm_2, gbc_lblUm_2);
    }

    private void setDepth(int value)
    {
        depthField.setText(Integer.toString(value));
    }

    private void setImageRes(int x, int y)
    {
        imgResField.setText(Integer.toString(x) + " x " + Integer.toString(y));
    }

    private void setPixelSize(double value)
    {
        pixelSizeField.setText(Double.toString(value));
    }

    private void setPositionXY(double x, double y)
    {
        String str;

        if (Double.isNaN(x))
            str = "--";
        else
            str = Double.toString(x);
        str += " , ";
        if (Double.isNaN(y))
            str += "--";
        else
            str += Double.toString(y);

        posXYField.setText(str);
    }

    private void setPositionZ(double value)
    {
        if (Double.isNaN(value))
            posZField.setText("--");
        else
            posZField.setText(Double.toString(value));
    }

    void refreshXYPosition()
    {
        try
        {
            Point2D.Double xy = StageMover.getXY();
            setPositionXY(MathUtil.roundSignificant(xy.getX(), 5), MathUtil.roundSignificant(xy.getY(), 5));
        }
        catch (Exception e)
        {
            setPositionXY(Double.NaN, Double.NaN);
        }
    }

    void refreshZPosition()
    {
        try
        {
            setPositionZ(MathUtil.roundSignificant(StageMover.getZ(), 5));
        }
        catch (Exception e)
        {
            setPositionZ(Double.NaN);
        }
    }

    void refreshImageInfo()
    {
        final CMMCore core = MicroManager.getCore();

        if (core != null)
        {
            try
            {
                setImageRes((int) core.getImageWidth(), (int) core.getImageHeight());
                setDepth((int) core.getImageBitDepth());
                setPixelSize(core.getPixelSizeUm());
            }
            catch (Throwable t)
            {
                System.err.println("Warning: can't read camera informations from Micro-Manager");
                System.err.println("Cause: " + t);
            }
        }
    }

    public void refreshNow()
    {
        refreshXYPosition();
        refreshZPosition();
        refreshImageInfo();
    }

    public void refresh()
    {
        // passive refresh
        ThreadUtil.bgRunSingle(this);
    }

    @Override
    public void run()
    {
        ThreadUtil.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                refreshNow();
            }
        });
        
        // keep it cool :)
        ThreadUtil.sleep(20);        
    }
}
