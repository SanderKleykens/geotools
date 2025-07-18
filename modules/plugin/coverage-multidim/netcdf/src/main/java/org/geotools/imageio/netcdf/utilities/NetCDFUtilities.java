/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2018, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.imageio.netcdf.utilities;

import it.geosolutions.imageio.stream.AccessibleStream;
import it.geosolutions.imageio.stream.input.URIImageInputStream;
import it.geosolutions.imageio.utilities.ImageIOUtilities;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.imageio.netcdf.AncillaryFileManager;
import org.geotools.imageio.netcdf.VariableAdapter;
import org.geotools.imageio.netcdf.cv.CoordinateHandlerFinder;
import org.geotools.imageio.netcdf.cv.CoordinateHandlerSpi;
import org.geotools.io.MemoryMappedFileCache;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.util.NumberRange;
import org.geotools.util.SoftValueHashMap;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionConfigBuilder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheIF;

/**
 * Set of NetCDF utility methods.
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class NetCDFUtilities {

    public static final boolean CHECK_COORDINATE_PLUGINS;

    public static final String CHECK_COORDINATE_PLUGINS_KEY = "netcdf.coordinates.enablePlugins";

    public static final String NETCDF4_MIMETYPE = "application/x-netcdf4";

    public static final String NETCDF3_MIMETYPE = "application/x-netcdf";

    public static final String NETCDF = "NetCDF";

    public static final String NETCDF4 = "NetCDF4";

    public static final String NETCDF_4C = "NetCDF-4C";

    public static final String NETCDF_3 = "NetCDF-3";

    public static final String STANDARD_PARALLEL_1 =
            MapProjection.AbstractProvider.STANDARD_PARALLEL_1.getName().getCode();

    public static final String STANDARD_PARALLEL_2 =
            MapProjection.AbstractProvider.STANDARD_PARALLEL_2.getName().getCode();

    public static final String CENTRAL_MERIDIAN =
            MapProjection.AbstractProvider.CENTRAL_MERIDIAN.getName().getCode();

    public static final String LATITUDE_OF_ORIGIN =
            MapProjection.AbstractProvider.LATITUDE_OF_ORIGIN.getName().getCode();

    public static final String SCALE_FACTOR =
            MapProjection.AbstractProvider.SCALE_FACTOR.getName().getCode();

    public static final String FALSE_EASTING =
            MapProjection.AbstractProvider.FALSE_EASTING.getName().getCode();

    public static final String FALSE_NORTHING =
            MapProjection.AbstractProvider.FALSE_NORTHING.getName().getCode();

    public static final String SEMI_MINOR =
            MapProjection.AbstractProvider.SEMI_MINOR.getName().getCode();

    public static final String SEMI_MAJOR =
            MapProjection.AbstractProvider.SEMI_MAJOR.getName().getCode();

    public static final String INVERSE_FLATTENING = "inverse_flattening";

    public static final String UNKNOWN = "unknown";

    public static final double DEFAULT_EARTH_RADIUS = 6371229.0d;
    /**
     * When true, the stack trace that created a reader that wasn't closed is recorded and then printed out when warning
     * the user about this.
     */
    public static final Boolean TRACE_ENABLED = "true".equalsIgnoreCase(System.getProperty("gt2.netcdf.trace"));

    public static final Set<NetcdfDataset.Enhance> DEFAULT_ENHANCE_MODE;

    private NetCDFUtilities() {}

    /** The LOGGER for this class. */
    private static final Logger LOGGER = Logger.getLogger(NetCDFUtilities.class.toString());

    /** Set containing all the definition of the UNSUPPORTED DIMENSIONS to set as vertical ones */
    private static final Set<String> UNSUPPORTED_DIMENSIONS;

    /** Set containing all the dimensions to be ignored */
    private static final Set<String> IGNORED_DIMENSIONS;

    /** Boolean indicating if GRIB library is available */
    private static boolean IS_GRIB_AVAILABLE;

    /** Boolean indicating if NC4 C Library is available */
    private static boolean IS_NC4_LIBRARY_AVAILABLE;

    public static final String EXTERNAL_DATA_DIR;

    public static final String NETCDF_DATA_DIR = "NETCDF_DATA_DIR";
    public static final String NETCDF_DATA_DIR_TREE = "NETCDF_DATA_DIR_TREE";
    public static final String NETCDF_ROOT = "NETCDF_ROOT";

    public static final String FILL_VALUE = "_FillValue";

    public static final String MISSING_VALUE = "missing_value";

    public static final String ACTUAL_RANGE = "actual_range";

    public static final String VALID_RANGE = "valid_range";

    public static final String VALID_MIN = "valid_min";

    public static final String VALID_MAX = "valid_max";

    public static final String LOWER_LEFT_LONGITUDE = "lower_left_longitude";

    public static final String LOWER_LEFT_LATITUDE = "lower_left_latitude";

    public static final String UPPER_RIGHT_LONGITUDE = "upper_right_longitude";

    public static final String UPPER_RIGHT_LATITUDE = "upper_right_latitude";

    public static final String COORDSYS = "latLonCoordSys";

    public static final String Y = "y";

    public static final String Y_COORD_PROJ = "y coordinate of projection";

    public static final String Y_PROJ_COORD = "projection_y_coordinate";

    public static final String X = "x";

    public static final String X_COORD_PROJ = "x coordinate of projection";

    public static final String X_PROJ_COORD = "projection_x_coordinate";

    public static final String LATITUDE = "latitude";

    public static final String LAT = "lat";

    public static final String LONGITUDE = "longitude";

    public static final String LON = "lon";

    public static final String GRID_LATITUDE = "grid_latitude";

    public static final String RLAT = "rlat";

    public static final String GRID_LONGITUDE = "grid_longitude";

    public static final String RLON = "rlon";

    public static final String DEPTH = "depth";

    public static final String ZETA = "z";

    public static final String BOUNDS = "bounds";

    private static final String BNDS = "bnds";

    public static final String HEIGHT = "height";

    public static final String TIME = "time";

    public static final String POSITIVE = "positive";

    public static final String UNITS = "units";

    public static final String NAME = "name";

    public static final String LONG_NAME = "long_name";

    public static final String ELEVATION_DIM =
            ImageMosaicFormat.ELEVATION.getName().toString();

    public static final String TIME_DIM = ImageMosaicFormat.TIME.getName().toString();

    public static final String STANDARD_NAME = "standard_name";

    public static final String DESCRIPTION = "description";

    public static final String M = "m";

    public static final String BOUNDS_SUFFIX = "_bnds";

    public static final String LON_UNITS = "degrees_east";

    public static final String LAT_UNITS = "degrees_north";

    public static final String RLATLON_UNITS = "degrees";

    public static final String NO_COORDS = "NoCoords";

    public static final String TIME_ORIGIN = "seconds since 1970-01-01 00:00:00 UTC";

    public static final long START_TIME;

    public static final String BOUNDARY_DIMENSION = "nv";

    public static final TimeZone UTC;

    public static final String GRID_MAPPING = "grid_mapping";

    public static final String GRID_MAPPING_NAME = "grid_mapping_name";

    public static final String COORDINATE_AXIS_TYPE = "_CoordinateAxisType";

    public static final String CONVENTIONS = "Conventions";

    public static final String COORD_SYS_BUILDER = "_CoordSysBuilder";

    public static final String COORD_SYS_BUILDER_CONVENTION = "ucar.nc2.dataset.conv.CF1Convention";

    public static final String COORDINATE_TRANSFORM_TYPE = "_CoordinateTransformType";

    public static final String COORDINATES = "coordinates";

    // They are recognized from GDAL
    public static final String SPATIAL_REF = "spatial_ref";

    public static final String GEO_TRANSFORM = "GeoTransform";

    // They are recognized from CERP NetCDF Metadata convention
    // https://www.jem.gov/downloads/CERP%20NetCDF%20standard/CERP_NetCDF_Metadata_Conventions_1.2.pdf
    public static final String CERP_ESRI_PE_STRING = "esri_pe_string";

    public static final String UNIQUE_TIME_ATTRIBUTE = "uniqueTimeAttribute";

    static final Set<String> EXCLUDED_ATTRIBUTES = new HashSet<>();

    public static final String ENHANCE_COORD_SYSTEMS = "org.geotools.coverage.io.netcdf.enhance.CoordSystems";

    public static final String ENHANCE_SCALE_MISSING = "org.geotools.coverage.io.netcdf.enhance.ScaleMissing";

    public static final String ENHANCE_CONVERT_ENUMS = "org.geotools.coverage.io.netcdf.enhance.ConvertEnums";

    public static boolean ENHANCE_SCALE_OFFSET = false;

    public static final String STORE_NAME = "StoreName";

    protected static final Map<URI, FileFormat> URI_FORMAT_CACHE = new SoftValueHashMap<>();

    protected static final Map<String, DatasetUrl> DATASET_URL_CACHE = new SoftValueHashMap<>();

    private static FileCacheIF rafFileCache;

    private static FileCacheIF netcdfFileCache;

    private static boolean USE_MEMORY_MAPPING;

    private static boolean USE_CACHE;

    /**
     * Number of bytes at the start of a file to search for a GRIB signature. Some GRIB files have WMO headers prepended
     * by a telecommunications gateway. NetCDF-Java Grib{1,2}RecordScanner look for the header in this many bytes.
     */
    private static final int GRIB_SEARCH_BYTES = 16000;

    public static boolean useMemoryMapping() {
        return USE_MEMORY_MAPPING;
    }

    public static boolean useCache() {
        return USE_CACHE;
    }

    /**
     * Global attribute for coordinate coverageDescriptorsCache.
     *
     * @author Simone Giannecchini, GeoSolutions S.A.S.
     */
    public enum Axis {
        X,
        Y,
        Z,
        T
    }

    public enum CheckType {
        NONE,
        UNSET,
        NOSCALARS,
        ONLYGEOGRIDS
    }

    public enum FileFormat {
        NONE,
        CDF,
        HDF5,
        GRIB,
        NCML,
        FC
    }

    /**
     * The dimension <strong>relative to the rank</strong> in {@link #variable} to use as image width. The actual
     * dimension is {@code variable.getRank() - X_DIMENSION}. Is hard-coded because the loop in the {@code read} method
     * expects this order.
     */
    public static final int X_DIMENSION = 1;

    /**
     * The dimension <strong>relative to the rank</strong> in {@link #variable} to use as image height. The actual
     * dimension is {@code variable.getRank() - Y_DIMENSION}. Is hard-coded because the loop in the {@code read} method
     * expects this order.
     */
    public static final int Y_DIMENSION = 2;

    /**
     * The default dimension <strong>relative to the rank</strong> in {@link #variable} to use as Z dimension. The
     * actual dimension is {@code variable.getRank() - Z_DIMENSION}.
     *
     * <p>
     */
    public static final int Z_DIMENSION = 3;

    /**
     * The data type to accept in images. Used for automatic detection of which coverageDescriptorsCache to assign to
     * images.
     */
    public static final Set<DataType> VALID_TYPES = new HashSet<>(12);

    public static final String NC4_ERROR_MESSAGE = "Native NetCDF C library is not available. "
            + "Unable to handle NetCDF4 files on input/output."
            + "\nPlease make sure to add the path of the Native NetCDF C libraries to the "
            + "PATH environment variable\n if you want to support NetCDF4-Classic files";

    static {
        // TODO remove this block when enhance mode can be set some other way, possibly via read
        // params

        // Default used to be to just enhance coord systems
        DEFAULT_ENHANCE_MODE = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);
        initEnhanceMode();

        // Initializing NetCDF Caches
        initMemoryMapping();
        initCache();

        String property = System.getProperty(CHECK_COORDINATE_PLUGINS_KEY);
        CHECK_COORDINATE_PLUGINS = Boolean.getBoolean(CHECK_COORDINATE_PLUGINS_KEY);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Value of Check Coordinate Plugins:" + property);
            LOGGER.info("Should check for coordinate handler plugins:" + CHECK_COORDINATE_PLUGINS);
        }

        IGNORED_DIMENSIONS = initIgnoreSet();

        // Setting the LINUX Epoch as start time
        final GregorianCalendar calendar = new GregorianCalendar(1970, 00, 01, 00, 00, 00);
        UTC = TimeZone.getTimeZone("UTC");
        calendar.setTimeZone(UTC);
        START_TIME = calendar.getTimeInMillis();

        EXCLUDED_ATTRIBUTES.add(UNITS);
        EXCLUDED_ATTRIBUTES.add(LONG_NAME);
        EXCLUDED_ATTRIBUTES.add(DESCRIPTION);
        EXCLUDED_ATTRIBUTES.add(STANDARD_NAME);

        UNSUPPORTED_DIMENSIONS = Set.of("OSEQD");

        VALID_TYPES.add(DataType.BOOLEAN);
        VALID_TYPES.add(DataType.BYTE);
        VALID_TYPES.add(DataType.SHORT);
        VALID_TYPES.add(DataType.INT);
        VALID_TYPES.add(DataType.LONG);
        VALID_TYPES.add(DataType.FLOAT);
        VALID_TYPES.add(DataType.DOUBLE);

        EXTERNAL_DATA_DIR = Optional.ofNullable(BaseDirectoryStrategy.getSystemPropertyDirectory(NETCDF_DATA_DIR))
                .map(f -> f.getPath())
                .orElse(null);

        try {
            Class.forName("ucar.nc2.grib.collection.GribIosp");
            Class.forName("org.geotools.coverage.io.grib.GribUtilities");
            IS_GRIB_AVAILABLE = true;
        } catch (ClassNotFoundException cnf) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("No Grib library found on classpath. GRIB will not be supported");
            }
            IS_GRIB_AVAILABLE = false;
        }

        IS_NC4_LIBRARY_AVAILABLE = NetcdfClibrary.isLibraryPresent();
        if (!IS_NC4_LIBRARY_AVAILABLE && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(NC4_ERROR_MESSAGE);
        }
        refreshParameterBehaviors();
    }

    private static void initEnhanceMode() {
        if (System.getProperty(ENHANCE_COORD_SYSTEMS) != null && !Boolean.getBoolean(ENHANCE_COORD_SYSTEMS)) {
            DEFAULT_ENHANCE_MODE.remove(NetcdfDataset.Enhance.CoordSystems);
        }

        if (Boolean.getBoolean(ENHANCE_SCALE_MISSING)) {
            // These sets are taken from the deprecated NetCDFDataset.parseEnhanceMode
            EnumSet<NetcdfDataset.Enhance> set = EnumSet.of(
                    NetcdfDataset.Enhance.ConvertUnsigned,
                    NetcdfDataset.Enhance.ApplyScaleOffset,
                    NetcdfDataset.Enhance.ConvertMissing);
            DEFAULT_ENHANCE_MODE.addAll(set);
            ENHANCE_SCALE_OFFSET = true;
        }

        if (Boolean.getBoolean(ENHANCE_CONVERT_ENUMS)) {
            DEFAULT_ENHANCE_MODE.add(NetcdfDataset.Enhance.ConvertEnums);
        }

        // ENHANCE_SCALE_MISSING_DEFER isn't exposed anymore since it will add a "none" enhancement,
        // so no need to parse it anymore

        NetcdfDataset.setDefaultEnhanceMode(DEFAULT_ENHANCE_MODE);
    }

    private static void initMemoryMapping() {
        String os = System.getProperty("os.name").toLowerCase();
        String initiator = "system default";
        if (os.contains("linux")) {
            USE_MEMORY_MAPPING = true;
        } else {
            USE_MEMORY_MAPPING = false;
        }
        String memoryMapping = System.getProperty("org.geotools.coverage.io.netcdf.memorymap");
        if (memoryMapping != null && !memoryMapping.isEmpty()) {
            USE_MEMORY_MAPPING = Boolean.parseBoolean(memoryMapping);
            initiator = "system property";
        }
        if (USE_MEMORY_MAPPING) {
            LOGGER.info("MemoryMapped RandomAccessFile has been enabled through " + initiator);
        }
    }

    private static void initCache() {
        USE_CACHE = Boolean.getBoolean("org.geotools.coverage.io.netcdf.cachefile");
        if (useCache()) {
            LOGGER.info("NetCDF File Cache has been enabled");
            int minElements = Integer.getInteger("org.geotools.coverage.io.netcdf.cache.min", 200);
            int maxElements = Integer.getInteger("org.geotools.coverage.io.netcdf.cache.max", 300);
            int period = Integer.getInteger("org.geotools.coverage.io.netcdf.cache.cleanup.period", 12 * 60);

            // No replacements found for 5.5 even if deprecated. Documentation still uses it
            // https://docs.unidata.ucar.edu/netcdf-java/5.5/userguide/disk_caching.html#netcdffilecache

            if (useMemoryMapping()) {
                rafFileCache = new MemoryMappedFileCache("rafCache", minElements, maxElements, -1, period);
            } else {
                rafFileCache = new FileCache("rafCache", minElements, maxElements, -1, period);
            }
            NetcdfDatasets.initNetcdfFileCache(minElements, maxElements, period);
            netcdfFileCache = NetcdfDatasets.getNetcdfFileCache();

            ucar.unidata.io.RandomAccessFile.setGlobalFileCache(rafFileCache);
        }
    }

    private static Set<String> initIgnoreSet() {
        Set<CoordinateHandlerSpi> handlers = CoordinateHandlerFinder.getAvailableHandlers();
        Iterator<CoordinateHandlerSpi> iterator = handlers.iterator();
        Set<String> ignoredSet = new HashSet<>();
        while (iterator.hasNext()) {
            CoordinateHandlerSpi handler = iterator.next();
            Set<String> ignored = handler.getIgnoreSet();
            if (ignored != null && !ignored.isEmpty()) {
                ignoredSet.addAll(ignored);
            }
        }
        if (!ignoredSet.isEmpty()) {
            return ignoredSet;
        }
        return new HashSet<>();
    }

    /** Get Z Dimension Lenght for standard CF variables */
    public static int getZDimensionLength(Variable var) {
        final int rank = var.getRank();
        if (rank > 2) {
            return var.getDimension(rank - Z_DIMENSION).getLength();
        }
        // TODO: Should I avoid use this method in case of 2D Variables?
        return 0;
    }

    public static int getDimensionLength(Variable var, final int dimensionIndex) {
        return var.getDimension(dimensionIndex).getLength();
    }

    /**
     * Returns the data type which most closely represents the "raw" internal data of the variable. This is the value
     * returned by the default implementation of {@link NetcdfImageReader#getRawDataType}.
     *
     * @param variable The variable.
     * @return The data type, or {@link DataBuffer#TYPE_UNDEFINED} if unknown.
     * @see NetcdfImageReader#getRawDataType
     */
    public static int getRawDataType(final VariableSimpleIF variable) {
        Variable ds = (Variable) variable;
        final DataType type = ds.getDataType();
        return transcodeNetCDFDataType(type, variable.getDataType().isUnsigned());
    }

    /**
     * Transcode a NetCDF data type into a java2D DataBuffer type.
     *
     * @param type the {@link DataType} to transcode.
     * @param unsigned if the original data is unsigned or not
     * @return an int representing the correct DataBuffer type.
     */
    public static int transcodeNetCDFDataType(final DataType type, final boolean unsigned) {
        if (DataType.BOOLEAN.equals(type) || DataType.BYTE.equals(type)) {
            return DataBuffer.TYPE_BYTE;
        }
        if (DataType.CHAR.equals(type)) {
            return DataBuffer.TYPE_USHORT;
        }
        if (DataType.SHORT.equals(type)) {
            return unsigned ? DataBuffer.TYPE_USHORT : DataBuffer.TYPE_SHORT;
        }
        if (DataType.INT.equals(type)) {
            return DataBuffer.TYPE_INT;
        }
        if (DataType.FLOAT.equals(type)) {
            return DataBuffer.TYPE_FLOAT;
        }
        if (DataType.LONG.equals(type) || DataType.DOUBLE.equals(type)) {
            return DataBuffer.TYPE_DOUBLE;
        }
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * NetCDF files may contains a wide set of coverageDescriptorsCache. Some of them are unuseful for our purposes. The
     * method returns {@code true} if the specified variable is accepted.
     */
    public static boolean isVariableAccepted(final Variable var, final CheckType checkType) {
        return isVariableAccepted(var, checkType, null);
    }

    /**
     * NetCDF files may contains a wide set of coverageDescriptorsCache. Some of them are unuseful for our purposes. The
     * method returns {@code true} if the specified variable is accepted.
     */
    @SuppressWarnings("deprecation") // no alternative for Dimension.getFullName
    public static boolean isVariableAccepted(
            final Variable var, final CheckType checkType, final NetcdfDataset dataset) {
        if (var instanceof CoordinateAxis1D) {
            return false;
        } else if (checkType == CheckType.NOSCALARS) {
            List<Dimension> dimensions = var.getDimensions();
            if (dimensions.size() < 2) {
                return false;
            }
            DataType dataType = var.getDataType();
            if (dataType == DataType.CHAR) {
                return false;
            }
            return isVariableAccepted(var.getFullName(), CheckType.NONE);
        } else if (checkType == CheckType.ONLYGEOGRIDS) {
            List<Dimension> dimensions = var.getDimensions();
            if (dimensions.size() < 2) {
                return false;
            }
            int twoDimensionalCoordinates = 0;
            for (Dimension dimension : dimensions) {
                String dimName = dimension.getFullName();
                // check the dimension to be defined
                Group group = dimension.getGroup();
                // Simple check if the group is not present. In that case false is returned.
                // This situation could happen with anonymous dimensions inside variables which
                // indicates the bounds of another variable. These kind of variable are not useful
                // for displaying the final raster.
                if (group == null) {
                    return false;
                }
                if (IGNORED_DIMENSIONS.contains(dimName)) {
                    continue;
                }
                Variable dimVariable = group.findVariable(dimName);
                if (dimVariable == null && dataset != null) {
                    // fallback on coordinates attribute for auxiliary coordinates.
                    dimVariable = getAuxiliaryCoordinate(dataset, group, var, dimName);
                }
                if (dimVariable instanceof CoordinateAxis1D) {
                    CoordinateAxis1D axis = (CoordinateAxis1D) dimVariable;
                    AxisType axisType = axis.getAxisType();
                    if (axisType == null) {
                        return false;
                    }
                    switch (axisType) {
                        case GeoX:
                        case GeoY:
                        case Lat:
                        case Lon:
                            twoDimensionalCoordinates++;
                            break;
                        default:
                            break;
                    }
                }
            }
            if (twoDimensionalCoordinates < 2) {
                // 2D Grid is missing
                return false;
            }

            DataType dataType = var.getDataType();
            if (dataType == DataType.CHAR) {
                return false;
            }
            return isVariableAccepted(var.getFullName(), CheckType.NONE);
        } else {
            return isVariableAccepted(var.getFullName(), checkType);
        }
    }

    @SuppressWarnings("deprecation") // no Alternative for Dimension.getFullName
    private static Variable getAuxiliaryCoordinate(NetcdfDataset dataset, Group group, Variable var, String dimName) {
        Variable coordinateVariable = null;
        Attribute attribute = var.findAttribute(NetCDFUtilities.COORDINATES);
        if (attribute != null) {
            String coordinates = attribute.getStringValue();
            String[] coords = coordinates.split(" ");
            for (String coord : coords) {
                Variable coordVar = group == null ? dataset.findVariable(coord) : group.findVariableLocal(coord);
                List<Dimension> varDimensions = coordVar.getDimensions();
                if (varDimensions != null
                        && varDimensions.size() == 1
                        && varDimensions.get(0).getFullName().equalsIgnoreCase(dimName)) {
                    coordinateVariable = coordVar;
                    break;
                }
            }
        }
        return coordinateVariable;
    }

    /**
     * NetCDF files may contain a wide set of coverageDescriptorsCache. Some of them are unuseful for our purposes. The
     * method returns {@code true} if the specified variable is accepted.
     */
    public static boolean isVariableAccepted(final String name, final CheckType checkType) {
        if (checkType == CheckType.NONE) {
            return true;
        } else if (name.equalsIgnoreCase(LATITUDE)
                || name.equalsIgnoreCase(LONGITUDE)
                || name.equalsIgnoreCase(LON)
                || name.equalsIgnoreCase(LAT)
                || name.equalsIgnoreCase(TIME)
                || name.equalsIgnoreCase(DEPTH)
                || name.equalsIgnoreCase(ZETA)
                || name.equalsIgnoreCase(HEIGHT)
                || name.toLowerCase().contains(COORDSYS.toLowerCase())
                || name.endsWith(BOUNDS)
                || name.endsWith(BNDS)
                || UNSUPPORTED_DIMENSIONS.contains(name)) {
            return false;
        } else {
            return true;
        }
    }

    public static FileFormat getFormat(URI uri) throws IOException {
        FileFormat format = URI_FORMAT_CACHE.get(uri);
        if (format != null) {
            return format;
        }
        format = getFormatFromUri(uri);
        URI_FORMAT_CACHE.put(uri, format);
        return format;
    }

    public static DatasetUrl getDatasetUrl(String uriString) throws IOException {
        DatasetUrl url = DATASET_URL_CACHE.get(uriString);
        if (url != null) {
            return url;
        }
        url = DatasetUrl.findDatasetUrl(uriString);
        DATASET_URL_CACHE.put(uriString, url);
        return url;
    }

    public static boolean isFile(String location) {
        String scheme = location.split(":")[0].toLowerCase();
        return "file".equalsIgnoreCase(scheme);
    }

    @SuppressWarnings("PMD.UseTryWithResources")
    private static FileFormat getFormatFromUri(URI uri) throws IOException {
        // try binary
        try (InputStream input = uri.toURL().openStream()) {
            // Checking Magic Number
            byte[] b = new byte[GRIB_SEARCH_BYTES];
            int count = input.read(b);
            if (count < 3) {
                return FileFormat.NONE;
            }
            // CDF signature at start of file
            if (b[0] == (byte) 0x43 && b[1] == (byte) 0x44 && b[2] == (byte) 0x46) {
                return FileFormat.CDF;
            }
            // HDF signature at start of file
            if (b[0] == (byte) 0x89 && b[1] == (byte) 0x48 && b[2] == (byte) 0x44) {
                return FileFormat.HDF5;
            }
            // Search for GRIB signature in first count bytes (up to GRIB_SEARCH_BYTES)
            for (int i = 0; i < count - 3; i++) {
                if (b[i] == (byte) 0x47
                        && b[i + 1] == (byte) 0x52
                        && b[i + 2] == (byte) 0x49
                        && b[i + 3] == (byte) 0x42) {
                    return FileFormat.GRIB;
                }
            }
        }

        // try XML
        try (InputStream input = uri.toURL().openStream()) {
            StreamSource streamSource = null;
            XMLStreamReader reader = null;
            try {
                streamSource = new StreamSource(input);
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                reader = inputFactory.createXMLStreamReader(streamSource);
                reader.nextTag();
                if ("netcdf".equals(reader.getName().getLocalPart())) {
                    return FileFormat.NCML;
                }
                if ("featureCollection".equals(reader.getName().getLocalPart())) {
                    return FileFormat.FC;
                }
            } catch (XMLStreamException | FactoryConfigurationError e) {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            } finally {
                if (reader != null) {
                    if (streamSource.getInputStream() != null) {
                        streamSource.getInputStream().close();
                    }
                    try {
                        reader.close();
                    } catch (XMLStreamException e) {
                    }
                }
            }
        }
        return FileFormat.NONE;
    }

    public static NetcdfDataset acquireFeatureCollection(String path) throws IOException {
        @SuppressWarnings("PMD.CloseResource") // won't risk closing System.err
        Formatter formatter = new Formatter(System.err);
        FeatureCollectionConfigBuilder builder = new FeatureCollectionConfigBuilder(formatter);
        // this is the path to the feature collection XML
        FeatureCollectionConfig config = builder.readConfigFromFile(path);
        @SuppressWarnings("PMD.CloseResource") // It will be closed outside
        Fmrc fmrc = Fmrc.open(config, formatter);
        NetcdfDataset dataset = NetcdfDataset.builder().setLocation(path).build();
        fmrc.getDataset2D(dataset);
        return dataset;
    }

    public static NetcdfDataset acquireDataset(URI uri) throws IOException {
        String uriString = uri.toString();
        if (getFormat(uri) == FileFormat.FC) {
            return acquireFeatureCollection(uriString);
        } else {
            DatasetUrl url = getDatasetUrl(uriString);
            return NetcdfDatasets.acquireDataset(url, null);
        }
    }

    /**
     * Returns a {@code NetcdfDataset} given an input object
     *
     *                the input object (usually a {@code File}, a
     *                {@code String} or a {@code FileImageInputStreamExt).
     * @return {@code NetcdfDataset} in case of success.
     *                 if some error occur while opening the dataset.
     * @throws {@link IllegalArgumentException}
     *                 in case the specified input is a directory
     */
    public static NetcdfDataset getDataset(Object input) throws IOException {
        NetcdfDataset dataset = null;
        if (input instanceof URI) {
            dataset = acquireDataset((URI) input);
        } else if (input instanceof File) {
            final File file = (File) input;
            if (!file.isDirectory()) {
                dataset = acquireDataset(file.toURI());
            } else {
                throw new IllegalArgumentException(
                        "Error occurred during NetCDF file reading: The input file is a Directory.");
            }
        } else if (input instanceof String) {
            File file = new File((String) input);
            if (!file.isDirectory()) {
                dataset = acquireDataset(file.toURI());
            } else {
                throw new IllegalArgumentException(
                        "Error occurred during NetCDF file reading: The input file is a Directory.");
            }
        } else if (input instanceof URL) {
            final URL tempURL = (URL) input;
            String protocol = tempURL.getProtocol();
            if (protocol.equalsIgnoreCase("file")) {
                File file = ImageIOUtilities.urlToFile(tempURL);
                if (!file.isDirectory()) {
                    dataset = acquireDataset(file.toURI());
                } else {
                    throw new IllegalArgumentException(
                            "Error occurred during NetCDF file reading: The input file is a Directory.");
                }
            } else if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("dods")) {
                try {
                    dataset = acquireDataset(tempURL.toURI());
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
        } else if (input instanceof AccessibleStream) {
            final AccessibleStream<?> stream = (AccessibleStream<?>) input;
            if (stream.getBinding().isAssignableFrom(File.class)) {
                @SuppressWarnings("unchecked")
                AccessibleStream<File> as = (AccessibleStream<File>) input;
                final File file = as.getTarget();
                if (!file.isDirectory()) {
                    dataset = acquireDataset(file.toURI());
                } else {
                    throw new IllegalArgumentException(
                            "Error occurred during NetCDF file reading: The input file is a Directory.");
                }
            } else if (stream.getBinding().isAssignableFrom(URI.class)) {
                @SuppressWarnings("unchecked")
                AccessibleStream<URI> as = (AccessibleStream<URI>) input;
                final URI uri = as.getTarget();
                dataset = acquireDataset(uri);
            }
        }
        return dataset;
    }

    /**
     * Checks if the input is file based, and if yes, returns the file.
     *
     * @param input the input to check.
     * @return the file or <code>null</code> if it is not file based.
     */
    public static File getFile(Object input) throws IOException {
        File guessedFile = null;
        if (input instanceof File) {
            guessedFile = (File) input;
        } else if (input instanceof String) {
            guessedFile = new File((String) input);
        } else if (input instanceof URL) {
            final URL tempURL = (URL) input;
            String protocol = tempURL.getProtocol();
            if (protocol.equalsIgnoreCase("file")) {
                guessedFile = ImageIOUtilities.urlToFile(tempURL);
            }
        } else if (input instanceof URIImageInputStream) {
            final URIImageInputStream uriInStream = (URIImageInputStream) input;
            String uri = uriInStream.getUri().toString();
            guessedFile = new File(uri);
        } else if (input instanceof AccessibleStream) {
            final AccessibleStream<?> stream = (AccessibleStream<?>) input;
            if (stream.getBinding().isAssignableFrom(File.class)) {
                @SuppressWarnings("unchecked")
                AccessibleStream<File> as = (AccessibleStream<File>) input;
                guessedFile = as.getTarget();
            }
        }
        // check
        if (guessedFile != null && guessedFile.exists() && !guessedFile.isDirectory()) {
            return guessedFile;
        }
        return null;
    }

    /**
     * Returns a format to use for parsing values along the specified axis type. This method is invoked when parsing the
     * date part of axis units like "<cite>days since 1990-01-01 00:00:00</cite>". Subclasses should override this
     * method if the date part is formatted in a different way. The default implementation returns the following
     * formats:
     *
     * <p>
     *
     * <ul>
     *   <li>For {@linkplain AxisType#Time time axis}, a {@link DateFormat} using the {@code "yyyy-MM-dd HH:mm:ss"}
     *       pattern in UTC {@linkplain TimeZone timezone}.
     *   <li>For all other kind of axis, a {@link NumberFormat}.
     * </ul>
     *
     * <p>The {@linkplain Locale#ENGLISH English locale} is used by default for most formats because it is relatively
     * close to ISO (for example regarding days and months order in dates) while using the English symbols.
     *
     * @param type The type of the axis.
     * @param prototype An example of the values to be parsed. Implementations may parse this prototype when the axis
     *     type alone is not sufficient. For example the {@linkplain AxisType#Time time axis type} should uses the
     *     {@code "yyyy-MM-dd"} date pattern, but some files do not follow this convention and use the default local
     *     instead.
     * @return The format for parsing values along the axis.
     */
    public static Format getAxisFormat(final AxisType type, final String prototype) {
        if (!type.equals(AxisType.Time) && !type.equals(AxisType.RunTime)) {
            return NumberFormat.getNumberInstance(Locale.ENGLISH);
        }
        char dateSeparator = '-'; // The separator used in ISO format.
        boolean twoDigitYear = false; // Year is two digits
        boolean yearLast = false; // Year is first in ISO pattern.
        boolean namedMonth = false; // Months are numbers in the ISO pattern.
        boolean monthFirst = false; // Month first (assumes yearLast AND namedMonth true as well)
        boolean addT = false;
        boolean appendZ = false;
        int dateLength = 0;
        if (prototype != null) {
            /*
             * Performs a quick check on the prototype content. If the prototype
             * seems to use a different date separator than the ISO one, we will
             * adjust the pattern accordingly. Also checks if the year seems to
             * appears last rather than first, and if the month seems to be
             * written using letters rather than digits.
             */
            int field = 1;
            int digitCount = 0;

            final int length = prototype.length();
            for (int i = 0; i < length; i++) {
                final char c = prototype.charAt(i);
                if (Character.isWhitespace(c)) {
                    if (monthFirst && field == 1) {
                        dateLength++; // move to next field
                    } else {
                        break; // Checks only the dates, ignore the hours.
                    }
                } else if (Character.isDigit(c)) {
                    digitCount++;
                    dateLength++;
                    continue; // Digits are legal in all cases.
                } else if (Character.isLetter(c) && field <= 2) {
                    if (field == 1) {
                        yearLast = true;
                        monthFirst = true;
                    }
                    namedMonth = true;
                    dateLength++;
                    continue; // Letters are legal for month only.
                } else if (field == 1 || monthFirst && field == 2) {
                    dateSeparator = c;
                    dateLength++;
                } else if (c == dateSeparator) {
                    dateLength++;
                } else if (c == 'T') {
                    addT = true;
                } else if (c == 'Z' && i == length - 1) {
                    appendZ = true;
                }

                if ((field == 1 || yearLast && field == 3) && digitCount <= 2) {
                    twoDigitYear = true;
                }

                digitCount = 0;
                field++;
            }
            if (digitCount >= 4) {
                yearLast = true;
                twoDigitYear = false;
            }
        }
        String pattern = null;
        if (yearLast) {
            pattern = (monthFirst ? "MMM dd-" : "dd-" + (namedMonth ? "MMM-" : "MM-")) + (twoDigitYear ? "yy" : "yyyy");
        } else {
            pattern = (twoDigitYear ? "yy-" : "yyyy-") + (namedMonth ? "MMM-" : "MM-") + "dd";
            if (dateLength < pattern.length()) {
                // case of truncated date
                pattern = pattern.substring(0, dateLength);
            }
        }
        pattern = pattern.replace('-', dateSeparator);
        int lastColon = prototype != null ? prototype.lastIndexOf(":") : -1; // $NON-NLS-1$
        if (lastColon != -1) {
            pattern += addT ? "'T'" : " ";
            pattern += prototype != null && lastColon >= 16 ? "HH:mm:ss" : "HH:mm";
        }
        // TODO: Improve me:
        // Handle timeZone
        pattern += appendZ ? "'Z'" : "";
        final DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    /**
     * Depending on the type of model/netcdf file, we will check for the presence of some coverageDescriptorsCache
     * rather than some others. The method returns the type of check on which we need to leverage to restrict the set of
     * interesting coverageDescriptorsCache. The method will check for some KEY/FLAGS/ATTRIBUTES within the input
     * dataset in order to define the proper check type to be performed.
     *
     * @param dataset the input dataset.
     * @return the proper {@link CheckType} to be performed on the specified dataset.
     */
    public static CheckType getCheckType(NetcdfDataset dataset) {
        CheckType ct = CheckType.UNSET;
        if (dataset != null) {
            ct = CheckType.ONLYGEOGRIDS;
        }
        return ct;
    }

    /** */
    public static SimpleFeatureType createFeatureType(
            String schemaName, String schemaDef, CoordinateReferenceSystem crs) {
        SimpleFeatureType indexSchema = null;
        if (schemaDef == null) {
            throw new IllegalArgumentException("Unable to create feature type from null definition!");
        }
        schemaDef = schemaDef.trim();
        // get the schema
        try {
            indexSchema = DataUtilities.createType(schemaName, schemaDef);
            indexSchema = DataUtilities.createSubType(indexSchema, DataUtilities.attributeNames(indexSchema), crs);
        } catch (Throwable e) {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
            indexSchema = null;
        }
        return indexSchema;
    }

    /** @return true if the GRIB library is available */
    public static boolean isGribAvailable() {
        return IS_GRIB_AVAILABLE;
    }

    /** @return true if the C Native NetCDF 4 library is available */
    public static boolean isNC4CAvailable() {
        return IS_NC4_LIBRARY_AVAILABLE;
    }

    public static boolean isCheckCoordinatePlugins() {
        return CHECK_COORDINATE_PLUGINS;
    }

    /** @return An unmodifiable Set of Unsupported Dimension names */
    public static Set<String> getUnsupportedDimensions() {
        return UNSUPPORTED_DIMENSIONS;
    }

    /** @return an unmodifiable Set of the Dimensions to be ignored by the Coordinate parsing machinery */
    public static Set<String> getIgnoredDimensions() {
        return Collections.unmodifiableSet(IGNORED_DIMENSIONS);
    }

    /** Adds a dimension to the ignored dimensions set. */
    public static void addIgnoredDimension(String dimensionName) {
        IGNORED_DIMENSIONS.add(dimensionName);
    }

    /**
     * Utility method for getting NoData from an input {@link Variable}
     *
     * @param var Variable instance
     * @return a Number representing NoData
     */
    @SuppressWarnings("deprecation") // no alternative for Attribute.getFullName
    public static Number getNodata(Variable var) {
        if (var != null) {
            // Getting all the Variable attributes
            Iterator<Attribute> attributes = var.iterator();
            String fullName;
            // Searching for FILL_VALUE or MISSING_VALUE attributes
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                fullName = attribute.getFullName();
                if (fullName.equalsIgnoreCase(FILL_VALUE) || fullName.equalsIgnoreCase(MISSING_VALUE)) {
                    return attribute.getNumericValue();
                }
            }
        }
        return null;
    }

    /**
     * Utility method for getting Range from an input {@link Variable}
     *
     * @param var Variable instance
     * @return a Range representing actual_range or valid_min/valid_max range or valid_range
     */
    public static NumberRange getRange(Variable var) {
        if (var != null) {
            double min = Double.NaN;
            double max = Double.NaN;
            DataType dataType = null;
            Attribute rangeAttribute = var.findAttribute(ACTUAL_RANGE);
            if (rangeAttribute == null) {
                rangeAttribute = var.findAttribute(VALID_RANGE);
            }
            if (rangeAttribute != null) {
                dataType = rangeAttribute.getDataType();
                min = rangeAttribute.getNumericValue(0).doubleValue();
                max = rangeAttribute.getNumericValue(1).doubleValue();
            } else {
                Attribute minAttribute = var.findAttribute(VALID_MIN);
                Attribute maxAttribute = var.findAttribute(VALID_MAX);
                if (minAttribute != null && maxAttribute != null) {
                    dataType = minAttribute.getDataType();
                    min = minAttribute.getNumericValue().doubleValue();
                    max = maxAttribute.getNumericValue().doubleValue();
                }
            }
            if (!Double.isNaN(min) && !Double.isNaN(max)) {
                // Quoting from:
                // http://www.unidata.ucar.edu/software/netcdf/docs/attribute_conventions.html
                // For integer types, there should be a difference of 1 between the _FillValue and
                // this valid minimum or maximum.
                // Force this constraints to also avoid overlapping categories
                if (dataType == DataType.BYTE
                        || dataType == DataType.INT
                        || dataType == DataType.SHORT
                        || dataType == DataType.LONG) {
                    Number noData = getNodata(var);
                    if (noData != null && noData.intValue() == min) {
                        min++;
                    }
                }
                return NumberRange.create(min, max);
            }
        }

        return null;
    }

    /** Return the propery NetCDF dataType for the input datatype class */
    public static DataType getNetCDFDataType(String classDataType) {
        if (isATime(classDataType)) {
            return DataType.DOUBLE;
        } else if (classDataType.endsWith("Integer")) {
            return DataType.INT;
        } else if (classDataType.endsWith("Double")) {
            return DataType.DOUBLE;
        } else if (classDataType.endsWith("String")) {
            return DataType.STRING;
        }
        return DataType.STRING;
    }

    /**
     * Transcode a DataBuffer type into a NetCDF DataType .
     *
     * @param dataType the beam {@link ProductData} type to transcode.
     * @return an NetCDF DataType type.
     */
    public static DataType transcodeImageDataType(final int dataType) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                return DataType.BYTE;
            case DataBuffer.TYPE_SHORT:
                return DataType.SHORT;
            case DataBuffer.TYPE_INT:
                return DataType.INT;
            case DataBuffer.TYPE_DOUBLE:
                return DataType.DOUBLE;
            case DataBuffer.TYPE_FLOAT:
                return DataType.FLOAT;
            case DataBuffer.TYPE_UNDEFINED:
            default:
                throw new IllegalArgumentException("Invalid input data type:" + dataType);
        }
    }

    /** Return true in case that dataType refers to something which need to be handled as a Time (TimeStamp, Date) */
    public static final boolean isATime(String classDataType) {
        return classDataType.endsWith("Timestamp") || classDataType.endsWith("Date");
    }

    /**
     * Get an Array of proper size and type.
     *
     * @param dimensions the dimensions
     * @param varDataType the DataType of the required array
     */
    public static Array getArray(int[] dimensions, DataType varDataType) {
        if (dimensions == null) throw new IllegalArgumentException("Illegal dimensions");
        final int nDims = dimensions.length;
        if (nDims < 1 || nDims > 7) {
            throw new IllegalArgumentException(
                    "Unable to create a proper array unsupported Datatype; nDims not between 1 and 7");
        }

        return Array.factory(varDataType, dimensions);
    }

    /**
     * Transcode a NetCDF Number into a proper Number instance.
     *
     * @param type the {@link DataType} to transcode.
     * @return the proper number instance
     */
    public static Number transcodeNumber(final DataType type, Number value) {
        if (DataType.DOUBLE.equals(type)) {
            return value.doubleValue();
        } else if (DataType.FLOAT.equals(type)) {
            return value.floatValue();
        } else if (DataType.LONG.equals(type)) {
            return value.longValue();
        } else if (DataType.INT.equals(type)) {
            return value.intValue();
        } else if (DataType.SHORT.equals(type)) {
            return value.shortValue();
        } else if (DataType.BYTE.equals(type)) {
            return value.byteValue();
        }
        throw new IllegalArgumentException(
                "Unsupported type or value: type = " + type.toString() + " value = " + value);
    }

    /**
     * Default parameter behavior properties TODO: better way of handling configuration settings, such as read
     * parameters.
     */
    public static final String PARAMS_MAX_KEY = "org.geotools.coverage.io.netcdf.param.max";

    public static final String PARAMS_MIN_KEY = "org.geotools.coverage.io.netcdf.param.min";

    private static Set<String> PARAMS_MAX;

    private static Set<String> PARAMS_MIN;

    public static void refreshParameterBehaviors() {
        PARAMS_MAX = new HashSet<>();
        String maxProperty = System.getProperty(PARAMS_MAX_KEY);
        if (maxProperty != null) {
            for (String param : maxProperty.split(",")) {
                PARAMS_MAX.add(param.trim().toUpperCase());
            }
        }

        String minProperty = System.getProperty(PARAMS_MIN_KEY);
        PARAMS_MIN = new HashSet<>();
        if (minProperty != null) {
            for (String param : minProperty.split(",")) {
                PARAMS_MIN.add(param.trim().toUpperCase());
            }
        }
    }

    public enum ParameterBehaviour {
        DO_NOTHING,
        MAX,
        MIN
    }

    public static ParameterBehaviour getParameterBehaviour(String parameter) {
        if (PARAMS_MAX.contains(parameter.toUpperCase())) {
            return ParameterBehaviour.MAX;
        } else if (PARAMS_MIN.contains(parameter.toUpperCase())) {
            return ParameterBehaviour.MIN;
        } else {
            return ParameterBehaviour.DO_NOTHING;
        }
    }

    private static void clearCache() {
        URI_FORMAT_CACHE.clear();
        DATASET_URL_CACHE.clear();
    }

    public static void disableNetCDFFileCaches() {
        if (rafFileCache != null) {
            rafFileCache.disable();
        }
        if (netcdfFileCache != null) {
            netcdfFileCache.disable();
        }
    }

    public static void enableNetCDFFileCaches() {
        if (rafFileCache != null) {
            rafFileCache.enable();
        }
        if (netcdfFileCache != null) {
            netcdfFileCache.enable();
        }
    }

    /**
     * Clear all the internal caches. Call this method if the datastore config has been updated or the NetCDF XML file
     * has been modified.
     */
    public static void clearCaches() {
        clearCache();
        AncillaryFileManager.clearCache();
        VariableAdapter.clearCache();
        if (useCache()) {
            if (netcdfFileCache != null) {
                netcdfFileCache.clearCache(true);
            }
            if (rafFileCache != null) {
                rafFileCache.clearCache(true);
            }
        }
    }

    public static FileCacheIF getRafFileCache() {
        return rafFileCache;
    }

    public static FileCacheIF getNetCDFFileCache() {
        return netcdfFileCache;
    }
}
