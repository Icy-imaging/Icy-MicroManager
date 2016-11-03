package plugins.tprovoost.Microscopy.MicroManager.tools;

import icy.image.IcyBufferedImage;
import icy.type.collection.array.Array1DUtil;

import java.util.List;

import mmcorej.CMMCore;
import mmcorej.TaggedImage;

import org.json.JSONException;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;

/**
 * @deprecated Use the MicroManager methods directly.
 */
@Deprecated
public class ImageGetter
{
    /**
     * @deprecated Use {@link MMUtils#convertToIcyImage(List)} instead.
     */
    @Deprecated
    public static IcyBufferedImage convertToIcyImage(List<TaggedImage> images) throws JSONException
    {
        return MMUtils.convertToIcyImage(images);
    }

    /**
     * @deprecated Use {@link MMUtils#convertToIcyImage(TaggedImage)} instead.
     */
    @Deprecated
    public static IcyBufferedImage convertToIcyImage(TaggedImage image) throws JSONException
    {
        return MMUtils.convertToIcyImage(image);
    }

    // /**
    // * Clear all images coming from the micro manager continuous acquisition in the circular
    // buffer
    // * except the last one.
    // *
    // * @throws Exception
    // */
    // static void clearBufferExceptLast() throws Exception
    // {
    // final CMMCore core = MicroManager.getCore();
    //
    // if (core == null || !core.isSequenceRunning())
    // return;
    //
    // while (core.getRemainingImageCount() > 1)
    // core.popNextImage();
    // }

    /**
     * @deprecated Use {@link MicroManager#getLastImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage getLastImage() throws Exception
    {
        return MicroManager.getLastImage();
    }

    /**
     * @deprecated Use {@link MicroManager#getLastTaggedImage()} instead.
     */
    @Deprecated
    public static List<TaggedImage> getLastTaggedImage() throws Exception
    {
        return MicroManager.getLastTaggedImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapImage() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapTaggedImage()} instead.
     */
    @Deprecated
    public static List<TaggedImage> snapTaggedImage() throws Exception
    {
        return MicroManager.snapTaggedImage();
    }

    /**
     * Use this method to only get the data of a snapped image. Uses short.
     * 
     * @param core
     * @return Returns data in int. Returns null if an error occurs (Mostly when acquisition is
     *         running).
     * @deprecated use {@link #snapImageFromCore()} instead.
     */
    @Deprecated
    public static short[] snapImageToShort(CMMCore core)
    {
        if (core == null || core.isSequenceRunning())
            return null;

        try
        {
            core.snapImage();
            if (core.getBytesPerPixel() > 2)
                throw new RuntimeException("Dont' know how to handle images with "
                        + MicroManager.getCore().getBytesPerPixel() + " byte pixels.");

            return Array1DUtil.arrayToShortArray(core.getImage(), false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Use this method to get the last image of the acquisition sequence. If
     * sequence is running, will not wait for exposure.
     * 
     * @deprecated use {@link #snapImageFromLive()} instead.
     * @param core
     *        : reference to actual core
     * @return Returns a data short[] image. Returns null if an error occurs
     *         (mostly when acquisition sequence has not yet been run).
     */
    @Deprecated
    public static short[] getImageFromLiveToShort(CMMCore core)
    {
        if (core == null || !core.isSequenceRunning())
            return null;

        try
        {
            if (core.getBytesPerPixel() > 2)
                throw new RuntimeException("Dont' know how to handle images with "
                        + MicroManager.getCore().getBytesPerPixel() + " byte pixels.");

            return Array1DUtil.arrayToShortArray(core.getLastImage(), false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @deprecated Use {@link MicroManager#snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapIcyImageFromCore() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapImageFromLiveToIcy() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapImageFromCoreToIcy() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link #snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapIcyImage() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapToIcy() throws Exception
    {
        return MicroManager.snapImage();
    }

    /**
     * @deprecated Use {@link MicroManager#snapTaggedImage()} instead.
     */
    @Deprecated
    public static TaggedImage snapImageFromCore() throws Exception
    {
        final List<TaggedImage> results = MicroManager.snapTaggedImage();

        if (results == null)
            return null;

        return results.get(0);
    }

    /**
     * @deprecated Use {@link MicroManager#getLastImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapIcyImageFromLive(boolean remove) throws Exception
    {
        return MicroManager.getLastImage();
    }

    /**
     * @deprecated Use {@link MicroManager#getLastImage()} instead.
     */
    @Deprecated
    public static IcyBufferedImage snapIcyImageFromLive() throws Exception
    {
        return MicroManager.getLastImage();
    }

    /**
     * @deprecated Use {@link MicroManager#getLastTaggedImage()}
     */
    @Deprecated
    public static TaggedImage snapImageFromLive(boolean remove) throws Exception
    {
        final List<TaggedImage> results = MicroManager.getLastTaggedImage();

        if (results == null)
            return null;

        return results.get(0);
    }

    /**
     * @deprecated Use {@link MicroManager#getLastTaggedImage()} instead.
     */
    @Deprecated
    public static TaggedImage snapImageFromLive() throws Exception
    {
        final List<TaggedImage> results = MicroManager.getLastTaggedImage();

        if (results == null)
            return null;

        return results.get(0);
    }

    /**
     * @deprecated Use {@link MicroManager#snapTaggedImage()} instead.
     */
    @Deprecated
    public static TaggedImage snap() throws Exception
    {
        final List<TaggedImage> results = MicroManager.snapTaggedImage();

        if (results == null)
            return null;

        return results.get(0);
    }
}