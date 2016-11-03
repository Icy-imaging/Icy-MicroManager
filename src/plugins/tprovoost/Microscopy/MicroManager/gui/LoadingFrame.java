package plugins.tprovoost.Microscopy.MicroManager.gui;

import icy.gui.frame.IcyFrame;
import icy.system.thread.ThreadUtil;

import java.awt.BorderLayout;

import javax.swing.JProgressBar;

/**
 * Simple static Loading Frame for Micro-Manager initialization.
 * 
 * @author Stephane
 */
public class LoadingFrame extends IcyFrame
{
    JProgressBar progressBar;

    public LoadingFrame(final String message)
    {
        super();

        ThreadUtil.invokeNow(new Runnable()
        {
            @Override
            public void run()
            {
                progressBar = new JProgressBar();

                progressBar.setIndeterminate(true);
                progressBar.setMinimum(0);
                progressBar.setMaximum(1000);
                progressBar.setStringPainted(true);
                progressBar.setString(message);

                setLayout(new BorderLayout());
                add(progressBar, BorderLayout.CENTER);
            }
        });

        setSyncProcess(true);
        setSize(500, 32);
        setResizable(false);
        setTitleBarVisible(false);
        pack();
        addToDesktopPane();
        center();
    }

    public void setMessage(String text)
    {
        progressBar.setString(text);
    }

    public void show()
    {
        setVisible(true);
    }

    public void hide()
    {
        setVisible(false);
    }
}
