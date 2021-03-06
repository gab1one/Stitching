package mpicbg.stitching.plugin;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loci.common.services.ServiceFactory;
import loci.formats.ChannelSeparator;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import mpicbg.models.TranslationModel2D;
import mpicbg.models.TranslationModel3D;
import mpicbg.stitching.math.CommonFunctions;
import mpicbg.stitching.stitching.Downsampler;
import mpicbg.stitching.stitching.ImageCollectionElement;
import mpicbg.stitching.stitching.ImagePlusTimePoint;
import mpicbg.stitching.stitching.TextFileAccess;
import mpicbg.stitching.utils.Log;
import ome.units.quantity.Length;

/**
 *
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Stitching_Grid implements PlugIn {
    final public static String version = "1.2";
    final private String myURL = "http://fly.mpi-cbg.de/preibisch";

    public static boolean seperateOverlapY = false;

    public static int defaultGridChoice1 = 0;
    public static int defaultGridChoice2 = 0;

    public static int defaultGridSizeX = 2, defaultGridSizeY = 3;
    public static double defaultOverlapX = 20;
    public static double defaultOverlapY = 20;

    public static String defaultDirectory = "";
    public static String defaultSeriesFile = "";
    public static boolean defaultConfirmFiles = true;

    public static String defaultFileNames = "tile_{ii}.tif";
    public static String defaultTileConfiguration = "TileConfiguration.txt";
    public static boolean defaultAddTilesAsRois = false;
    public static boolean defaultComputeOverlap = true;
    public static boolean defaultInvertX = false;
    public static boolean defaultInvertY = false;
    public static boolean defaultIgnoreZStage = false;
    public static boolean defaultSubpixelAccuracy = false;
    public static boolean defaultDownSample = false;
    public static boolean defaultDisplayFusion = false;
    public static boolean writeOnlyTileConfStatic = false;

    public static boolean defaultIgnoreCalibration = false;
    public static double defaultIncreaseOverlap = 0;
    public static boolean defaultVirtualInput = false;

    public static int defaultStartI = 1;
    public static int defaultStartX = 1;
    public static int defaultStartY = 1;
    public static int defaultFusionMethod = 0;
    public static double defaultR = 0.3;
    public static double defaultRegressionThreshold = 0.3;
    public static double defaultDisplacementThresholdRelative = 2.5;
    public static double defaultDisplacementThresholdAbsolute = 3.5;
    public static boolean defaultOnlyPreview = false;
    public static int defaultMemorySpeedChoice = 0;

    // Added by John Lapage: user sets this parameter to define how many
    // adjacent files each image will be compared to
    public static double defaultSeqRange = 1;

    public static boolean defaultQuickFusion = true;

    public static String[] resultChoices = { "Fuse and display",
    "Write to disk" };
    public static int defaultResult = 0;
    public static String defaultOutputDirectory = "";

    @Override
    public void run(final String arg0) {
    }

    /**
     * Generates a ROI for each tile in the list of optimized images. The
     * fusedImage is the resultant on which the ROIs will be drawn. The offset
     * is a global offset to 0,0 for the upper leftmost tile.
     */
    protected void generateRois(final double[] offsets,
            final ArrayList<ImagePlusTimePoint> optimizedImages) {
        IJ.showStatus("Generating ROIs from image tiles...");

        RoiManager rm = RoiManager.getInstance();
        if (rm == null) {
            rm = new RoiManager();
        }

        // we'll make a map of rois to their ImageStack slice #, allowing them
        // to be added to the RoiManager in the correct order.
        final Map<Integer, List<Roi>> roisBySlice =
                new HashMap<Integer, List<Roi>>();

        // This reader is used to determine associations between tiles and files
        // on disk
        IFormatReader reader = null;

        // Generate ROIs
        for (int i = 0; i < optimizedImages.size(); i++) {
            // Each element of optimizedImages is assumed, for a given zct, to
            // be part
            // of the same tile (slice)
            final ImagePlusTimePoint iptp = optimizedImages.get(i);
            reader =
                    initializeReader(reader, iptp.getElement().getFile()
                            .getAbsolutePath());
            final int sizeZ = reader.getSizeZ();
            final int sizeT = reader.getSizeT();
            final int sizeC = reader.getSizeC();

            // Skip any .xml, .cfg, etc... when looking up the image names
            String[] seriesFiles = reader.getSeriesUsedFiles(true);
            final int pixelOffset =
                    seriesFiles == null ? 0 : seriesFiles.length;
            // Assume that each series correlates to an element of
            // optimizedImages
            if (reader.getSeriesCount() > 1) {
                reader.setSeries(i);
            }
            seriesFiles = reader.getSeriesUsedFiles();

            final ImagePlus unfused = iptp.getImagePlus();
            // ROI/ImageJ slice number
            int slice = 1;
            // compute the x,y coordinates within this slice
            final double[] coords =
                    new double[iptp.getElement().getDimensionality()];
            iptp.getModel().applyInPlace(coords);
            for (int j = 0; j < offsets.length; j++) {
                coords[j] -= offsets[j];
            }
            final int coordXOffset = (int) Math.floor(coords[0]);
            final int coordYOffset = (int) Math.floor(coords[1]);
            for (int t = 0; t < sizeT; t++) {
                for (int z = 0; z < sizeZ; z++) {
                    for (int c = 0; c < sizeC; c++) {
                        final Roi roi =
                                new Roi(coordXOffset, coordYOffset,
                                        unfused.getWidth(), unfused.getHeight());

                        // set roi name and position
                        roi.setPosition(c + 1, z + 1, t + 1);
                        String roiName =
                                "sp=" + slice + "; label=" + i + "; series="
                                        + reader.getSeries() + "; C=" + (c + 1)
                                        + "; Z=" + (z + 1) + "; T=" + (t + 1);
                        if (reader != null) {
                            String sourceName =
                                    "unknown file " + reader.getIndex(z, c, t);
                            if (seriesFiles != null
                                    && seriesFiles.length > reader.getIndex(z,
                                            c, t) + pixelOffset) {
                                sourceName =
                                        new File(seriesFiles[reader.getIndex(z,
                                                c, t) + pixelOffset]).getName();
                            }
                            roiName += "; file=" + sourceName;
                        }
                        roi.setName(roiName);

                        if (roisBySlice.get(slice) == null) {
                            roisBySlice.put(slice, new ArrayList<Roi>());
                        }

                        // index the roi to be added later to the RoiManager
                        roisBySlice.get(slice).add(roi);
                        slice++;
                    }
                }

            }
        }

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (final IOException e) {
            Log.error("Failed to close Bio-Formats reader.");
        }

        Log.info("Adding ROIs...");

        final List<Integer> keys = new ArrayList<Integer>(roisBySlice.keySet());
        Collections.sort(keys);

        for (final Integer slice : keys) {
            // HACK..
            // rm.add annoyingly puts a 0 at the end of each label. But if using
            // RoiManager.addRoi, only the first slice's rois are added (even if
            // updating the fusedImage's position).
            for (final Roi roi : roisBySlice.get(slice)) {
                rm.add((ImagePlus) null, roi, 0);
            }
        }

        Log.info("ROIs generated.");
    }

    /**
     * Initializes an {@link ImageReader} if the provided reader is null, or
     * does not match the given file id.
     * <p>
     * NB: All exceptions are handled in this method. If an exception is caught,
     * null will be returned.
     * </p>
     */
    protected IFormatReader initializeReader(IFormatReader in, final String file) {
        Log.info("Initializing Bio-Formats reader...");
        if (in == null || !file.equalsIgnoreCase(in.getCurrentFile())) {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.error("Failed to close Bio-Formats reader.");
                    return null;
                }
            }
            in = new ImageReader();
            final IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
            in.setMetadataStore(omeMeta);
            try {
                in.setId(file);
            } catch (final FormatException e) {
                Log.error("Failed to discover file names. FormatException when parsing: "
                        + file);
                return null;
            } catch (final IOException e) {
                Log.error("Failed to discover file names. IOException when parsing: "
                        + file);
                return null;
            }
        }

        return in;
    }

    protected ImagePlus[] openBF(final String multiSeriesFileName,
            final boolean splitC, final boolean splitT, final boolean splitZ,
            final boolean autoScale, final boolean crop, final boolean allSeries) {
        ImporterOptions options;
        ImagePlus[] imps = null;
        try {
            options = new ImporterOptions();
            options.setId(new File(multiSeriesFileName).getAbsolutePath());
            options.setSplitChannels(splitC);
            options.setSplitTimepoints(splitT);
            options.setSplitFocalPlanes(splitZ);
            options.setAutoscale(autoScale);
            options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
            options.setStackOrder(ImporterOptions.ORDER_XYCZT);
            options.setCrop(crop);

            options.setOpenAllSeries(allSeries);

            imps = BF.openImagePlus(options);
        } catch (final Exception e) {
            Log.error("Cannot open multiseries file: " + e, e);
            return null;
        }
        return imps;
    }

    protected ImagePlus[] openBFDefault(final String multiSeriesFileName) {
        return openBF(multiSeriesFileName, false, false, false, false, false,
                true);
    }

    protected ArrayList<ImageCollectionElement> getLayoutFromMultiSeriesFile(
            final String multiSeriesFile, final double increaseOverlap,
            final boolean ignoreCalibration, final boolean invertX,
            final boolean invertY, final boolean ignoreZStage,
            final Downsampler ds) {
        if (multiSeriesFile == null || multiSeriesFile.length() == 0) {
            Log.error("Filename is empty!");
            return null;
        }

        final ArrayList<ImageCollectionElement> elements =
                new ArrayList<ImageCollectionElement>();

        final IFormatReader r = new ChannelSeparator();

        final boolean timeHack;
        try {
            final ServiceFactory factory = new ServiceFactory();
            final OMEXMLService service =
                    factory.getInstance(OMEXMLService.class);
            final IMetadata meta = service.createOMEXMLMetadata();
            r.setMetadataStore(meta);

            r.setId(multiSeriesFile);

            final int numSeries = r.getSeriesCount();

            Log.debug("numSeries:  " + numSeries);

            // get maxZ
            int dim = 2;
            for (int series = 0; series < numSeries; ++series) {
                if (r.getSizeZ() > 1) {
                    dim = 3;
                }
            }

            Log.debug("dim:  " + dim);

            final MetadataRetrieve retrieve =
                    service.asRetrieve(r.getMetadataStore());
            Log.debug("retrieve:  " + retrieve);

            // CTR HACK: In the case of a single series, we treat each time
            // point
            // as a separate series for the purpose of stitching tiles.
            timeHack = numSeries == 1;

            for (int series = 0; series < numSeries; ++series) {
                Log.debug("fetching data for series:  " + series);
                r.setSeries(series);

                final int sizeT = r.getSizeT();
                Log.debug("sizeT:  " + sizeT);

                final int maxT = timeHack ? sizeT : 1;

                for (int t = 0; t < maxT; ++t) {
                    final double[] location =
                            CommonFunctions.getPlanePosition(r, retrieve,
                                    series, t, invertX, invertY, ignoreZStage);
                    double locationX = location[0];
                    double locationY = location[1];
                    double locationZ = location[2];

                    if (!ignoreCalibration) {
                        // calibration
                        double calX = 1, calY = 1, calZ = 1;
                        Length cal;
                        final String dimOrder =
                                r.getDimensionOrder().toUpperCase();

                        final int posX = dimOrder.indexOf('X');
                        cal = retrieve.getPixelsPhysicalSizeX(series);
                        if (posX >= 0 && cal != null
                                && cal.value().doubleValue() != 0) {
                            calX = cal.value().doubleValue();
                        }

                        Log.debug("calibrationX:  " + calX);

                        final int posY = dimOrder.indexOf('Y');
                        cal = retrieve.getPixelsPhysicalSizeY(series);
                        if (posY >= 0 && cal != null
                                && cal.value().doubleValue() != 0) {
                            calY = cal.value().doubleValue();
                        }

                        Log.debug("calibrationY:  " + calY);

                        final int posZ = dimOrder.indexOf('Z');
                        cal = retrieve.getPixelsPhysicalSizeZ(series);
                        if (posZ >= 0 && cal != null
                                && cal.value().doubleValue() != 0) {
                            calZ = cal.value().doubleValue();
                        }

                        Log.debug("calibrationZ:  " + calZ);

                        // location in pixel values;
                        locationX /= calX;
                        locationY /= calY;
                        locationZ /= calZ;
                    }

                    // increase overlap if desired
                    locationX *= (100.0 - increaseOverlap) / 100.0;
                    locationY *= (100.0 - increaseOverlap) / 100.0;
                    locationZ *= (100.0 - increaseOverlap) / 100.0;

                    // create ImageInformationList

                    final ImageCollectionElement element;

                    if (dim == 2) {
                        element =
                                new ImageCollectionElement(new File(
                                        multiSeriesFile), elements.size());
                        element.setModel(new TranslationModel2D());
                        element.setOffset(new float[] { (float) locationX,
                                (float) locationY });
                        element.setDimensionality(2);
                    } else {
                        element =
                                new ImageCollectionElement(new File(
                                        multiSeriesFile), elements.size());
                        element.setModel(new TranslationModel3D());
                        element.setOffset(new float[] { (float) locationX,
                                (float) locationY, (float) locationZ });
                        element.setDimensionality(3);
                    }

                    elements.add(element);
                }
            }
        } catch (final Exception ex) {
            Log.error(ex);
            return null;
        }

        // open all images
        ImporterOptions options;
        try {
            options = new ImporterOptions();
            options.setId(new File(multiSeriesFile).getAbsolutePath());
            options.setSplitChannels(false);
            options.setSplitTimepoints(timeHack);
            options.setSplitFocalPlanes(false);
            options.setAutoscale(false);
            options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
            options.setStackOrder(ImporterOptions.ORDER_XYCZT);
            options.setCrop(false);

            options.setOpenAllSeries(true);

            final ImagePlus[] imps = BF.openImagePlus(options);

            if (ds != null) {
                ds.getInput(imps[0].getWidth(), imps[0].getHeight());
                ds.run(imps);
                ds.run(elements);
            }

            if (imps.length != elements.size()) {
                Log.error("Inconsistent series layout. Metadata says "
                        + elements.size() + " tiles, but contains only "
                        + imps.length + " images/tiles.");

                for (final ImagePlus imp : imps) {
                    if (imp != null) {
                        imp.close();
                    }
                }

                return null;
            }

            for (int series = 0; series < elements.size(); ++series) {
                final ImageCollectionElement element = elements.get(series);
                element.setImagePlus(imps[series]); // assign the sub-series to
                // the elements list

                if (element.getDimensionality() == 2) {
                    Log.info("series " + series + ": position = ("
                            + element.getOffset(0) + "," + element.getOffset(1)
                            + ") [px], " + "size = (" + element.getDimension(0)
                            + "," + element.getDimension(1) + ")");
                } else {
                    Log.info("series " + series + ": position = ("
                            + element.getOffset(0) + "," + element.getOffset(1)
                            + "," + element.getOffset(2) + ") [px], "
                            + "size = (" + element.getDimension(0) + ","
                            + element.getDimension(1) + ","
                            + element.getDimension(2) + ")");
                }
            }

        } catch (final Exception e) {
            Log.error("Cannot open multiseries file: " + e, e);
            return null;
        }

        return elements;
    }

    protected ArrayList<ImageCollectionElement> getLayoutFromFile(
            final String directory, final String layoutFile,
            final Downsampler ds) {
        final ArrayList<ImageCollectionElement> elements =
                new ArrayList<ImageCollectionElement>();
        int dim = -1;
        int index = 0;
        boolean multiSeries = false;
        // A HashMap using the filename (including the full path) as the key is
        // used to access the individual tiles of a multiSeriesFile. This way
        // it's very easy to check if a file has already been opened. Note that
        // the map doesn't get used in the case of single series files below!
        // TODO: check performance on large datasets! Use an array for the
        // ImagePlus'es otherwise and store the index number in the hash map!
        final Map<String, ImagePlus[]> multiSeriesMap =
                new HashMap<String, ImagePlus[]>();
                String pfx = "Stitching_Grid.getLayoutFromFile: ";
                try {
                    final BufferedReader in =
                            TextFileAccess
                            .openFileRead(new File(directory, layoutFile));
                    if (in == null) {
                        Log.error(pfx + "Cannot find tileconfiguration file '"
                                + new File(directory, layoutFile).getAbsolutePath()
                                + "'");
                        return null;
                    }
                    int lineNo = 0;
                    pfx += "Line ";
                    while (in.ready()) {
                        final String line = in.readLine().trim();
                        lineNo++;
                        if (!line.startsWith("#") && line.length() > 3) {
                            if (line.startsWith("dim")) { // dimensionality parsing
                                final String entries[] = line.split("=");
                                if (entries.length != 2) {
                                    Log.error(pfx + lineNo
                                            + " does not look like [ dim = n ]: "
                                            + line);
                                    return null;
                                }

                                try {
                                    dim = Integer.parseInt(entries[1].trim());
                                } catch (final NumberFormatException e) {
                                    Log.error(pfx + lineNo
                                            + ": Cannot parse dimensionality: "
                                            + entries[1].trim());
                                    return null;
                                }

                            } else if (line.startsWith("multiseries")) {
                                final String entries[] = line.split("=");
                                if (entries.length != 2) {
                                    Log.error(pfx
                                            + lineNo
                                            + " does not look like [ multiseries = (true|false) ]: "
                                            + line);
                                    return null;
                                }

                                if (entries[1].trim().equals("true")) {
                                    multiSeries = true;
                                    Log.info(pfx + lineNo
                                            + ": parsing MultiSeries configuration.");
                                }

                            } else { // body parsing (tiles + coordinates)
                                if (dim < 0) {
                                    Log.error(pfx
                                            + lineNo
                                            + ": Header missing, should look like [dim = n], but first line is: "
                                            + line);
                                    return null;
                                }

                                if (dim < 2 || dim > 3) {
                                    Log.error(pfx
                                            + lineNo
                                            + ": only dimensions of 2 and 3 are supported: "
                                            + line);
                                    return null;
                                }

                                // read image tiles
                                final String entries[] = line.split(";");
                                if (entries.length != 3) {
                                    Log.error(pfx
                                            + lineNo
                                            + " does not have 3 entries! [fileName; seriesNr; (x,y,...)]");
                                    return null;
                                }

                                final String imageName = entries[0].trim();
                                if (imageName.length() == 0) {
                                    Log.error(pfx
                                            + lineNo
                                            + ": You have to give a filename [fileName; ; (x,y,...)]: "
                                            + line);
                                    return null;
                                }

                                int seriesNr = -1;
                                if (multiSeries) {
                                    final String imageSeries = entries[1].trim(); // sub-volume
                            // (series
                            // nr)
                                    if (imageSeries.length() == 0) {
                                        Log.info(pfx
                                                + lineNo
                                                + ": Series index required [fileName; series; (x,y,...)");
                                    } else {
                                        try {
                                            seriesNr = Integer.parseInt(imageSeries);
                                            Log.info(pfx + lineNo
                                                    + ": Series nr (sub-volume): "
                                                    + seriesNr);
                                        } catch (final NumberFormatException e) {
                                            Log.error(pfx + lineNo
                                                    + ": Cannot parse series nr: "
                                                    + imageSeries);
                                            return null;
                                        }
                                    }
                                }

                                String point = entries[2].trim(); // coordinates
                                if (!point.startsWith("(") || !point.endsWith(")")) {
                                    Log.error(pfx
                                            + lineNo
                                            + ": Wrong format of coordinates: (x,y,...): "
                                            + point);
                                    return null;
                                }

                                point = point.substring(1, point.length() - 1); // crop
                                // enclosing
                                // braces
                                final String points[] = point.split(",");
                                if (points.length != dim) {
                                    Log.error(pfx
                                            + lineNo
                                            + ": Wrong format of coordinates: (x,y,z,...), dim = "
                                            + dim + ": " + point);
                                    return null;
                                }
                                final float[] offset = new float[dim];
                                for (int i = 0; i < dim; i++) {
                                    try {
                                        offset[i] = Float.parseFloat(points[i].trim());
                                    } catch (final NumberFormatException e) {
                                        Log.error(pfx + lineNo
                                                + ": Cannot parse number: "
                                                + points[i].trim());
                                        return null;
                                    }
                                }

                                // now we can assemble the ImageCollectionElement:
                                final ImageCollectionElement element =
                                        new ImageCollectionElement(new File(directory,
                                                imageName), index++);
                                element.setDimensionality(dim);
                                if (dim == 3) {
                                    element.setModel(new TranslationModel3D());
                                } else {
                                    element.setModel(new TranslationModel2D());
                                }
                                element.setOffset(offset);

                                if (multiSeries) {
                                    final String imageNameFull =
                                            element.getFile().getAbsolutePath();
                                    if (multiSeriesMap.get(imageNameFull) == null) {
                                        Log.info(pfx + lineNo
                                                + ": Loading MultiSeries file: "
                                                + imageNameFull);
                                        multiSeriesMap.put(imageNameFull,
                                                openBFDefault(imageNameFull));
                                    }
                                    element.setImagePlus(multiSeriesMap
                                            .get(imageNameFull)[seriesNr]);
                                }

                                elements.add(element);
                            }
                        }
                    }
                } catch (final IOException e) {
                    Log.error("Stitching_Grid.getLayoutFromFile: " + e);
                    return null;
                }

                if (ds != null) {
                    final ImagePlus img = elements.get(0).open(true);
                    ds.getInput(img.getWidth(), img.getHeight());
                    ds.run(elements);
                }
                return elements;
    }

    protected ArrayList<ImageCollectionElement> getAllFilesInDirectory(
            final String directory, final boolean confirmFiles) {
        // get all files from the directory
        final File dir = new File(directory);
        if (!dir.isDirectory()) {
            Log.error("'" + directory + "' is not a directory. stop.");
            return null;
        }

        final String[] imageFiles = dir.list();
        final ArrayList<String> files = new ArrayList<String>();
        for (final String fileName : imageFiles) {
            final File file = new File(dir, fileName);

            if (file.isFile() && !file.isHidden() && !fileName.endsWith(".txt")
                    && !fileName.endsWith(".TXT")) {
                Log.info(file.getPath());
                files.add(fileName);
            }
        }

        Log.info("Found " + files.size()
                + " files (we ignore hidden and .txt files).");

        if (files.size() < 2) {
            Log.error("Only " + files.size() + " files found in '"
                    + dir.getPath() + "', you need at least 2 - stop.");
            return null;
        }

        final boolean[] useFile = new boolean[files.size()];
        for (int i = 0; i < files.size(); ++i) {
            useFile[i] = true;
        }

        if (confirmFiles) {
            final GenericDialogPlus gd = new GenericDialogPlus("Confirm files");

            for (final String name : files) {
                gd.addCheckbox(name, true);
            }

            gd.showDialog();

            if (gd.wasCanceled()) {
                return null;
            }

            for (int i = 0; i < files.size(); ++i) {
                useFile[i] = gd.getNextBoolean();
            }
        }

        final ArrayList<ImageCollectionElement> elements =
                new ArrayList<ImageCollectionElement>();

        for (int i = 0; i < files.size(); ++i) {
            if (useFile[i]) {
                elements.add(new ImageCollectionElement(new File(directory,
                        files.get(i)), i));
            }
        }

        if (elements.size() < 2) {
            Log.error("Only " + elements.size()
                    + " files selected, you need at least 2 - stop.");
            return null;
        }

        return elements;
    }

    protected ArrayList<ImageCollectionElement> getGridLayout(
            final GridType grid, final int gridSizeX, final int gridSizeY,
            final double overlapX, final double overlapY,
            final String directory, final String filenames, final int startI,
            final int startX, final int startY, final boolean virtual,
            final Downsampler ds) {
        final int gridType = grid.getType();
        final int gridOrder = grid.getOrder();

        // define the parsing of filenames
        // find how to parse
        String replaceX = "{", replaceY = "{", replaceI = "{";
        int numXValues = 0, numYValues = 0, numIValues = 0;

        if (grid.getType() < 4) {
            final int i1 = filenames.indexOf("{i");
            final int i2 = filenames.indexOf("i}");
            if (i1 >= 0 && i2 > 0) {
                numIValues = i2 - i1;
                for (int i = 0; i < numIValues; i++) {
                    replaceI += "i";
                }
                replaceI += "}";
            } else {
                replaceI = "\\\\\\\\";
            }
        } else {
            final int x1 = filenames.indexOf("{x");
            final int x2 = filenames.indexOf("x}");
            if (x1 >= 0 && x2 > 0) {
                numXValues = x2 - x1;
                for (int i = 0; i < numXValues; i++) {
                    replaceX += "x";
                }
                replaceX += "}";
            } else {
                replaceX = "\\\\\\\\";
            }

            final int y1 = filenames.indexOf("{y");
            final int y2 = filenames.indexOf("y}");
            if (y1 >= 0 && y2 > 0) {
                numYValues = y2 - y1;
                for (int i = 0; i < numYValues; i++) {
                    replaceY += "y";
                }
                replaceY += "}";
            } else {
                replaceY = "\\\\\\\\";
            }
        }

        // determine the layout
        final ImageCollectionElement[][] gridLayout =
                new ImageCollectionElement[gridSizeX][gridSizeY];

        // all snakes, row, columns, whatever
        if (grid.getType() < 4) {
            // the current position[x, y]
            final int[] position = new int[2];

            // we have gridSizeX * gridSizeY tiles
            for (int i = 0; i < gridSizeX * gridSizeY; ++i) {
                // get the vector where to move
                getPosition(position, i, gridType, gridOrder, gridSizeX,
                        gridSizeY);

                // get the filename
                final String file =
                        filenames.replace(replaceI,
                                getLeadingZeros(numIValues, i + startI));
                gridLayout[position[0]][position[1]] =
                        new ImageCollectionElement(new File(directory, file), i);
            }
        } else // fixed positions
        {
            // an index for the element
            int i = 0;

            for (int y = 0; y < gridSizeY; ++y) {
                for (int x = 0; x < gridSizeX; ++x) {
                    final String file =
                            filenames.replace(replaceX,
                                    getLeadingZeros(numXValues, x + startX))
                                    .replace(
                                            replaceY,
                                            getLeadingZeros(numYValues, y
                                                    + startY));
                    gridLayout[x][y] =
                            new ImageCollectionElement(
                                    new File(directory, file), i++);
                }
            }
        }

        // based on the minimum size we will compute the initial arrangement
        int minWidth = Integer.MAX_VALUE;
        int minHeight = Integer.MAX_VALUE;
        int minDepth = Integer.MAX_VALUE;

        boolean is2d = false;
        boolean is3d = false;

        // open all images and test them, collect information
        for (int y = 0; y < gridSizeY; ++y) {
            for (int x = 0; x < gridSizeX; ++x) {
                if (virtual) {
                    Log.info("Opening VIRTUAL (" + x + ", " + y + "): "
                            + gridLayout[x][y].getFile().getAbsolutePath()
                            + " ... ");
                } else {
                    Log.info("Loading (" + x + ", " + y + "): "
                            + gridLayout[x][y].getFile().getAbsolutePath()
                            + " ... ");
                }

                long time = System.currentTimeMillis();
                final ImagePlus imp = gridLayout[x][y].open(virtual);
                if (ds != null) {
                    if (!ds.hasInput()) {
                        ds.getInput(imp.getWidth(), imp.getHeight());
                    }
                    ds.run(imp);
                }
                time = System.currentTimeMillis() - time;

                if (imp == null) {
                    return null;
                }

                if (imp.getNSlices() > 1) {
                    Log.info("" + imp.getWidth() + "x" + imp.getHeight() + "x"
                            + imp.getNSlices() + "px, channels="
                            + imp.getNChannels() + ", timepoints="
                            + imp.getNFrames() + " (" + time + " ms)");
                    is3d = true;
                } else {
                    Log.info("" + imp.getWidth() + "x" + imp.getHeight()
                            + "px, channels=" + imp.getNChannels()
                            + ", timepoints=" + imp.getNFrames() + " (" + time
                            + " ms)");
                    is2d = true;
                }

                // test validity of images
                if (is2d && is3d) {
                    Log.info("Some images are 2d, some are 3d ... cannot proceed");
                    return null;
                }

                if (imp.getWidth() < minWidth) {
                    minWidth = imp.getWidth();
                }

                if (imp.getHeight() < minHeight) {
                    minHeight = imp.getHeight();
                }

                if (imp.getNSlices() < minDepth) {
                    minDepth = imp.getNSlices();
                }
            }
        }

        final int dimensionality;

        if (is3d) {
            dimensionality = 3;
        } else {
            dimensionality = 2;
        }

        // now get the approximate coordinates for each element
        // that is easiest done incrementally
        int xoffset = 0, yoffset = 0, zoffset = 0;

        // an ArrayList containing all the ImageCollectionElements
        final ArrayList<ImageCollectionElement> elements =
                new ArrayList<ImageCollectionElement>();

        for (int y = 0; y < gridSizeY; y++) {
            if (y == 0) {
                yoffset = 0;
            } else {
                yoffset += (int) (minHeight * (1 - overlapY));
            }

            for (int x = 0; x < gridSizeX; x++) {
                final ImageCollectionElement element = gridLayout[x][y];

                if (x == 0 && y == 0) {
                    xoffset = yoffset = zoffset = 0;
                }

                if (x == 0) {
                    xoffset = 0;
                } else {
                    xoffset += (int) (minWidth * (1 - overlapX));
                }

                element.setDimensionality(dimensionality);

                if (dimensionality == 3) {
                    element.setModel(new TranslationModel3D());
                    element.setOffset(new float[] { xoffset, yoffset, zoffset });
                } else {
                    element.setModel(new TranslationModel2D());
                    element.setOffset(new float[] { xoffset, yoffset });
                }

                elements.add(element);
            }
        }

        return elements;
    }

    // current snake directions ( if necessary )
    // they need a global state
    int snakeDirectionX = 0;
    int snakeDirectionY = 0;

    protected void writeTileConfiguration(final File file,
            final ArrayList<ImageCollectionElement> elements) {
        // write the initial tileconfiguration
        final PrintWriter out = TextFileAccess.openFileWrite(file);
        final int dimensionality = elements.get(0).getDimensionality();

        out.println("# Define the number of dimensions we are working on");
        out.println("dim = " + dimensionality);
        out.println("");
        out.println("# Define the image coordinates");

        for (final ImageCollectionElement element : elements) {
            if (dimensionality == 3) {
                out.println(element.getFile().getName() + "; ; ("
                        + element.getOffset(0) + ", " + element.getOffset(1)
                        + ", " + element.getOffset(2) + ")");
            } else {
                out.println(element.getFile().getName() + "; ; ("
                        + element.getOffset(0) + ", " + element.getOffset(1)
                        + ")");
            }
        }

        out.close();
    }

    protected void writeRegisteredTileConfiguration(final File file,
            final ArrayList<ImageCollectionElement> elements) {
        // write the tileconfiguration using the translation model
        final PrintWriter out = TextFileAccess.openFileWrite(file);
        final int dimensionality = elements.get(0).getDimensionality();

        Log.info("Writing registered TileConfiguration: " + file);

        out.println("# Define the number of dimensions we are working on");
        out.println("dim = " + dimensionality);
        out.println("");
        out.println("# Define the image coordinates");

        for (final ImageCollectionElement element : elements) {
            if (dimensionality == 3) {
                final TranslationModel3D m =
                        (TranslationModel3D) element.getModel();
                out.println(element.getFile().getName() + "; ; ("
                        + m.getTranslation()[0] + ", " + m.getTranslation()[1]
                                + ", " + m.getTranslation()[2] + ")");
            } else {
                final TranslationModel2D m =
                        (TranslationModel2D) element.getModel();
                final double[] tmp = new double[2];
                m.applyInPlace(tmp);

                out.println(element.getFile().getName() + "; ; (" + tmp[0]
                        + ", " + tmp[1] + ")");
            }
        }

        out.close();
    }

    protected void getPosition(final int[] currentPosition, final int i,
            final int gridType, final int gridOrder, final int sizeX,
            final int sizeY) {
        // gridType: "Row-by-row", "Column-by-column", "Snake by rows",
        // "Snake by columns", "Fixed position"
        // gridOrder:
        // choose2[ 0 ] = new String[]{ "Right & Down", "Left & Down",
        // "Right & Up", "Left & Up" };
        // choose2[ 1 ] = new String[]{ "Down & Right", "Down & Left",
        // "Up & Right", "Up & Left" };
        // choose2[ 2 ] = new String[]{ "Right & Down", "Left & Down",
        // "Right & Up", "Left & Up" };
        // choose2[ 3 ] = new String[]{ "Down & Right", "Down & Left",
        // "Up & Right", "Up & Left" };

        // init the position
        if (i == 0) {
            if (gridOrder == 0 || gridOrder == 2) {
                currentPosition[0] = 0;
            } else {
                currentPosition[0] = sizeX - 1;
            }

            if (gridOrder == 0 || gridOrder == 1) {
                currentPosition[1] = 0;
            } else {
                currentPosition[1] = sizeY - 1;
            }

            // it is a snake
            if (gridType == 2 || gridType == 3) {
                // starting with right
                if (gridOrder == 0 || gridOrder == 2) {
                    snakeDirectionX = 1;
                } else {
                    // starting with left
                    snakeDirectionX = -1;
                }

                // starting with down
                if (gridOrder == 0 || gridOrder == 1) {
                    snakeDirectionY = 1;
                } else {
                    // starting with up
                    snakeDirectionY = -1;
                }
            }
        } else // a move is required
        {
            // row-by-row, "Right & Down", "Left & Down", "Right & Up",
            // "Left & Up"
            if (gridType == 0) {
                // 0="Right & Down", 2="Right & Up"
                if (gridOrder == 0 || gridOrder == 2) {
                    if (currentPosition[0] < sizeX - 1) {
                        // just move one more right
                        ++currentPosition[0];
                    } else {
                        // we have to change rows
                        if (gridOrder == 0) {
                            ++currentPosition[1];
                        } else {
                            --currentPosition[1];
                        }

                        // row-by-row going right, so only set position to 0
                        currentPosition[0] = 0;
                    }
                } else // 1="Left & Down", 3="Left & Up"
                {
                    if (currentPosition[0] > 0) {
                        // just move one more left
                        --currentPosition[0];
                    } else {
                        // we have to change rows
                        if (gridOrder == 1) {
                            ++currentPosition[1];
                        } else {
                            --currentPosition[1];
                        }

                        // row-by-row going left, so only set position to 0
                        currentPosition[0] = sizeX - 1;
                    }
                }
            } else if (gridType == 1) // col-by-col, "Down & Right",
                // "Down & Left", "Up & Right",
                // "Up & Left"
            {
                // 0="Down & Right", 1="Down & Left"
                if (gridOrder == 0 || gridOrder == 1) {
                    if (currentPosition[1] < sizeY - 1) {
                        // just move one down
                        ++currentPosition[1];
                    } else {
                        // we have to change columns
                        if (gridOrder == 0) {
                            ++currentPosition[0];
                        } else {
                            --currentPosition[0];
                        }

                        // column-by-column going down, so position = 0
                        currentPosition[1] = 0;
                    }
                } else // 2="Up & Right", 3="Up & Left"
                {
                    if (currentPosition[1] > 0) {
                        // just move one up
                        --currentPosition[1];
                    } else {
                        // we have to change columns
                        if (gridOrder == 2) {
                            ++currentPosition[0];
                        } else {
                            --currentPosition[0];
                        }

                        // column-by-column going up, so position = sizeY - 1
                        currentPosition[1] = sizeY - 1;
                    }
                }
            } else if (gridType == 2) // "Snake by rows"
            {
                // currently going right
                if (snakeDirectionX > 0) {
                    if (currentPosition[0] < sizeX - 1) {
                        // just move one more right
                        ++currentPosition[0];
                    } else {
                        // just we have to change rows
                        currentPosition[1] += snakeDirectionY;

                        // and change the direction of the snake in x
                        snakeDirectionX *= -1;
                    }
                } else {
                    // currently going left
                    if (currentPosition[0] > 0) {
                        // just move one more left
                        --currentPosition[0];
                        return;
                    }
                    // just we have to change rows
                    currentPosition[1] += snakeDirectionY;

                    // and change the direction of the snake in x
                    snakeDirectionX *= -1;
                }
            } else if (gridType == 3) // "Snake by columns"
            {
                // currently going down
                if (snakeDirectionY > 0) {
                    if (currentPosition[1] < sizeY - 1) {
                        // just move one more down
                        ++currentPosition[1];
                    } else {
                        // we have to change columns
                        currentPosition[0] += snakeDirectionX;

                        // and change the direction of the snake in y
                        snakeDirectionY *= -1;
                    }
                } else {
                    // currently going up
                    if (currentPosition[1] > 0) {
                        // just move one more up
                        --currentPosition[1];
                    } else {
                        // we have to change columns
                        currentPosition[0] += snakeDirectionX;

                        // and change the direction of the snake in y
                        snakeDirectionY *= -1;
                    }
                }
            }
        }
    }

    public static String getLeadingZeros(final int zeros, final int number) {
        String output = "" + number;

        while (output.length() < zeros) {
            output = "0" + output;
        }

        return output;
    }
}
