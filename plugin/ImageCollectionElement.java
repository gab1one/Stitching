package plugin;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mpicbg.models.Model;

public class ImageCollectionElement 
{
	final File file;
	ImagePlus imp = null;
	final int index;
	Model<?> model;
	int dimensionality;
	float[] offset;
	
	public ImageCollectionElement( final File file, final int index )
	{
		this.file = file;
		this.index = index;
	}
	
	public void setOffset( final float[] offset ) { this.offset = offset; }
	public float[] getOffset() { return offset; }
	public float getOffset( final int dim ) { return offset[ dim ]; }
	
	public int getIndex() { return index; }
	
	public void setModel( final Model<?> model ) { this.model = model; }
	public Model<?> getModel() { return model; }
	
	public void setDimensionality( final int dimensionality ) { this.dimensionality = dimensionality; }
	public int getDimensionality() { return dimensionality; }
	
	public File getFile() { return file; }
	
	public ImagePlus open()
	{
		if ( imp != null )
		{
			return imp;
		}
		else
		{
			try 
			{
				ImporterOptions options = new ImporterOptions();
				options.setId( file.getAbsolutePath() );
				options.setSplitChannels( false );
				options.setSplitTimepoints( false );
				options.setSplitFocalPlanes( false );
				options.setAutoscale( false );
				
				final ImagePlus[] imp = BF.openImagePlus( file.getAbsolutePath() );
				
				if ( imp.length > 1 )
				{
					IJ.log( "LOCI does not open the file '" + file + "'correctly, it opens the image and splits it - maybe you should convert all input files first to TIFF?" );
					return null;
				}
				else
				{
					return imp[ 0 ];
				}
				
			} catch ( Exception e ) 
			{
				IJ.log( "Cannot open file '" + file + "': " + e );
				e.printStackTrace();
				return null;
			} 
			
		}
	}
}