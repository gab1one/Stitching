package mpicbg.stitching.plugin;

import java.util.ArrayList;

import ij.ImagePlus;
import ij.plugin.PlugIn;
import mpicbg.models.InvertibleBoundable;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.stitching.PairWiseStitchingResult;
import mpicbg.stitching.stitching.StitchingParameters;
import mpicbg.stitching.utils.Log;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class Stitching_Pairwise implements PlugIn {
    final private String myURL = "http://fly.mpi-cbg.de/preibisch";
    final private String paperURL =
            "http://bioinformatics.oxfordjournals.org/cgi/content/abstract/btp184";

    public static int defaultImg1 = 0;
    public static int defaultImg2 = 1;
    public static int defaultChannel1 = 0;
    public static int defaultChannel2 = 0;
    public static int defaultTimeSelect = 1;
    public static boolean defaultFuseImages = true;
    public static int defaultFusionMethod = 0;
    public static boolean defaultIgnoreZeroValues = false;
    public static boolean defaultComputeOverlap = true;
    public static boolean defaultSubpixelAccuracy = false;
    public static int defaultCheckPeaks = 5;
    public static double defaultxOffset = 0, defaultyOffset = 0,
            defaultzOffset = 0;

    public static boolean[] defaultHandleChannel1 = null;
    public static boolean[] defaultHandleChannel2 = null;

    public static int defaultMemorySpeedChoice = 0;
    public static double defaultDisplacementThresholdRelative = 2.5;
    public static double defaultDisplacementThresholdAbsolute = 3.5;

    @Override
    public void run(final String arg0) {
    }

    public static <T extends RealType<T>> ImgPlus<T> performPairWiseStitching(
            final ImgPlus<T> imp1, final ImgPlus<T> imp2,
            final StitchingParameters params, OpService ops) {
        final ArrayList<InvertibleBoundable> models =
                new ArrayList<InvertibleBoundable>();

        // the simplest case, only one registration necessary
        if (imp1.dimension(params.channel1) == 1 || params.timeSelect == 0) {
            singleTimepointStitching(imp1, imp2, params, models, ops);
        } else {
            // multiTimepointStitching(imp1, imp2, params, models, ops);
            // TODO: IMPLEMENT
        }
        return fuseImg(imp1, imp2, params, models, ops);
    }

    private static <T extends RealType<T>> void singleTimepointStitching(
            final ImgPlus<T> imp1, final ImgPlus<T> imp2,
            final StitchingParameters params,
            final ArrayList<InvertibleBoundable> models, OpService opservice) {
        // compute the stitching
        final long start = System.currentTimeMillis();

        final PairWiseStitchingResult result;

        // Always compute overlap!
        result = PairWiseStitchingImgLib.stitchPairwise(imp1, imp2, 1, 1,
                params, opservice);

        Log.info("shift (second relative to first): "
                + Util.printCoordinates(result.getOffset())
                + " correlation (R)=" + result.getCrossCorrelation() + " ("
                + (System.currentTimeMillis() - start) + " ms)");

        // update the dialog to show the numbers next time
        params.dimensionality = 2;
        for (int f = 1; f <= imp1.dimension(params.channel1); ++f) {
            if (params.dimensionality == 2) {
                final TranslationModel2D model1 = new TranslationModel2D();
                final TranslationModel2D model2 = new TranslationModel2D();
                model2.set(result.getOffset(0), result.getOffset(1));

                models.add(model1);
                models.add(model2);
            } else {
                final TranslationModel3D model1 = new TranslationModel3D();
                final TranslationModel3D model2 = new TranslationModel3D();
                model2.set(result.getOffset(0), result.getOffset(1),
                        result.getOffset(2));

                models.add(model1);
                models.add(model2);
            }
        }
    }

    /**
     * Fuses an Image selecting the right parameters for
     * {@link #fuse(RealType, ImagePlus, ImagePlus, ArrayList, StitchingParameters)}
     *
     * @param imp1
     * @param imp2
     * @param params
     * @param models
     * @return
     */
    private static <T extends RealType<T>> ImgPlus<T> fuseImg(
            final ImgPlus<T> imp1, final ImgPlus<T> imp2,
            final StitchingParameters params,
            final ArrayList<InvertibleBoundable> models, OpService ops) {
        Log.info("Fusing ...");

        final long start = System.currentTimeMillis();
        final ImgPlus<T> resultImg = fuse(imp1, imp2, params, models, ops);

        Log.info("Finished ... (" + (System.currentTimeMillis() - start)
                + " ms)");
        return resultImg;
    }

    private static <T extends RealType<T>> ImgPlus<T> fuse(
            final ImgPlus<T> img1, final ImgPlus<T> img2,
            final StitchingParameters params,
            final ArrayList<InvertibleBoundable> models, OpService ops) {
        final ArrayList<ImgPlus<T>> images = new ArrayList<ImgPlus<T>>();
        images.add(img1);
        images.add(img2);
        
        RandomAccess<T> origRA = img2.randomAccess();
        origRA.setPosition(new int[]{10,10}};
        
        RandomAccess<T> offsetimg2 = Views.offset(img2, 100, 0).randomAccess();
        offsetimg2.setPosition(10, 10);
        T a = offsetimg2.get();
        T b = origRA.get();

        System.out.println(a.compareTo(b));

        // if (params.fusionMethod != FusionType.OVERLAY
        // || params.fusionMethod != FusionType.INTENSITY_RANDOM_TILE) {
        // final ImagePlus imp = Fusion.fuse(targetType, images, models,
        // params.dimensionality, params.subpixelAccuracy,
        // params.fusionMethod, null, false,
        // params.ignoreZeroValuesFusion, params.displayFusion);
        // return imp;
        // } else {
        // // "Do not fuse images"
        // return null;
        // }
        return null;
    }

    //
    // private static <T extends RealType<T>> void multiTimepointStitching(
    // final ImgPlus<T> imp1, final ImgPlus<T> imp2,
    // final StitchingParameters params,
    // final ArrayList<InvertibleBoundable> models, OpService ops) {
    // // get all that we have to compare
    // final List<ComparePair> pairs =
    // getComparePairs(imp1, imp2, params.dimensionality,
    // params.timeSelect);
    //
    // // compute all compare pairs
    // // compute all matchings
    // final AtomicInteger ai = new AtomicInteger(0);
    //
    // final int numThreads;
    //
    // if (params.cpuMemChoice == 0) {
    // numThreads = 1;
    // } else {
    // numThreads = Runtime.getRuntime().availableProcessors();
    // }
    //
    // final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
    //
    // // for (int ithread = 0; ithread < threads.length; ++ithread) {
    // // threads[ithread] = new Thread(new Runnable() {
    // // @Override
    // // public void run() {
    // // final int myNumber = ai.getAndIncrement();
    // //
    // // for (int i = 0; i < pairs.size(); i++) {
    // // if (i % numThreads == myNumber) {
    // // final ComparePair pair = pairs.get(i);
    // //
    // // final long start = System.currentTimeMillis();
    // //
    // // final PairWiseStitchingResult result =
    // // PairWiseStitchingImgLib.stitchPairwise(pair
    // // .getImagePlus1(), pair
    // // .getImagePlus2(), pair
    // // .getTimePoint1(), pair
    // // .getTimePoint2(), params, ops);
    // //
    // // if (params.dimensionality == 2) {
    // // pair.setRelativeShift(new float[] {
    // // result.getOffset(0),
    // // result.getOffset(1) });
    // // } else {
    // // pair.setRelativeShift(new float[] {
    // // result.getOffset(0),
    // // result.getOffset(1),
    // // result.getOffset(2) });
    // // }
    // //
    // // pair.setCrossCorrelation(result
    // // .getCrossCorrelation());
    // //
    // // Log.info(pair.getImagePlus1().getTitle() + "["
    // // + pair.getTimePoint1() + "]" + " <- "
    // // + pair.getImagePlus2().getTitle() + "["
    // // + pair.getTimePoint2() + "]" + ": "
    // // + Util.printCoordinates(result.getOffset())
    // // + " correlation (R)="
    // // + result.getCrossCorrelation() + " ("
    // // + (System.currentTimeMillis() - start)
    // // + " ms)");
    // // }
    // // }
    // // }
    // // });
    // // }
    // //
    // // SimpleMultiThreading.startAndJoin(threads);
    //
    // // get the final positions of all tiles
    // final ArrayList<ImagePlusTimePoint> optimized =
    // GlobalOptimization.optimize(pairs, pairs.get(0).getTile1(),
    // params);
    //
    // for (int f = 0; f < imp1.getNFrames(); ++f) {
    // Log.info(optimized.get(f * 2).getImagePlus().getTitle() + "["
    // + optimized.get(f * 2).getImpId() + ","
    // + optimized.get(f * 2).getTimePoint() + "]: "
    // + optimized.get(f * 2).getModel());
    // Log.info(optimized.get(f * 2 + 1).getImagePlus().getTitle() + "["
    // + optimized.get(f * 2 + 1).getImpId() + ","
    // + optimized.get(f * 2 + 1).getTimePoint() + "]: "
    // + optimized.get(f * 2 + 1).getModel());
    // models.add((InvertibleBoundable) optimized.get(f * 2).getModel());
    // models.add((InvertibleBoundable) optimized.get(f * 2 + 1)
    // .getModel());
    // }
    // }

    public static String testRegistrationCompatibility(final ImagePlus imp1,
            final ImagePlus imp2) {
        // test time points
        final int numFrames1 = imp1.getNFrames();
        final int numFrames2 = imp2.getNFrames();

        if (numFrames1 != numFrames2) {
            return "Images have a different number of time points, cannot proceed...";
        }

        // test if both have 2d or 3d image contents
        final int numSlices1 = imp1.getNSlices();
        final int numSlices2 = imp2.getNSlices();

        if (numSlices1 == 1 && numSlices2 != 1
                || numSlices1 != 1 && numSlices2 == 1) {
            return "One image is 2d and the other one is 3d, cannot proceed...";
        }

        return null;
    }

}
