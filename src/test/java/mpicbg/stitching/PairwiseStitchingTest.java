package mpicbg.stitching;

import mpicbg.stitching.plugin.Stitching_Pairwise;
import mpicbg.stitching.stitching.StitchingParameters;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;
import org.scijava.plugin.Parameter;

import other.ops.AbstractOpTest;

public class PairwiseStitchingTest extends AbstractOpTest {

    @Parameter
    OpService ops;
    
    @Test
    public void test() {
        StitchingParameters params = new StitchingParameters();
        
        ImgPlus<FloatType> imp1 = ImgPlus.wrap(generateFloatArrayTestImg(true, 300,300));
        ImgPlus<FloatType> imp2 = ImgPlus.wrap(generateFloatArrayTestImg(true, 300,300));
        
       Stitching_Pairwise.performPairWiseStitching(imp1, imp2, params, ops);
        
    }

}
