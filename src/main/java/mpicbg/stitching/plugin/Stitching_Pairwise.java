package mpicbg.stitching.plugin;

import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.util.ArrayList;

import mpicbg.models.InvertibleBoundable;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.stitching.PairWiseStitchingResult;
import mpicbg.stitching.stitching.StitchingParameters;
import mpicbg.stitching.utils.Log;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

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
        }
        // now fuse
        // return fuseImg(imp1, imp2, params, models);
        return null;
    }

    private static <T extends RealType<T>> void singleTimepointStitching(
            final ImgPlus<T> imp1, final ImgPlus<T> imp2,
            final StitchingParameters params,
            final ArrayList<InvertibleBoundable> models, OpService opservice) {
        // compute the stitching
        final long start = System.currentTimeMillis();

        final PairWiseStitchingResult result;
        
        // Always compute overlap!
        result =
                PairWiseStitchingImgLib.stitchPairwise(imp1, imp2, 1, 1,
                        params, opservice);
        Log.info("shift (second relative to first): "
                + Util.printCoordinates(result.getOffset())
                + " correlation (R)=" + result.getCrossCorrelation() + " ("
                + (System.currentTimeMillis() - start) + " ms)");

        // update the dialog to show the numbers next time

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

    // /**
    // * Fuses an Image selecting the right parameters for
    // * {@link #fuse(RealType, ImagePlus, ImagePlus, ArrayList,
    // StitchingParameters)}
    // *
    // * @param imp1
    // * @param imp2
    // * @param params
    // * @param models
    // * @return
    // */
    // private static <T extends RealType<T>> ImgPlus<T> fuseImg(
    // final ImgPlus<T> imp1, final ImgPlus<T> imp2,
    // final StitchingParameters params,
    // final ArrayList<InvertibleBoundable> models) {
    // Log.info("Fusing ...");
    //
    // final ImgPlus<T> resultImg;
    // final long start = System.currentTimeMillis();
    //
    // if (imp1.getType() == ImagePlus.GRAY32
    // || imp2.getType() == ImagePlus.GRAY32) {
    // resultImg = fuse(new FloatType(), imp1, imp2, models, params);
    // } else if (imp1.getType() == ImagePlus.GRAY16
    // || imp2.getType() == ImagePlus.GRAY16) {
    // resultImg =
    // fuse(new UnsignedShortType(), imp1, imp2, models, params);
    // } else {
    // resultImg =
    // fuse(new UnsignedByteType(), imp1, imp2, models, params);
    // }
    // Log.info("Finished ... (" + (System.currentTimeMillis() - start)
    // + " ms)");
    // return resultImg;
    // }

    // private static <T extends RealType<T>> ImagePlus fuse(final T targetType,
    // final ImagePlus imp1, final ImagePlus imp2,
    // final ArrayList<InvertibleBoundable> models,
    // final StitchingParameters params) {
    // final ArrayList<ImagePlus> images = new ArrayList<ImagePlus>();
    // images.add(imp1);
    // images.add(imp2);
    //
    // if (params.fusionMethod != FusionType.OVERLAY
    // || params.fusionMethod != FusionType.INTENSITY_RANDOM_TILE) {
    // final ImagePlus imp =
    // Fusion.fuse(targetType, images, models,
    // params.dimensionality, params.subpixelAccuracy,
    // params.fusionMethod, null, false,
    // params.ignoreZeroValuesFusion, params.displayFusion);
    // return imp;
    // } else if (params.fusionMethod == FusionType.OVERLAY) // overlay
    // {
    // // images are always the same, we just trigger different timepoints
    // final InterpolatorFactory<FloatType, RandomAccessible<FloatType>>
    // factory;
    //
    // if (params.subpixelAccuracy) {
    // factory = new NLinearInterpolatorFactory<FloatType>();
    // } else {
    // factory = new NearestNeighborInterpolatorFactory<FloatType>();
    // }
    //
    // // fuses the first timepoint but estimates the boundaries for all
    // // timepoints as it gets all models
    // final CompositeImage timepoint0 =
    // OverlayFusion.createOverlay(targetType, images, models,
    // params.dimensionality, 1, factory);
    //
    // if (imp1.getNFrames() > 1) {
    // final ImageStack stack =
    // new ImageStack(timepoint0.getWidth(),
    // timepoint0.getHeight());
    //
    // // add all slices of the first timepoint
    // for (int c = 1; c <= timepoint0.getStackSize(); ++c) {
    // stack.addSlice("", timepoint0.getStack().getProcessor(c));
    // }
    //
    // // "Overlay into composite image"
    // for (int f = 2; f <= imp1.getNFrames(); ++f) {
    // final CompositeImage tmp =
    // OverlayFusion.createOverlay(targetType, images,
    // models, params.dimensionality, f, factory);
    //
    // // add all slices of the first timepoint
    // for (int c = 1; c <= tmp.getStackSize(); ++c) {
    // stack.addSlice("", tmp.getStack().getProcessor(c));
    // }
    // }
    //
    // // convertXYZCT ...
    // final ImagePlus result = new ImagePlus(params.fusedName, stack);
    //
    // // numchannels, z-slices, timepoints (but right now the order is
    // // still XYZCT)
    // result.setDimensions(timepoint0.getNChannels(),
    // timepoint0.getNSlices(), imp1.getNFrames());
    // return CompositeImageFixer.makeComposite(result,
    // CompositeImage.COMPOSITE);
    // } else {
    // timepoint0.setTitle(params.fusedName);
    // return timepoint0;
    // }
    // } else {
    // // "Do not fuse images"
    // return null;
    // }
    // }
    //
    // protected static <T extends RealType<T>> List<ComparePair>
    // getComparePairs(
    // final ImgPlus<T> imp1, final ImgPlus<T> imp2,
    // final int dimensionality, final int timeSelect) {
    // final Model<?> model;
    //
    // if (dimensionality == 2) {
    // model = new TranslationModel2D();
    // } else {
    // model = new TranslationModel3D();
    // }
    //
    // final ArrayList<ImagePlusTimePoint> listImp1 =
    // new ArrayList<ImagePlusTimePoint>();
    // final ArrayList<ImagePlusTimePoint> listImp2 =
    // new ArrayList<ImagePlusTimePoint>();
    //
    // for (int timePoint1 = 1; timePoint1 <= imp1.getNFrames(); timePoint1++) {
    // listImp1.add(new ImagePlusTimePoint(imp1, 1, timePoint1, model
    // .copy(), null));
    // }
    //
    // for (int timePoint2 = 1; timePoint2 <= imp2.getNFrames(); timePoint2++) {
    // listImp2.add(new ImagePlusTimePoint(imp2, 2, timePoint2, model
    // .copy(), null));
    // }
    //
    // final List<ComparePair> pairs = new ArrayList<ComparePair>();
    //
    // // imp1 vs imp2 at all timepoints
    // for (int timePointA = 1; timePointA <= Math.min(imp1.getNFrames(),
    // imp2.getNFrames()); timePointA++) {
    // final ImagePlusTimePoint a = listImp1.get(timePointA - 1);
    // final ImagePlusTimePoint b = listImp2.get(timePointA - 1);
    // pairs.add(new ComparePair(a, b));
    // }
    //
    // if (timeSelect == 1) {
    // // consequtively all timepoints of imp1
    // for (int timePointA = 1; timePointA <= imp1.getNFrames() - 1;
    // timePointA++) {
    // pairs.add(new ComparePair(listImp1.get(timePointA - 1),
    // listImp1.get(timePointA + 1 - 1)));
    // }
    //
    // // consequtively all timepoints of imp2
    // for (int timePointB = 1; timePointB <= imp2.getNFrames() - 1;
    // timePointB++) {
    // pairs.add(new ComparePair(listImp2.get(timePointB - 1),
    // listImp2.get(timePointB + 1 - 1)));
    // }
    //
    // } else {
    // // all against all for imp1
    // for (int timePointA = 1; timePointA <= imp1.getNFrames() - 1;
    // timePointA++) {
    // for (int timePointB = timePointA + 1; timePointB <= imp1
    // .getNFrames(); timePointB++) {
    // pairs.add(new ComparePair(listImp1.get(timePointA - 1),
    // listImp1.get(timePointB - 1)));
    // }
    // }
    //
    // // all against all for imp2
    // for (int timePointA = 1; timePointA <= imp2.getNFrames() - 1;
    // timePointA++) {
    // for (int timePointB = timePointA + 1; timePointB <= imp2
    // .getNFrames(); timePointB++) {
    // pairs.add(new ComparePair(listImp2.get(timePointA - 1),
    // listImp2.get(timePointB - 1)));
    // }
    // }
    // }
    //
    // return pairs;
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

        if (numSlices1 == 1 && numSlices2 != 1 || numSlices1 != 1
                && numSlices2 == 1) {
            return "One image is 2d and the other one is 3d, cannot proceed...";
        }

        return null;
    }

}