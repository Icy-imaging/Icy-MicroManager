package plugins.tprovoost.Microscopy.MicroManager.tools;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.preferences.XMLPreferences;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.point.Point3D;
import icy.util.StringUtil;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;

/**
 * Static Utility class for moving the microscope stage and listening to stage movements.
 * 
 * @author Irsath Nguyen
 */
public class StageMover
{
    public interface StageListener
    {
        public void onStagePositionChanged(String zStage, double z);

        public void onStagePositionChangedRelative(String zStage, double z);

        public void onXYStagePositionChanged(String XYStage, double x, double y);

        public void onXYStagePositionChangedRelative(String XYStage, double x, double y);
    }

    private static List<StageListener> _listeners = new ArrayList<StageListener>();
    private static boolean invertX = false;
    private static boolean invertY = false;
    private static boolean invertZ = false;
    private static boolean switchXY = false;

    private static XMLPreferences prefs;
    /** Constant value of the invert x for stage movement */
    private static final String INVERTX = "invertx";
    /** Constant value of the invert y for stage movement */
    private static final String INVERTY = "inverty";
    /** Constant value of the invert z for stage movement */
    private static final String INVERTZ = "invertz";
    /** Constant value of the switch between x and y for stage movement */
    private static final String SWITCHXY = "switchxy";

    public static void loadPreferences(XMLPreferences pref)
    {
        prefs = pref;
        invertX = prefs.getBoolean(INVERTX, false);
        invertY = prefs.getBoolean(INVERTY, false);
        invertZ = prefs.getBoolean(INVERTZ, false);
        switchXY = prefs.getBoolean(SWITCHXY, false);
    }

    /**
     * Add a listener to the Stage Mover. The Stage Mover will update the listeners with the new values of the XY stage
     * and focus device (x,y,z).
     * 
     * @param sl
     *        : Listener object to be added.
     */
    public static void addListener(StageListener sl)
    {
        _listeners.add(sl);
    }

    /**
     * Add a listener to the Stage Mover. he stage will update the listeners with the new values.
     * 
     * @param sl
     *        : Listener object to be removed.
     */
    public static void removeListener(StageListener sl)
    {
        _listeners.remove(sl);
    }

    public static void clearListener()
    {
        _listeners.clear();
    }

    public static void setInvertX(boolean invertX)
    {
        StageMover.invertX = invertX;
        prefs.putBoolean(INVERTX, invertX);
    }

    public static void setInvertY(boolean invertY)
    {
        StageMover.invertY = invertY;
        prefs.putBoolean(INVERTY, invertY);
    }

    public static void setInvertZ(boolean invertZ)
    {
        StageMover.invertZ = invertZ;
        prefs.putBoolean(INVERTZ, invertZ);
    }

    public static boolean isInvertX()
    {
        return invertX;
    }

    public static boolean isInvertY()
    {
        return invertY;
    }

    public static boolean isInvertZ()
    {
        return invertZ;
    }

    public static void setSwitchXY(boolean switchXY)
    {
        StageMover.switchXY = switchXY;
        prefs.putBoolean(SWITCHXY, switchXY);
    }

    public static boolean isSwitchXY()
    {
        return switchXY;
    }

    public static void onStagePositionChanged(String s, double z)
    {
        for (StageListener l : _listeners)
            l.onStagePositionChanged(s, z);
    }

    public static void onXYStagePositionChanged(String s, double d, double d1)
    {
        for (StageListener l : _listeners)
            l.onXYStagePositionChanged(s, d, d1);
    };

    /**
     * Returns current Stage device
     */
    public static String getXYStageDevice()
    {
        return MicroManager.getCore().getXYStageDevice();
    }

    /**
     * Returns current focus (Z position) device
     */
    public static String getZFocusDevice()
    {
        return MicroManager.getCore().getFocusDevice();
    }

    /**
     * Blocking method.
     * 
     * @throws Exception
     *         if an error occurs
     */
    public static void waitXYToRespond() throws Exception
    {
        final String device = getXYStageDevice();

        if (!StringUtil.isEmpty(device))
            MicroManager.waitForDevice(device);
    }

    /**
     * Blocking method.
     * 
     * @throws Exception
     *         if an error occurs
     */
    public static void waitZToRespond() throws Exception
    {
        final String device = getZFocusDevice();

        if (!StringUtil.isEmpty(device))
            MicroManager.waitForDevice(device);
    }

    /**
     * This method returns the coordinates x of the current stage device
     * 
     * @return Returns the coordinates x of the current stage device.
     * @throws Exception
     *         if an error occurs
     */
    public static double getX() throws Exception
    {
        final String device = getXYStageDevice();

        if (StringUtil.isEmpty(device))
            return 0d;

        // MicroManager.getCore().waitForDevice(device);
        return MicroManager.getCore().getXPosition(device);
    }

    /**
     * This method returns the coordinates y of the current stage device
     * 
     * @return Returns the coordinates y of the current stage device.
     * @throws Exception
     *         if an error occurs
     */
    public static double getY() throws Exception
    {
        final String device = getXYStageDevice();

        if (StringUtil.isEmpty(device))
            return 0d;

        // MicroManager.getCore().waitForDevice(device);
        return MicroManager.getCore().getYPosition(device);
    }

    /**
     * This method returns the coordinates z of the current focus device
     * 
     * @return Returns the coordinates z of the current focus device.
     * @throws Exception
     *         if an error occurs
     */
    public static double getZ() throws Exception
    {
        final String device = getZFocusDevice();

        if (StringUtil.isEmpty(device))
            return 0d;

        // MicroManager.getCore().waitForDevice(device);
        return MicroManager.getCore().getPosition(device);
    }

    /**
     * This method returns the coordinates x and y of the current stage device.
     * 
     * @throws Exception
     *         if an error occurs
     */
    public static Point2D.Double getXY() throws Exception
    {
        final String device = getXYStageDevice();

        if (StringUtil.isEmpty(device))
            return new Point2D.Double(0d, 0d);

        // MicroManager.getCore().waitForDevice(device);
        return MicroManager.getCore().getXYStagePosition(device);
    }

    /**
     * This method returns the coordinates x and y of the current stage deviceand z coordinate of focus device.
     * 
     * @throws Exception
     *         if an error occurs
     */
    public static Point3D.Double getXYZ() throws Exception
    {
        final Point2D.Double pt2d = getXY();
        return new Point3D.Double(pt2d.x, pt2d.y, getZ());
    }

    /**
     * This method returns the coordinates x and y of the current stage device and z coordinate of focus device.
     * 
     * @return Returns [x, y, z]
     * @throws Exception
     *         if an error occurs
     */
    public static double[] getXYZAsDoubleArray() throws Exception
    {
        final Point3D.Double pt = getXYZ();
        return new double[] {pt.x, pt.y, pt.z};
    }

    /**
     * Wait while stage device (XY position) is changing (blocking method).
     * 
     * @return <code>false</code> if more than 5 seconds passed or <code>true</code> if the movement is done.
     */
    public static boolean waitXYMoving() throws Exception
    {
        final long start = System.currentTimeMillis();
        Point2D.Double last = getXY();
        Point2D.Double current;

        while ((System.currentTimeMillis() - start) < 5000)
        {
            current = getXY();
            if ((current.distance(last) < 0.05d))
                return true;

            last = current;
            ThreadUtil.sleep(100);
        }

        return false;
    }

    /**
     * Wait while focus device (Z position) is changing (blocking method).
     * 
     * @return <code>false</code> if more than 5 seconds passed or <code>true</code> if the movement is done.
     */
    public static boolean waitZMoving() throws Exception
    {
        final long start = System.currentTimeMillis();
        double last = getZ();
        double current;

        while ((System.currentTimeMillis() - start) < 5000)
        {
            current = getZ();
            if ((Math.abs(last - current) < 0.05d))
                return true;

            last = current;
            ThreadUtil.sleep(100);
        }

        return false;
    }

    /**
     * Stops the XY stage Movement.
     * 
     * @throws Exception
     */
    public static void stopXYStage() throws Exception
    {
        final String device = getXYStageDevice();

        if (!StringUtil.isEmpty(device))
        {
            MicroManager.lock();
            try
            {
                MicroManager.getCore().stop(device);
            }
            finally
            {
                MicroManager.unlock();
            }
        }
    }

    /**
     * Stops the Z focus Movement.
     * 
     * @throws Exception
     */
    public static void stopZFocus() throws Exception
    {
        final String device = getZFocusDevice();

        if (!StringUtil.isEmpty(device))
        {
            MicroManager.lock();
            try
            {
                MicroManager.getCore().stop(device);
            }
            finally
            {
                MicroManager.unlock();
            }
        }
    }

    /**
     * Moves the stage on the Z-Axis. Wait for movement.
     * 
     * @param position
     *        Z position (in µm)
     * @throws Exception
     */
    public static void moveZAbsolute(double position) throws Exception
    {
        moveZAbsolute(position, true);
    }

    /**
     * Moves the stage on the Z-Axis. <br/>
     * <b>You should wait for the microscope if you are planning to capture image after this call</b>
     * 
     * @param position
     *        Z position (in µm)
     * @param wait
     *        wait for device to process command. Note that you can use {@link #waitZMoving()} to ensure Z stage
     *        complete the movement.
     * @throws Exception
     * @see #moveZRelative(double, boolean)
     * @see #waitZMoving()
     */
    public static void moveZAbsolute(double position, boolean wait) throws Exception
    {
        final String device = getZFocusDevice();

        if (!StringUtil.isEmpty(device))
        {
            MicroManager.waitForDevice(device);
            MicroManager.lock();
            try
            {
                MicroManager.getCore().setPosition(device, position);
            }
            finally
            {
                MicroManager.unlock();
            }
            if (wait)
                MicroManager.waitForDevice(device);
        }
    }

    /**
     * Move the stage on the X and Y axes to the absolute position given by posX and posY. Wait for movement.
     * 
     * @param posX
     *        x position wanted
     * @param posY
     *        y position wanted
     * @throws Exception
     */
    public static void moveXYAbsolute(double posX, double posY) throws Exception
    {
        moveXYAbsolute(posX, posY, true);
    }

    /**
     * Move the stage on the X and Y axes to the absolute position given by posX and posY. <br/>
     * <b>You should wait for the microscope if you are planning to capture image after this call</b>
     * 
     * @param posX
     *        x position wanted
     * @param posY
     *        y position wanted
     * @param wait
     *        wait for device to process command. Note that you can use {@link #waitXYMoving()} to ensure XY stage
     *        complete the movement.
     * @throws Exception
     * @see #moveXYRelative(double, double, boolean)
     * @see #waitXYMoving()
     */
    public static void moveXYAbsolute(double posX, double posY, boolean wait) throws Exception
    {
        final String device = getXYStageDevice();

        if (!StringUtil.isEmpty(device))
        {
            MicroManager.waitForDevice(device);
            MicroManager.lock();
            try
            {
                MicroManager.getCore().setXYPosition(device, posX, posY);
            }
            finally
            {
                MicroManager.unlock();
            }
            if (wait)
                MicroManager.waitForDevice(device);
        }
    }

    /**
     * Moves the stage on the Z-axis relative to current position. Wait for movement. <br/>
     * <b>Relative move may not be accurate !</b>
     * 
     * @param movement
     *        : movement (in µm)
     * @throws Exception
     */
    public static void moveZRelative(double movement) throws Exception
    {
        moveZRelative(movement, true);
    }

    /**
     * Moves the stage on the Z-axis relative to current position. <br/>
     * <b>Relative move may not be accurate !</b> <br/>
     * <b>You should wait for the microscope if you are planning to capture image after this call</b>
     * 
     * @param movement
     *        Z movement (in µm)
     * @param wait
     *        wait for device to process command. Note that you can use {@link #waitZMoving()} to ensure Z stage
     *        complete the movement.
     * @throws Exception
     * @see #moveZAbsolute(double, boolean)
     * @see #waitZMoving()
     */
    public static void moveZRelative(double movement, boolean wait) throws Exception
    {
        final String device = getZFocusDevice();

        if (!StringUtil.isEmpty(device))
        {
            MicroManager.waitForDevice(device);
            MicroManager.lock();
            try
            {
                if (invertZ)
                    MicroManager.getCore().setRelativePosition(device, -movement);
                else
                    MicroManager.getCore().setRelativePosition(device, movement);
            }
            finally
            {
                MicroManager.unlock();
            }
            if (wait)
                MicroManager.waitForDevice(device);
        }
    }

    /**
     * Moves the stage on the X and Y axes relative to actual position. Wait for movement. <br/>
     * <b>Relative move may not be accurate !</b>
     * 
     * @param movX
     *        movement on X-Axis (in µm)
     * @param movY
     *        movement on Y-Axis (in µm)
     * @throws Exception
     */
    public static void moveXYRelative(double movX, double movY) throws Exception
    {
        moveXYRelative(movX, movY, true);
    }

    /**
     * Moves the stage on the X and Y axes relative to actual position. <br/>
     * <b>Relative move may not be accurate !</b> <br/>
     * <b>You should wait for the microscope if you are planning to capture image after this call</b>
     * 
     * @param movX
     *        movement on X-Axis (in µm)
     * @param movY
     *        movement on Y-Axis (in µm)
     * @param wait
     *        wait for device to process command. Note that you can use {@link #waitXYMoving()} to ensure XY stage
     *        complete the movement.
     * @throws Exception
     * @see #moveXYAbsolute(double, double, boolean)
     * @see #waitXYMoving()
     */
    public static void moveXYRelative(double movX, double movY, boolean wait) throws Exception
    {
        final String device = getXYStageDevice();

        if (!StringUtil.isEmpty(device))
        {
            final int invXModifier = invertX ? -1 : 1;
            final int invYModifier = invertY ? -1 : 1;

            MicroManager.waitForDevice(device);
            MicroManager.lock();
            try
            {
                if (switchXY)
                    MicroManager.getCore().setRelativeXYPosition(device, movY * invYModifier, movX * invXModifier);
                else
                    MicroManager.getCore().setRelativeXYPosition(device, movX * invXModifier, movY * invYModifier);
            }
            finally
            {
                MicroManager.unlock();
            }

            if (wait)
                MicroManager.waitForDevice(device);
        }
    }

    /**
     * This method will move the stage to the ROI wanted in the Sequence. This call is blocking while the microscope not
     * finished it move. <b>Be careful, this method should not be used if the pixelSize configuration is not
     * accurate.</b> <br/>
     * <br/>
     * <b>This movement may not be accurate !</b>
     * 
     * @param s
     *        Sequence with the ROIs wanted to focus on.
     * @param roi
     *        The ROI2D to focus on.
     * @throws Exception
     */
    public static void moveStageToROI(Sequence s, ROI2D roi) throws Exception
    {
        if (roi == null)
            return;

        final double pxsize;
        final double vectx = roi.getBounds().getCenterX() - s.getBounds2D().getCenterX();
        // Y coordinates are inverted in the sequence
        final double vecty = -(roi.getBounds().getCenterY() - s.getBounds2D().getCenterY());

        pxsize = MicroManager.getCore().getPixelSizeUm();

        StageMover.moveXYRelative(vectx * pxsize, vecty * pxsize);
    }

    /**
     * Move the stage to a specific point <b>in the sequence</b>. The movement will be relative to the actual position
     * of the stage, and relative to the center in the sequence. This call is blocking while the microscope not
     * finished/ it move. <br/>
     * <b>This movement may not be accurate !</b>
     * 
     * @param s
     *        Sequence used to calculate the center and the movement to the coordinates given.
     * @param x
     *        x value of the point we want to go to.
     * @param y
     *        y value of the point we want to go to.
     * @throws Exception
     */
    public static void moveToPoint(Sequence s, double x, double y) throws Exception
    {
        final double pxsize;
        final double vectx = x - s.getBounds2D().getCenterX();
        // Y coordinates are inverted in the sequence
        final double vecty = y - s.getBounds2D().getCenterY();

        pxsize = MicroManager.getCore().getPixelSizeUm();

        StageMover.moveXYRelative(vectx * pxsize, vecty * pxsize);
    }
}
