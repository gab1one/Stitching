package mpicbg.stitching.stitching;

import java.util.ArrayList;
import java.util.List;

import mpicbg.stitching.utils.ComplexImageHelpers;
import mpicbg.stitching.utils.FixedSizePriorityQueue;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Pairwise Stitching of two ImagePlus using ImgLib1 and PhaseCorrelation. It
 * deals with aligning two slices (2d) or stacks (3d) having an arbitrary amount
 * of channels. If the ImagePlus contains several time-points it will only
 * consider the first time-point as this requires global optimization of many
 * independent 2d/3d <-> 2d/3d alignments.
 *
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class PairWiseStitchingImgLib {

    private static final float normalizationThreshold = 1E-5f;

    public static <T extends RealType<T>> PairWiseStitchingResult stitchPairwise(
            final ImgPlus<T> imp1, final ImgPlus<T> imp2, final int timepoint1,
            final int timepoint2, final StitchingParameters params,
            OpService opservice) {
        PairWiseStitchingResult result = null;

        result = computePhaseCorrelation(imp1, imp2, params, opservice);

        if (result == null) {
            // Log.error("Pairwise stitching failed.");
            return null;
        }

        // add the offset to the shift
        result.offset[0] -= imp2.max(0);
        result.offset[1] -= imp2.max(1);

        result.offset[0] += imp1.max(1);
        result.offset[1] += imp1.max(1);

        return result;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public static <T extends RealType<T>> PairWiseStitchingResult computePhaseCorrelation(
            final ImgPlus<T> img1, final ImgPlus<T> img2,
            StitchingParameters params, OpService ops) {

        int padding = 512;
        OutOfBoundsMirrorExpWindowingFactory<T, Img<T>> mirrorPad =
                new OutOfBoundsMirrorExpWindowingFactory<T, Img<T>>(padding);

        OutOfBoundsFactory<T, Img<T>> zeroPad =
                new OutOfBoundsConstantValueFactory<T, Img<T>>(
                        Util.getTypeFromInterval(img1).createVariable());

        long[] size = new long[] { 512, 512 };
        Img<FloatType> outManual = ArrayImgs.floats(size);

        // Img<ComplexFloatType> fft1 = (Img<ComplexFloatType>)
        // ops.run(FFT.class, img1.getImg(), mirrorPad);

        Img<ComplexFloatType> fft1 = (Img<ComplexFloatType>) ops.fft(img1);
        Img<ComplexFloatType> fft2 = (Img<ComplexFloatType>) ops.fft(img2);

//        ImageJFunctions.show(fft1, "fft 1");
//        ImageJFunctions.show(fft2, "fft 2");

        // TODO Create op for this!
        ComplexImageHelpers.normalizeComplexImage(fft1, normalizationThreshold);
        ComplexImageHelpers.normalizeAndConjugateComplexImage(fft2,
                normalizationThreshold);

//        ImageJFunctions.show(fft1, "normalized fft 1");
//        ImageJFunctions.show(fft2, "normalized, conjugated fft2");

        // multiply the complex images
        Cursor<ComplexFloatType> fft1cursor = fft1.cursor();
        Cursor<ComplexFloatType> fft2RA = fft2.cursor();

        while (fft1cursor.hasNext()) {
            fft1cursor.next().mul(fft2RA.next());
        }

        ops.ifft(outManual, fft1);
//        ImageJFunctions.show(outManual, "manual");

        List<PhaseCorrelationPeak> peaks =
                extractPhaseCorrelationPeaks(outManual, params.checkPeaks, ops);
        System.out.println(peaks.toString());

        verifyWithCrossCorrelation(peaks, size, img1, img2);

        if (params.subpixelAccuracy) {
            // TODO: Subpixel accuracy?
        }

        PhaseCorrelationPeak topPeak = peaks.get(peaks.size() - 1);

        PairWiseStitchingResult result = new PairWiseStitchingResult(
                topPeak.getPosition(), topPeak.phaseCorrelationPeak,
                topPeak.getCrossCorrelationPeak());

        return result;

    }

    private static <T extends RealType<T>> void verifyWithCrossCorrelation(
            final List<PhaseCorrelationPeak> peaks, final long[] dims,
            final ImgPlus<T> img1, final ImgPlus<T> img2) {

        // final boolean[][] coordinates =
        // Util.getRecursiveCoordinates(img1.numDimensions());

        @SuppressWarnings("unused")
        final ArrayList<PhaseCorrelationPeak> newPeakList =
                new ArrayList<PhaseCorrelationPeak>();

        // no need to wrap the point coordinates

        // //
        // // test them multithreaded
        // //
        // final AtomicInteger ai = new AtomicInteger(0);
        // Thread[] threads = SimpleMultiThreading.newThreads(4);
        // final int numThreads = threads.length;
        //
        // for (int ithread = 0; ithread < threads.length; ++ithread)
        // threads[ithread] = new Thread(new Runnable() {
        // public void run() {
        // final int myNumber = ai.getAndIncrement();
        //
        // for (int i = 0; i < newPeakList.size(); ++i)
        // if (i % numThreads == myNumber) {
        // final PhaseCorrelationPeak peak =
        // newPeakList.get(i);
        // final long[] numPixels = new long[1];
        //
        // peak.setCrossCorrelationPeak(
        // (float) testCrossCorrelation(
        // peak.getPosition(), image1, image2,
        // minOverlapPx, numPixels));
        // peak.setNumPixels(numPixels[0]);
        //
        // // sort by cross correlation peak
        // peak.setSortPhaseCorrelation(false);
        // }
        //
        // }
        // });
        //
        // SimpleMultiThreading.startAndJoin(threads);
        //
        // // update old list and sort
        // peakList.clear();
        // peakList.addAll(newPeakList);
        // Collections.sort(peakList);

    }

    /**
     * Extract the n best peaks in the phase correlation.
     * 
     * @param invPCM
     *            the Inverted Phase correlation matrix
     * @param numPeaks
     *            the number of peaks to extract
     * @param ops
     *            the Opservice to use
     * @return list of the n best peaks
     */
    private static final <T extends RealType<T>> List<PhaseCorrelationPeak> extractPhaseCorrelationPeaks(
            final Img<T> invPCM, final int numPeaks, OpService ops) {

        FixedSizePriorityQueue<PhaseCorrelationPeak> peaks =
                new FixedSizePriorityQueue<PhaseCorrelationPeak>(numPeaks);
        final int dims = invPCM.numDimensions();

        ExtendedRandomAccessibleInterval<T, Img<T>> extended =
                Views.extendZero(invPCM);
        IntervalView<T> interval = Views.interval(extended, invPCM);

        // TODO: OFFSETS?

        // Define neightborhood for the Peaks
        final int neighborhoodSize = 3; // TODO
        RectangleShape rs = new RectangleShape(neighborhoodSize, false);
        Cursor<Neighborhood<T>> neighbour = rs.neighborhoods(interval).cursor();
        // find local maximum in each neighborhood
        while (neighbour.hasNext()) {
            Cursor<T> nhCursor = neighbour.next().localizingCursor();
            double maxValue = 0.0d;
            long[] maxPos = new long[dims];
            while (nhCursor.hasNext()) {
                double localValue = nhCursor.next().getRealDouble();
                if (localValue > maxValue) {
                    maxValue = localValue;
                    nhCursor.localize(maxPos);
                }
            }
            // queue ensures only n best are added.
            peaks.add(new PhaseCorrelationPeak(maxPos, maxValue));
        }
        return peaks.asList();
    }
}
