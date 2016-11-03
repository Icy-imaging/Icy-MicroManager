package plugins.tprovoost.Microscopy.MicroManager.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import mmcorej.CMMCore;
import mmcorej.Configuration;
import mmcorej.PropertySetting;

import org.micromanager.acquisition.AcquisitionWrapperEngine;
import org.micromanager.dialogs.ChannelCellEditor;
import org.micromanager.dialogs.ChannelCellRenderer;
import org.micromanager.dialogs.ChannelTableModel;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.ColorEditor;
import org.micromanager.utils.ColorRenderer;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;

/**
 * Custom table representing channel definition for Micro-Manager advanced acquisition
 * 
 * @author Stephane
 */
public class ChannelTable extends JTable
{
    /**
     * 
     */
    private static final long serialVersionUID = 8574853674441794225L;

    final protected ChannelTableModel model;

    public ChannelTable()
    {
        super();

        final MMMainFrame mmframe = MicroManager.getInstance();
        final AcquisitionWrapperEngine eng = MicroManager.getAcquisitionEngine();

        model = new ChannelTableModel(MicroManager.getMMStudio(), eng, mmframe.exposurePrefs, mmframe.colorPrefs,
                mmframe.options);
        model.setChannels(eng.getChannels());
        model.addTableModelListener(model);

        // setFont(new Font("Dialog", Font.PLAIN, 10));
        setAutoCreateColumnsFromModel(false);
        // setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setModel(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final ChannelCellEditor cellEditor = new ChannelCellEditor(eng, mmframe.exposurePrefs, mmframe.colorPrefs);
        final ChannelCellRenderer cellRenderer = new ChannelCellRenderer(eng);

        for (int k = 0; k < model.getColumnCount(); k++)
        {
            if (k == (model.getColumnCount() - 1))
            {
                ColorRenderer cr = new ColorRenderer(true);
                ColorEditor ce = new ColorEditor(model, model.getColumnCount() - 1);
                TableColumn column = new TableColumn(model.getColumnCount() - 1, 200, cr, ce);
                column.setPreferredWidth(60);
                addColumn(column);
            }
            else
            {
                TableColumn column = new TableColumn(k, 200, cellRenderer, cellEditor);
                column.setPreferredWidth(80);
                addColumn(column);
            }
        }

        setPreferredSize(new Dimension(400, 140));
    }

    @Override
    protected JTableHeader createDefaultTableHeader()
    {
        return new JTableHeader(columnModel)
        {
            /**
             * 
             */
            private static final long serialVersionUID = -3646841366871833973L;

            @Override
            public String getToolTipText(MouseEvent e)
            {
                Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return model.getToolTipText(realIndex);
            }
        };
    }

    public void setGroup(String group)
    {
        if (setChannelGroup(group))
            model.cleanUpConfigurationList();
    }

    public List<ChannelSpec> getChannels()
    {
        return model.getChannels();
    }

    public void setChannels(ArrayList<ChannelSpec> values)
    {
        model.setChannels(values);
    }

    protected static boolean setChannelGroup(String group)
    {
        if (groupIsEligibleChannel(group))
        {
            try
            {
                MicroManager.setChannelGroup(group);
                return true;
            }
            catch (Throwable t)
            {
                System.err.println("Cannot set channel group in Micro-Manager.");
                System.err.println("Cause: " + t);
            }
        }

        return false;
    }

    protected static boolean groupIsEligibleChannel(String group)
    {
        final CMMCore core = MicroManager.getCore();
        if (core == null)
            return false;

        final List<String> cfgs = MicroManager.getConfigs(group);

        if (!cfgs.isEmpty())
        {
            try
            {
                final Configuration presetData = core.getConfigData(group, cfgs.get(0));

                if (presetData.size() >= 1L)
                {
                    PropertySetting setting = presetData.getSetting(0L);
                    String devLabel = setting.getDeviceLabel();
                    String propName = setting.getPropertyName();

                    if (core.hasPropertyLimits(devLabel, propName))
                        return false;
                }
            }
            catch (Exception e)
            {
                System.err.println("Error with Micro-Manager:");
                System.err.println(e);
                return false;
            }
        }

        return true;
    }

    protected static boolean isConfigAvailable(String config)
    {
        return MicroManager.getChannelConfigs().contains(config);
    }
}
