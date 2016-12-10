package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.network.NetworkUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.micromanager.internal.MMVersion;

public class AboutPanel extends JPanel
{
    /**
     * 
     */
    private static final long serialVersionUID = -7561207579670139391L;

    JButton OkBtn;

    /**
     * Create the panel.
     */
    public AboutPanel(ActionListener OkAction)
    {
        super();

        initialize();

        if (OkAction != null)
            OkBtn.addActionListener(OkAction);
    }

    private void initialize()
    {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel center = new JPanel(new BorderLayout());
        final JLabel value = new JLabel(
                "<html><body>"
                        + "<h2>About</h2><p>Micro-Manager for Icy is being developed by Stephane Dallongeville, Irsath Nguyen and Thomas Provoost."
                        + "<br/>Copyright 2016, Institut Pasteur</p><br/>"
                        + "<p>This plugin is based on Micro-Manager© " + MMVersion.VERSION_STRING
                        + " which is developed under the following license:<br/>"
                        + "<i>This software is distributed free of charge in the hope that it will be<br/>"
                        + "useful, but WITHOUT ANY WARRANTY; without even the implied<br/>"
                        + "warranty of merchantability or fitness for a particular purpose. In no<br/>"
                        + "event shall the copyright owner or contributors be liable for any direct,<br/>"
                        + "indirect, incidental spacial, examplary, or consequential damages.<br/>"
                        + "Copyright University of California San Francisco, 2010. All rights reserved.</i>" + "</p>" + "</body></html>");
        JLabel link = new JLabel("<html><a href=\"\">For more information, please follow this link.</a></html>");
        link.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent mouseevent)
            {
                NetworkUtil.openBrowser("http://www.micro-manager.org/");
            }
        });
        value.setSize(new Dimension(50, 18));
        value.setAlignmentX(SwingConstants.HORIZONTAL);
        value.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        center.add(value, BorderLayout.CENTER);
        center.add(link, BorderLayout.SOUTH);

        JPanel panel_south = new JPanel();
        panel_south.setLayout(new BoxLayout(panel_south, BoxLayout.X_AXIS));
        OkBtn = new JButton("OK");
        panel_south.add(Box.createHorizontalGlue());
        panel_south.add(OkBtn);
        panel_south.add(Box.createHorizontalGlue());

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(panel_south, BorderLayout.SOUTH);
    }
}
