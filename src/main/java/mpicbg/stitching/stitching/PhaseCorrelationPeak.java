package mpicbg.stitching.stitching;

import java.util.Arrays;

public class PhaseCorrelationPeak implements Comparable<PhaseCorrelationPeak> {

    private int[] position = null;
    private int[] originalInvPCMPosition = null;
    private float phaseCorrelationPeak = 0, crossCorrelationPeak = 0;
    private boolean sortPhaseCorrelation = true;

    public PhaseCorrelationPeak(final int[] position,
            final float phaseCorrelationPeak) {
        this.position = position.clone();
        this.phaseCorrelationPeak = phaseCorrelationPeak;
    }

    public int[] getPosition() {
        return position.clone();
    }

    public int[] getOriginalInvPCMPosition() {
        return originalInvPCMPosition.clone();
    }

    public float getPhaseCorrelationPeak() {
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
        result = prime * result + Float.floatToIntBits(phaseCorrelationPeak);
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
        if (!Arrays
                .equals(originalInvPCMPosition, other.originalInvPCMPosition))
            return false;
        if (Float.floatToIntBits(phaseCorrelationPeak) != Float
                .floatToIntBits(other.phaseCorrelationPeak))
            return false;
        if (!Arrays.equals(position, other.position))
            return false;
        if (sortPhaseCorrelation != other.sortPhaseCorrelation)
            return false;
        return true;
    }
    
    

}
