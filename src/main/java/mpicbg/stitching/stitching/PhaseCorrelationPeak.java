package mpicbg.stitching.stitching;

import java.util.Arrays;

public class PhaseCorrelationPeak implements Comparable<PhaseCorrelationPeak> {

    private long[] position = null;
    private int[] originalInvPCMPosition = null;
    double phaseCorrelationPeak = 0;
    private float crossCorrelationPeak = 0;
    private boolean sortPhaseCorrelation = true;

    public PhaseCorrelationPeak(final long[] maxPos,
            final double maxValue) {
        this.position = maxPos.clone();
        this.phaseCorrelationPeak = maxValue;
    }

    @Override
    public String toString() {
        return "PhaseCorrelationPeak [position=" + Arrays.toString(position)
                + ", phaseCorrelationPeak=" + phaseCorrelationPeak + "]";
    }

    public long[] getPosition() {
        return position.clone();
    }

    public int[] getOriginalInvPCMPosition() {
        return originalInvPCMPosition.clone();
    }

    public double getPhaseCorrelationPeak() {
        return phaseCorrelationPeak;
    }

    public float getCrossCorrelationPeak() {
        return crossCorrelationPeak;
    }

    public boolean getSortPhaseCorrelation() {
        return sortPhaseCorrelation;
    }

    @Override
    public int compareTo(final PhaseCorrelationPeak o) {
        if (sortPhaseCorrelation) {
            if (this.phaseCorrelationPeak > o.phaseCorrelationPeak)
                return 1;
            else if (this.phaseCorrelationPeak == o.phaseCorrelationPeak)
                return 0;
            else
                return -1;
        }
        if (this.crossCorrelationPeak > o.crossCorrelationPeak) {
            return 1;
        }

        if (this.crossCorrelationPeak < o.crossCorrelationPeak) {
            return -1;
        }

        return 0;
    }

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(crossCorrelationPeak);
        result = prime * result + Arrays.hashCode(originalInvPCMPosition);
        long temp;
        temp = Double.doubleToLongBits(phaseCorrelationPeak);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + Arrays.hashCode(position);
        result = prime * result + (sortPhaseCorrelation ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PhaseCorrelationPeak other = (PhaseCorrelationPeak) obj;
        if (Float.floatToIntBits(crossCorrelationPeak) != Float
                .floatToIntBits(other.crossCorrelationPeak))
            return false;
        if (!Arrays.equals(originalInvPCMPosition,
                other.originalInvPCMPosition))
            return false;
        if (Double.doubleToLongBits(phaseCorrelationPeak) != Double
                .doubleToLongBits(other.phaseCorrelationPeak))
            return false;
        if (!Arrays.equals(position, other.position))
            return false;
        if (sortPhaseCorrelation != other.sortPhaseCorrelation)
            return false;
        return true;
    }
    
    

}
