package mpicbg.stitching.stitching;

import java.util.ArrayList;
import java.util.List;

import mpicbg.stitching.utils.ComplexImageHelpers;
import mpicbg.stitching.utils.FixedSizePriorityQueue;
import net.imagej.ImgPlus;
import net.imagej.ops.AbstractInplaceFunction;
import net.imagej.ops.AbstractOutputFunction;
import net.imagej.ops.InplaceFunction;
import net.imagej.ops.OpService;
import net.imagej.ops.convolve.CorrelateFFTImg;
import net.imagej.ops.fft.filter.CreateFFTFilterMemory;
import net.imagej.ops.join.AbstractJoinFunctions;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.outofbounds.OutOfBoundsPeriodicFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.DoubleType;
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

        int padding = 0;
        OutOfBoundsMirrorExpWindowingFactory<T, Img<T>> zeroPad =
                new OutOfBoundsMirrorExpWindowingFactory<T, Img<T>>(padding);

        long[] size = new long[] { 512, 512 };
        Img<FloatType> outManual = ArrayImgs.floats(size);

        Img<ComplexFloatType> fft1 = (Img<ComplexFloatType>) ops.fft(img1);
        Img<ComplexFloatType> fft2 = (Img<ComplexFloatType>) ops.fft(img2);

        // TODO Create op for this!
        ComplexImageHelpers.normalizeComplexImage(fft1, normalizationThreshold);
        ComplexImageHelpers.normalizeAndConjugateComplexImage(fft2,
                normalizationThreshold);

        // // multiply the complex images
        // Cursor<ComplexFloatType> fft1cursor = fft1.localizingCursor();
        // RandomAccess<ComplexFloatType> fft2RA = fft2.randomAccess();
        //
        // while (fft1cursor.hasNext()) {
        // fft1cursor.fwd();
        // fft2RA.setPosition(fft1cursor);
        // fft1cursor.get().mul(fft2RA.get());
        // }
        //

        // multiply the complex images
        Cursor<ComplexFloatType> fft1cursor = fft1.cursor();
        Cursor<ComplexFloatType> fft2RA = fft2.cursor();

        while (fft1cursor.hasNext()) {
            fft1cursor.next().mul(fft2RA.next());
        }

        ops.ifft(outManual, fft1);
        ImageJFunctions.show(outManual, "manual");

        Img<FloatType> outAuto = ArrayImgs.floats(size);

        ops.correlate(outAuto, img1, img2, size, zeroPad, zeroPad);

        ImageJFunctions.show(outAuto, "opp-a-licious");

        List<PhaseCorrelationPeak> peaks =
                extractPhaseCorrelationPeaks(outManual, params.checkPeaks, ops);
        System.out.println(peaks.toString());
        // out.toString();
        // // FFT
        // Img<ComplexFloatType> fftimg1 = (Img<ComplexFloatType>)
        // ops.fft(img1);
        // Img<ComplexFloatType> fftimg2 = (Img<ComplexFloatType>)
        // ops.fft(img2);
        //
        // // TODO normalizeAndConjugate ?
        //
        // // Multiply
        // // multiplyInPlace(fftimg1, fftimg2); // TODO Test if same output as
        // \/
        // ops.math().multiply(fftimg1, fftimg1, fftimg2);
        //
        // // TODO generalize types
        // Img<FloatType> out =
        // (Img<FloatType>) ops.createimg(img1.getImg(), new FloatType());
        // // Inverse FFT
        // ops.ifft(fftimg1, out);
        //
        // int numPeaks = params.checkPeaks;
        //
        // List<PhaseCorrelationPeak> peaks =
        // extractPhaseCorrelationPeaks(out, numPeaks, ops);
        //
        // long[] dims = new long[out.numDimensions()];
        // out.dimensions(dims);
        // verifyWithCrossCorrelation(peaks, dims, img1, img2);
        //
        // phaseCorr.setKeepPhaseCorrelationMatrix(subpixelAccuracy);

        // // result
        // final PhaseCorrelationPeak pcp = phaseCorr.getShift();
        // final float[] shift = new float[img1.getNumDimensions()];
        // final PairWiseStitchingResult result;
        //
        // if (subpixelAccuracy) {
        // final Image<FloatType> pcm = phaseCorr.getPhaseCorrelationMatrix();
        //
        // final ArrayList<DifferenceOfGaussianPeak<FloatType>> list =
        // new ArrayList<DifferenceOfGaussianPeak<FloatType>>();
        // final Peak p = new Peak(pcp);
        // list.add(p);
        //
        // final SubpixelLocalization<FloatType> spl =
        // new SubpixelLocalization<FloatType>(pcm, list);
        // final boolean move[] = new boolean[pcm.getNumDimensions()];
        // for (int i = 0; i < pcm.getNumDimensions(); ++i) {
        // move[i] = false;
        // }
        // spl.setCanMoveOutside(true);
        // spl.setAllowedToMoveInDim(move);
        // spl.setMaxNumMoves(0);
        // spl.setAllowMaximaTolerance(false);
        // spl.process();
        //
        // final Peak peak = (Peak) list.get(0);
        //
        // for (int d = 0; d < img1.getNumDimensions(); ++d) {
        // shift[d] =
        // peak.getPCPeak().getPosition()[d]
        // + peak.getSubPixelPositionOffset(d);
        // }
        //
        // pcm.close();
        //
        // result =
        // new PairWiseStitchingResult(shift,
        // pcp.getCrossCorrelationPeak(), p.getValue().get());
        // } else {
        // for (int d = 0; d < img1.getNumDimensions(); ++d) {
        // shift[d] = pcp.getPosition()[d];
        // }
        //
        // result =
        // new PairWiseStitchingResult(shift,
        // pcp.getCrossCorrelationPeak(),
        // pcp.getPhaseCorrelationPeak());
        // }
        //
        // return result;
        return null;
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
