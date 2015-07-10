package mpicbg.stitching.stitching;

public class PairWiseStitchingResult {
    long[] offset;
    double crossCorrelation;
    double phaseCorrelation;

    public PairWiseStitchingResult(final long[] offset,
            final double crossCorrelation, final double phaseCorrelation) {
        this.offset = offset;
        this.crossCorrelation = crossCorrelation;
        this.phaseCorrelation = phaseCorrelation;
    }

    public int getNumDimensions() {
        return offset.length;
    }

    public long[] getOffset() {
        return offset;
    }

    public float getOffset(final int dim) {
        return offset[dim];
    }

    public double getCrossCorrelation() {
        return crossCorrelation;
    }

    public double getPhaseCorrelation() {
        return phaseCorrelation;
    }
}
