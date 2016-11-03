package plugins.tprovoost.Microscopy.MicroManager.tools;

import icy.gui.component.button.IcyButton;
import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.PluginDescriptor;
import icy.resource.icon.IcyIcon;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * Utilities class used for wrapping Micro-Manager Frames and Dialogs in a IcyFrame.
 * Using this, we can add easily Icy function like detach to Micro-Manager's frame's
 * without having to recreate them.
 * 
 * @author Irsath Nguyen
 */
public class FrameUtils
{
    public static IcyFrame addMMWindowToDesktopPane(Window window)
    {
        IcyFrame frame = GuiUtil.createIcyFrameFromWindow(window);
        frame.addToDesktopPane();
        frame.setVisible(true);
        return frame;
    }
    
    public static JButton findButtonComponents(Container container, String label)
    {
        for (Component c : container.getComponents())
        {
            if (c instanceof JButton)
            {
                final JButton button = (JButton) c;

                if (button.getText().equalsIgnoreCase(label))
                    return button;
            }
        }

        return null;
    }


    /**
     * @param buttonText
     * @param iconPath
     *        may be null, if no icon wanted
     * @param action
     * @return an IcyButton with buttonText and an icon on it if iconPath not null.<br />
     *         The button returned have an actionListener wich execute the runnable.
     */
    public static IcyButton createUIButton(String buttonText, String iconPath, final Runnable action)
    {
        IcyButton theButton;
        if (iconPath != null && !iconPath.isEmpty())
            theButton = new IcyButton(buttonText, new IcyIcon(iconPath, IcyIcon.DEFAULT_SIZE));
        else
            theButton = new IcyButton(buttonText);
        theButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                action.run();
            }
        });
        return theButton;
    }

    /**
     * @return an IcyButton with text and icon describing the specified plugin.
     */
    public static IcyButton createPluginButton(PluginDescriptor plugin, ActionListener action)
    {
        final IcyButton result = new IcyButton(new IcyIcon(plugin.getIconAsImage(), 32, false));

        result.setToolTipText(plugin.getName());
        if (action != null)
            result.addActionListener(action);

        return result;
    }

}
