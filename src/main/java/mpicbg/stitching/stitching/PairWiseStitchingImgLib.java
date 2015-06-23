package mpicbg.stitching.stitching;

import ij.ImagePlus;
import ij.gui.Roi;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.stitching.utils.Log;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops.Mean;
import net.imglib2.type.numeric.RealType;

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
            Log.error("Pairwise stitching failed.");
            return null;
        }

        // add the offset to the shift
        result.offset[0] -= imp2.max(0);
        result.offset[1] -= imp2.max(1);

        result.offset[0] += imp1.max(1);
        result.offset[1] += imp1.max(1);

        return result;
    }

    @SuppressWarnings("deprecation")
    public static <T extends RealType<T>> PairWiseStitchingResult computePhaseCorrelation(
            final ImgPlus<T> img1, final ImgPlus<T> img2,
            StitchingParameters params, OpService ops) {

        Mean meanOp = ops.op(Mean.class, RealType.class);

        ImgPlus<T> flatImg1 =
                (ImgPlus<T>) ops.project(img1, meanOp, params.channel1);

        ImgPlus<T> flatImg2 =
                (ImgPlus<T>) ops.project(img2, meanOp, params.channel2);

        Object a = ops.fft(flatImg1, flatImg2);

        // final PhaseCorrelation<T, S> phaseCorr =
        // new PhaseCorrelation<T, S>(img1, img2);
        // phaseCorr.setInvestigateNumPeaks(numPeaks);
        //
        // phaseCorr.setKeepPhaseCorrelationMatrix(subpixelAccuracy);
        //
        // phaseCorr.setComputeFFTinParalell(true);
        // if (!phaseCorr.process()) {
        // Log.error("Could not compute phase correlation: "
        // + phaseCorr.getErrorMessage());
        // return null;
        // }
        //
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
