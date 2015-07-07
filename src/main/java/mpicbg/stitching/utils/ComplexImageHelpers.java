package mpicbg.stitching.utils;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.complex.ComplexFloatType;

public class ComplexImageHelpers {

    public static final <T extends ComplexFloatType> void normalizeComplexImage(
            final Img<T> fftImage, final float normalizationThreshold) {
        final Cursor<T> cursor = fftImage.cursor();

        while (cursor.hasNext()) {
            cursor.next();
            normalizeLength(cursor.get(), normalizationThreshold);
        }
    }

    public static final void normalizeAndConjugateComplexImage(
            final Img<ComplexFloatType> fftImage,
            final float normalizationThreshold) {
        final Cursor<ComplexFloatType> cursor = fftImage.cursor();

        while (cursor.hasNext()) {
            cursor.next();
            normalizeLength(cursor.get(), normalizationThreshold);
            cursor.get().complexConjugate();
        }
    }

    private static void normalizeLength(final ComplexFloatType input,
            final float threshold) {
        final float real = input.getRealFloat();
        final float complex = input.getImaginaryFloat();

        final float length = (float) Math.sqrt(real * real + complex * complex);

        if (length < threshold) {
            input.setReal(0);
            input.setImaginary(0);
        } else {
            input.setReal(real / length);
            input.setImaginary(complex / length);
        }
    }

}
