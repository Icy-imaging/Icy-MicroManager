package plugins.tprovoost.Microscopy.MicroManager.tools;

import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.internal.ConfigGroupPad;
import org.micromanager.acquisition.internal.TaggedImageQueue;
import org.micromanager.internal.utils.MDUtils;

import icy.file.FileUtil;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.image.colormap.IcyColorMap;
import icy.main.Icy;
import icy.network.NetworkUtil;
import icy.plugin.PluginLoader;
import icy.plugin.PluginLoader.PluginClassLoader;
import icy.preferences.PluginsPreferences;
import icy.preferences.XMLPreferences;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.system.SystemUtil;
import icy.system.thread.ThreadUtil;
import icy.type.DataType;
import icy.type.collection.CollectionUtil;
import icy.type.collection.array.Array2DUtil;
import icy.type.collection.array.ArrayUtil;
import icy.type.point.Point3D;
import icy.util.OMEUtil;
import icy.util.ReflectionUtil;
import icy.util.StringUtil;
import loci.formats.ome.OMEXMLMetadataImpl;
import mmcorej.TaggedImage;
import ome.xml.model.Pixels;
import ome.xml.model.primitives.NonNegativeInteger;
import ome.xml.model.primitives.Timestamp;
import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.patch.MMPatcher;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicromanagerPlugin;

/**
 * Utility class for loading MicromanagerPlugin native libraries such as MMCoreJ_wrap and device libraries.
 * 
 * @author Irsath Nguyen
 */
public class MMUtils
{
    private final static String MM_PATH_ID = "libray_path";

    static XMLPreferences prefs = PluginsPreferences.root(MicromanagerPlugin.class);
    static String uManagerRep = null;
    public static File demoConfigFile = null;
    private static boolean loaded = false;

    /**
     * Returns the Micro-Manager folder
     */
    public static String getMicroManagerFolder()
    {
        if (!loaded)
            if (!fixSystemLibrairies())
                return "";

        return uManagerRep;
    }

    public static boolean isSystemLibrairiesLoaded()
    {
        return loaded;
    }

    public static boolean fixSystemLibrairies()
    {
        if (loaded)
            return loaded;

        uManagerRep = prefs.get(MM_PATH_ID, "");

        final File uManagerLibraryRep = new File(uManagerRep);

        // empty or not existing ?
        if (uManagerRep.isEmpty() || !uManagerLibraryRep.exists() || !uManagerLibraryRep.isDirectory())
        {
            ThreadUtil.invokeNow(new Runnable()
            {
                @Override
                public void run()
                {
                    final int option = JOptionPane.showOptionDialog(Icy.getMainInterface().getMainFrame(),
                            "Have you already installed Micro-Manager ?", "Micro-Manager For Icy",
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                            new String[] {"Select Micro-Manger directory", "Download Micro-Manager", "Cancel"},
                            "Download Micro-Manager");

                    if (option == JOptionPane.NO_OPTION)
                    {
                        NetworkUtil.openBrowser(
                                "http://www.micro-manager.org/wiki/Download%20Micro-Manager_Latest%20Release");
                        MessageDialog.showDialog("Restart this plugin after Micro-Manager installation complete.",
                                MessageDialog.INFORMATION_MESSAGE);
                        uManagerRep = null;
                    }
                    else if (option == JOptionPane.YES_OPTION)
                    {
                        final JFileChooser fc = new JFileChooser();
                        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        if (fc.showOpenDialog(Icy.getMainInterface().getMainFrame()) == JFileChooser.APPROVE_OPTION)
                            uManagerRep = fc.getSelectedFile().getAbsolutePath();
                        else
                            uManagerRep = null;
                    }
                    else
                        uManagerRep = null;
                }
            });
        }

        // operation canceled or directory set
        if (uManagerRep == null)
            return false;

        if (!uManagerRep.endsWith(FileUtil.separator))
            uManagerRep += FileUtil.separator;

        // show loading message
        final ProgressFrame loadingFrame = new ProgressFrame(
                "  Please wait while loading Micro-Manager libraries...  ");
        try
        {
            loaded = loadJarFrom(new File(uManagerRep + FileUtil.separator + "plugins" + FileUtil.separator
                    + "Micro-Manager" + FileUtil.separator));
        }
        finally
        {
            loadingFrame.close();
        }

        if (!loaded)
        {
            new FailedAnnounceFrame(
                    "Error while loading libraries, have you choosen the correct directory ? Please try again.");
        }
        else
        {
            // apply Micro-Manager classes aptches
            MMPatcher.applyPatches();
            // load DLL
            loadDllFrom(new File(uManagerRep));
            // find configuration file
            File[] cfg = new File(uManagerRep).listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(File file, String s)
                {
                    return s.equalsIgnoreCase("MMConfig_demo.cfg");
                }
            });
            if (cfg != null && cfg.length > 0)
                demoConfigFile = cfg[0];

            prefs.put(MM_PATH_ID, uManagerRep);
            System.setProperty("mmcorej.library.path", uManagerRep);
        }

        return loaded;
    }

    public static void resetLibrayPath()
    {
        prefs.put(MM_PATH_ID, "");
        System.setProperty("mmcorej.library.path", "");
    }

    /**
     * Return true if the specified list of TaggedImage contains a <code>null</code> or poison image.
     */
    public static boolean hasNullOrPoison(List<TaggedImage> images)
    {
        for (TaggedImage image : images)
            if ((image == null) || TaggedImageQueue.isPoison(image))
                return true;

        return false;
    }

    /**
     * Convert a list of {@link TaggedImage} (Micro Manager) to {@link IcyBufferedImage} (Icy) where each
     * {@link TaggedImage} represents one channel of the output image.
     * 
     * @throws JSONException
     */
    public static IcyBufferedImage convertToIcyImage(List<TaggedImage> images) throws JSONException
    {
        final List<TaggedImage> goodImages = new ArrayList<TaggedImage>(images.size());
        int w, h, bpp;

        w = -1;
        h = -1;
        bpp = -1;
        for (TaggedImage image : images)
        {
            if ((image != null) && !TaggedImageQueue.isPoison(image))
            {
                if (w == -1)
                    w = MDUtils.getWidth(image.tags);
                else if (w != MDUtils.getWidth(image.tags))
                    continue;
                if (h == -1)
                    h = MDUtils.getHeight(image.tags);
                else if (h != MDUtils.getHeight(image.tags))
                    continue;
                if (bpp == -1)
                    bpp = (MDUtils.getBitDepth(image.tags) + 7) / 8;
                else if (bpp != ((MDUtils.getBitDepth(image.tags) + 7) / 8))
                    continue;

                goodImages.add(image);
            }
        }

        if ((goodImages.size() > 0) && (w > 0) && (h > 0) && (bpp > 0))
        {
            // create the data array
            final Object[] data = Array2DUtil.createArray(ArrayUtil.getDataType(goodImages.get(0).pix),
                    goodImages.size());

            for (int i = 0; i < goodImages.size(); i++)
                data[i] = goodImages.get(i).pix;

            // create and return the image
            return new IcyBufferedImage(w, h, data);
        }

        return null;
    }

    /**
     * Convert a {@link TaggedImage} (Micro-Manager) to {@link IcyBufferedImage} (Icy).
     * 
     * @throws JSONException
     */
    public static IcyBufferedImage convertToIcyImage(TaggedImage img) throws JSONException
    {
        return convertToIcyImage(CollectionUtil.createArrayList(img));
    }

    /**
     * Set Sequence metadata from the given JSON metadata object (can be image or summaryMetadata).
     * 
     * @throws JSONException
     */
    public static void setMetadata(Sequence sequence, JSONObject tags) throws JSONException
    {
        final OMEXMLMetadataImpl metadata = sequence.getMetadata();

        try
        {
            // summary metadata has channel colors
            final JSONArray channelColors = tags.getJSONArray("ChColors");

            for (int c = 0; c < sequence.getSizeC(); c++)
            {
                final Color channelColor = new Color(channelColors.getInt(c));

                if (!channelColor.equals(Color.black))
                {
                    // start by modifying colormodel colormap
                    IcyColorMap colormap = sequence.getColorModel().getColorMap(c);
                    if (colormap != null)
                    {
                        colormap.setARGBControlPoint(0, Color.black);
                        colormap.setARGBControlPoint(255, channelColor);
                    }

                    // then set user colormap
                    colormap = sequence.getColorMap(c);
                    if (colormap != null)
                    {
                        colormap.setARGBControlPoint(0, Color.black);
                        colormap.setARGBControlPoint(255, channelColor);
                    }

                    // and finally metadata
                    metadata.setChannelColor(OMEUtil.getOMEColor(channelColor), 0, c);
                }
            }
        }
        catch (Exception e)
        {
            // ignore
        }

        try
        {
            // summary metadata has channel names
            final JSONArray channelNames = tags.getJSONArray("ChNames");

            for (int c = 0; c < sequence.getSizeC(); c++)
            {
                final String channelName = channelNames.getString(c);
                if (!StringUtil.isEmpty(channelName))
                    sequence.setChannelName(c, channelName);
            }
        }
        catch (Exception e)
        {
            // ignore
        }

        try
        {
            // we don't have this info in summary metadata
            final int ch = MDUtils.getChannelIndex(tags);

            // set channel information
            if (ch < sequence.getSizeC())
            {
                final Color channelColor = new Color(MDUtils.getChannelColor(tags));

                // the channel color information can be wrong here (set to white by default) so we
                // ignore white as well
                if (!channelColor.equals(Color.black) && !channelColor.equals(Color.white))
                {
                    // start by modifying colormodel colormap
                    IcyColorMap colormap = sequence.getColorModel().getColorMap(ch);
                    if (colormap != null)
                    {
                        colormap.setARGBControlPoint(0, Color.black);
                        colormap.setARGBControlPoint(255, channelColor);
                    }

                    // then set user colormap
                    colormap = sequence.getColorMap(ch);
                    if (colormap != null)
                    {
                        colormap.setARGBControlPoint(0, Color.black);
                        colormap.setARGBControlPoint(255, channelColor);
                    }

                    // and finally metadata
                    metadata.setChannelColor(OMEUtil.getOMEColor(channelColor), 0, ch);
                }

                final String channelName = MDUtils.getChannelName(tags);
                if (!StringUtil.isEmpty(channelName))
                    sequence.setChannelName(ch, channelName);

                // final ChannelSpec chSpec = (c < settings.channels.size()) ?
                // settings.channels.get(c) : null;
                // if (chSpec != null)
                // {
                //
                // if (colormap != null)
                // {
                // colormap.setARGBControlPoint(0, Color.black);
                // colormap.setARGBControlPoint(255, chSpec.color);
                // }
                //
                // if (StringUtil.isEmpty(channelName))
                // channelName = chSpec.config;
                // }
            }
        }
        catch (Exception e)
        {
            // ignore
        }

        // set pixel size XY
        if (MDUtils.hasPixelSizeUm(tags))
        {
            final double pixelSize = MDUtils.getPixelSizeUm(tags);
            if (pixelSize > 0d)
            {
                sequence.setPixelSizeX(pixelSize);
                sequence.setPixelSizeY(pixelSize);
            }
        }
        // set pixel size Z
        if (MDUtils.hasZStepUm(tags))
        {
            final double pixelSizeZ = MDUtils.getZStepUm(tags);
            if (pixelSizeZ > 0d)
                sequence.setPixelSizeZ(pixelSizeZ);
        }
        // set time interval
        if (MDUtils.hasIntervalMs(tags))
        {
            final double intervalMs = MDUtils.getIntervalMs(tags) / 1000d;
            if (intervalMs > 0d)
                sequence.setTimeInterval(intervalMs);
        }
    }

    /**
     * Set the TaggedImage metadata.
     * 
     * @param t
     *        wanted T position (frame) of the image, set to <code>-1</code> to keep current value
     * @param z
     *        wanted Z position (slice) of the image, set to <code>-1</code> to keep current value
     * @param c
     *        wanted C position (channel) of the image, set to <code>-1</code> to keep current value
     * @param sizeT
     *        wanted global size T (number of frame), set to <code>-1</code> to keep current value
     * @param sizeZ
     *        wanted global size Z (number of slice), set to <code>-1</code> to keep current value
     * @param sizeC
     *        wanted global size C (number of channel), set to <code>-1</code> to keep current value
     * @throws JSONException
     */
    public static void setImageMetadata(TaggedImage taggedImage, int t, int z, int c, int sizeT, int sizeZ, int sizeC)
            throws JSONException
    {
        if (taggedImage == null)
            return;

        final JSONObject tags = taggedImage.tags;

        if (t >= 0)
            MDUtils.setFrameIndex(tags, t);
        if (z >= 0)
            MDUtils.setSliceIndex(tags, z);
        if (c >= 0)
            MDUtils.setChannelIndex(tags, c);
        if (sizeT > 0)
            tags.put("Frames", sizeT);
        if (sizeZ > 0)
            tags.put("Slices", sizeZ);
        if (sizeC > 0)
            tags.put("Channels", sizeC);
    }

    /**
     * Set image in the specified Sequence object from the given TaggedImage.<br>
     * Returns <code>false</code> if specified image is null or empty, or if the Sequence image format is not compatible
     * with TaggedImage.
     * 
     * @param sequence
     *        destination sequence
     * @param taggedImage
     *        Tagged image to set in the sequence, metadata are used to define the T,Z,C position of the image (see
     *        {@link #setImageMetadata(TaggedImage, int, int, int, int, int, int)} method)
     * @param startDate
     *        Date when acquisition was started (can be used to set delta T information when it's missing from
     *        metadata), set it to 0 to ignore it.
     * @return <code>true</code> if the operation succeed and <code>false</code> otherwise.
     * @throws JSONException
     * @see {@link #setImageMetadata(TaggedImage, int, int, int, int, int, int)}
     */
    public static boolean setImage(Sequence sequence, TaggedImage taggedImage, long startDate) throws JSONException
    {
        // incorrect image --> do nothing
        if ((taggedImage == null) || TaggedImageQueue.isPoison(taggedImage))
            return false;

        final JSONObject tags = taggedImage.tags;

        // not compatible with the sequence --> return false
        if (!isCompatible(sequence, tags))
            return false;

        // position is sequence position (position in XY list)
        // final Integer position = Integer.valueOf(MDUtils.getPositionIndex(tags));

        final int frame = MDUtils.getFrameIndex(tags);
        final int slice = MDUtils.getSliceIndex(tags);
        final int ch = MDUtils.getChannelIndex(tags);

        IcyBufferedImage image = sequence.getImage(frame, slice);

        if (image == null)
            image = createEmptyImage(tags);

        // set data
        image.setDataXY(ch, taggedImage.pix);
        sequence.setImage(frame, slice, image);

        final int sizeC = MDUtils.getNumChannels(tags);
        final int sizeZ = MDUtils.getNumSlices(tags);
        // get plane index from current position (dimension order = XYCZT)
        final int planeIndex = (frame * (sizeZ * sizeC)) + (slice * sizeC) + ch;
        final OMEXMLMetadataImpl metadata = sequence.getMetadata();

        // first image --> set extra general metadata
        if (sequence.getNumImage() == 1)
        {
            setMetadata(sequence, tags);
            if (startDate != 0L)
                metadata.setImageAcquisitionDate(new Timestamp(new DateTime(startDate)), 0);
        }

        // ensure the plane is defined in metadata
        final Pixels pix = MetaDataUtil.getPixels(metadata, 0);
        MetaDataUtil.ensurePlane(pix, planeIndex);

        // fill plane specific metadata
        metadata.setPlaneTheT(NonNegativeInteger.valueOf(String.valueOf(frame)), 0, planeIndex);
        metadata.setPlaneTheZ(NonNegativeInteger.valueOf(String.valueOf(slice)), 0, planeIndex);
        metadata.setPlaneTheC(NonNegativeInteger.valueOf(String.valueOf(ch)), 0, planeIndex);

        Point3D.Double pos;
        double time;
        double exposure;

        try
        {
            pos = StageMover.getXYZ();
        }
        catch (Exception e1)
        {
            pos = new Point3D.Double(0d, 0d, 0d);
        }

        time = 0d;
        exposure = 0d;

        if (MDUtils.hasXPositionUm(tags))
            pos.setX(MDUtils.getXPositionUm(tags));
        if (MDUtils.hasYPositionUm(tags))
            pos.setY(MDUtils.getYPositionUm(tags));
        if (MDUtils.hasZPositionUm(tags))
            pos.setZ(MDUtils.getZPositionUm(tags));
        // get time info (we want it in second)
        if (MDUtils.hasElapsedTimeMs(tags))
            time = MDUtils.getElapsedTimeMs(tags) / 1000d;
        // no time information but we have a start acquisition date ?
        if ((time == 0d) && (startDate != 0))
            time = (Calendar.getInstance().getTimeInMillis() - startDate) / 1000d;
        // get exposure time (we want it in second)
        if (MDUtils.hasExposureMs(tags))
            exposure = MDUtils.getExposureMs(tags) / 1000d;

        try
        {
            // exposure not set ? --> try to get it from core directly
            if (exposure == 0d)
                exposure = MicroManager.getExposure() / 1000d;
        }
        catch (Throwable e)
        {
            IcyExceptionHandler.showErrorMessage(e, false);
        }

        metadata.setPlanePositionX(OMEUtil.getLength(pos.getX()), 0, planeIndex);
        metadata.setPlanePositionY(OMEUtil.getLength(pos.getY()), 0, planeIndex);
        metadata.setPlanePositionZ(OMEUtil.getLength(pos.getZ()), 0, planeIndex);
        metadata.setPlaneDeltaT(OMEUtil.getTime(time), 0, planeIndex);
        metadata.setPlaneExposureTime(OMEUtil.getTime(exposure), 0, planeIndex);

        return true;
    }

    /**
     * Set image in the specified Sequence object from the given TaggedImage.<br>
     * Returns <code>false</code> if specified image is null or empty, or if the Sequence image format is not compatible
     * with TaggedImage.
     * 
     * @param sequence
     *        destination sequence
     * @param taggedImage
     *        Tagged image to set in the sequence
     * @return <code>true</code> if the operation succeed and <code>false</code> otherwise.
     * @throws JSONException
     */
    public static boolean setImage(Sequence sequence, TaggedImage taggedImage) throws JSONException
    {
        return setImage(sequence, taggedImage, 0L);
    }

    /**
     * Returns <code>true</code> if the sequence data format is compatible with specified JSON metadata (same dimension
     * and same pixel format)
     * 
     * @throws JSONException
     */
    public static boolean isCompatible(Sequence sequence, JSONObject metadata) throws JSONException
    {
        if (sequence.isEmpty())
            return true;

        return (sequence.getSizeX() == MDUtils.getWidth(metadata))
                && (sequence.getSizeY() == MDUtils.getHeight(metadata))
                && (sequence.getSizeC() == MDUtils.getNumChannels(metadata))
                && (sequence.getDataType_().getSize() == ((MDUtils.getBitDepth(metadata) + 7) / 8));
    }

    /**
     * Create an empty IcyBufferedImage using properties in given JSON metadata object. It can be either the one from
     * TaggedImage or the summaryMetadata (see {@link MicroManager#getAcquisitionMetaData()})
     * 
     * @throws JSONException
     */
    public static IcyBufferedImage createEmptyImage(JSONObject metadata) throws JSONException
    {
        final int width = MDUtils.getWidth(metadata);
        final int height = MDUtils.getHeight(metadata);
        final int numChannels = MDUtils.getNumChannels(metadata);
        final int bpp = MDUtils.getBitDepth(metadata);
        final int bytespp = (bpp + 7) / 8;

        switch (bytespp)
        {
            default:
            case 1:
                return new IcyBufferedImage(width, height, numChannels, DataType.UBYTE);
            case 2:
                return new IcyBufferedImage(width, height, numChannels, DataType.USHORT);
            case 3:
            case 4:
                return new IcyBufferedImage(width, height, numChannels, DataType.UINT);
            case 5:
            case 6:
            case 7:
            case 8:
                return new IcyBufferedImage(width, height, numChannels, DataType.DOUBLE);
        }
    }

    /**
     * Returns the selected group name from specified ConfigGroupPad
     */
    public static String getSelectedGroupName(ConfigGroupPad group)
    {
        try
        {
            return (String) ReflectionUtil.invokeMethod(group, "getGroup", true, new Object[] {});
        }
        catch (Exception e1)
        {
            try
            {
                return (String) ReflectionUtil.invokeMethod(group, "getSelectedGroup", true, new Object[] {});
            }
            catch (Exception e2)
            {
                return "";
            }
        }
    }

    /**
     * Returns the selected preset name from specified ConfigGroupPad
     */
    public static String getSelectedPresetName(ConfigGroupPad group)
    {
        try
        {
            return (String) ReflectionUtil.invokeMethod(group, "getPreset", true, new Object[] {});
        }
        catch (Exception e1)
        {
            try
            {
                return (String) ReflectionUtil.invokeMethod(group, "getPresetForSelectedGroup", true, new Object[] {});
            }
            catch (Exception e2)
            {
                return "";
            }
        }
    }

    private static boolean loadDllFrom(File microManagerDirectory)
    {
        final List<File> dll = CollectionUtil.asList(FileUtil.getFiles(microManagerDirectory, new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                String extension = FileUtil.getFileExtension(pathname.getAbsolutePath(), false);
                return (extension.equalsIgnoreCase("dll") || extension.equalsIgnoreCase("jnilib"))
                        && !pathname.getName().contains("mmgr_dal_") && !pathname.getName().contains("MMCoreJ_wrap");
            }
        }, true, false, true));

        if (dll.isEmpty())
            return false;

        int numberOfTry = 0;
        while (!dll.isEmpty())
        {
            numberOfTry++;
            for (File f : dll)
            {
                try
                {
                    SystemUtil.loadLibrary(f.getAbsolutePath());
                }
                catch (UnsatisfiedLinkError e)
                {
                }
            }

            if (numberOfTry > 9)
                break;
        }

        return true;
    }

    private static boolean loadJarFrom(File microManagerDirectoryPath)
    {
        File[] files = FileUtil.getFiles(microManagerDirectoryPath, new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                return FileUtil.getFileExtension(pathname.getAbsolutePath(), false).equalsIgnoreCase("jar");
            }
        }, true, false, true);

        if (files == null || files.length == 0)
            return false;

        final ClassLoader cl = PluginLoader.getLoader();
        if (cl instanceof PluginClassLoader)
        {
            for (File f : files)
            {
                final String path = f.getAbsolutePath();
                final String ext = FileUtil.getFileExtension(path, false).toLowerCase();

                if (ext.equals("jar") || ext.equals("class"))
                    ((PluginClassLoader) cl).add(path);
            }
        }

        return true;
    }

}