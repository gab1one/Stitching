package mpicbg.stitching.stitching;

import java.util.ArrayList;
import java.util.List;

import sun.tools.tree.WhileStatement;
import ij.ImagePlus;
import ij.gui.Roi;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.stitching.utils.FixedSizePriorityQueue;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;

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

        // FFT
        Img<ComplexFloatType> fftimg1 = (Img<ComplexFloatType>) ops.fft(img1);
        Img<ComplexFloatType> fftimg2 = (Img<ComplexFloatType>) ops.fft(img2);

        // TODO normalizeAndConjugate ?

        // Multiply
        // multiplyInPlace(fftimg1, fftimg2);
        ops.math().multiply(fftimg1, fftimg1, fftimg2);

        // TODO generalize types
        Img<FloatType> out =
                (Img<FloatType>) ops.createimg(img1.getImg(), new FloatType());
        // Inverse FFT
        ops.ifft(fftimg1, out);

        int numPeaks = params.checkPeaks;

        List<PhaseCorrelationPeak> peaks = extractPhaseCorrelationPeaks(out, numPeaks, ops);

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

    private static final <T extends Img<ComplexFloatType>> void multiplyInPlace(
            final T fftImage1, final T fftImage2) {
        final Cursor<ComplexFloatType> cursor1 = fftImage1.cursor();
        final Cursor<ComplexFloatType> cursor2 = fftImage2.cursor();

        while (cursor1.hasNext()) {
            cursor1.fwd();
            cursor2.fwd();

            cursor1.get().mul(cursor2.get());
        }
    }

    private static final List<PhaseCorrelationPeak> extractPhaseCorrelationPeaks(
            final Img<FloatType> invPCM, final int numPeaks, OpService ops) {

        FixedSizePriorityQueue<PhaseCorrelationPeak> peakQueue =
                new FixedSizePriorityQueue<>(numPeaks);
        int dims = invPCM.numDimensions();

        int neighborhoodSize = 3; // TODO

        // TODO Out of bounds = periodic
        // TODO: OFFSETS?

        RectangleShape rs = new RectangleShape(neighborhoodSize, false);
        Cursor<Neighborhood<FloatType>> neighbour =
                rs.neighborhoods(invPCM).cursor();
        while (neighbour.hasNext()) {
            Cursor<FloatType> nhCursor = neighbour.next().localizingCursor();

            float maxValue = 0.0f;
            int[] maxPos = new int[dims];
            while (nhCursor.hasNext()) {
                float localValue = nhCursor.next().get();
                if (localValue > maxValue) {
                    maxValue = localValue;
                    nhCursor.localize(maxPos);
                }
            }
            peakQueue.add(new PhaseCorrelationPeak(maxPos, maxValue));
        }
        return peakQueue.getAllElements();
    }

    /**
     * Determines if this imageplus with these parameters can be wrapped
     * directly into an Image<T>. This is important, because if we would wrap
     * the first but not the second image, they would have different
     * {@link ImageFactory}s
     *
     * @param imp
     *            - the ImagePlus
     * @param channel
     *            - which channel (if channel=0 means average all channels)
     *
     * @return true if it can be wrapped, otherwise false
     */
    public static boolean canWrapIntoImgLib(final ImagePlus imp, Roi roi,
            final int channel) {
        // first test the roi
        roi = getOnlyRectangularRoi(roi);

        return roi == null && channel > 0;
    }

    protected static Roi getOnlyRectangularRoi(final Roi roi) {
        // we can only do rectangular rois
        if (roi != null && roi.getType() != Roi.RECTANGLE) {
            return null;
        }
        return roi;
    }

}
