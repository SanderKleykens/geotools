/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2015, Open Source Geospatial Foundation (OSGeo)
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
 *
 */
package org.geotools.gce.grassraster;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.grassraster.format.GrassCoverageFormatFactory;
import org.geotools.util.URLs;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the grass raster reader abd writer.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassRasterReaderWriterTest {
    private double n = 5140020.0;
    private double s = 5139780.0;
    private double w = 1640650.0;
    private double e = 1640950.0;

    public static double[][] mapData = {
        {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500},
        {600, 650, 750, 850, 860, 900, 1000, 1200, 1250, 1500},
        {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500},
        {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500},
        {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500},
        {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500},
        {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500},
        {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}
    };

    @Test
    public void test() throws Exception {
        URL pitUrl = this.getClass().getClassLoader().getResource("testlocation/test/cell/pit");
        AbstractGridFormat format = new GrassCoverageFormatFactory().createFormat();
        File pitFile = URLs.urlToFile(pitUrl);
        Assert.assertTrue(format.accepts(pitFile));

        GridCoverage2D gc = readGc(format, pitFile);

        File parentFile = pitFile.getParentFile();
        File newPitFile = new File(parentFile, "newpit");
        // writing it down
        GridCoverageWriter writer = format.getWriter(newPitFile);
        writer.write(gc);

        // check it again through reading
        readGc(format, newPitFile);
    }

    @Test
    public void testFormatAccepts() throws Exception {
        URL pitUrl = this.getClass().getClassLoader().getResource("testlocation/test/cellhd/pit");
        AbstractGridFormat format = new GrassCoverageFormatFactory().createFormat();
        File pitFile = URLs.urlToFile(pitUrl);
        Assert.assertFalse(format.accepts(pitFile));
    }

    private GridCoverage2D readGc(AbstractGridFormat format, File fileToRead) throws IOException {
        GridCoverageReader reader = format.getReader(fileToRead);
        GridCoverage2D gc = (GridCoverage2D) reader.read();
        Assert.assertNotNull(gc);

        checkMatrixEqual(gc.getRenderedImage(), mapData, 0);

        Bounds envelope = gc.getEnvelope();
        Position lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        Position upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridGeometry2D gridGeometry = gc.getGridGeometry();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;

        Assert.assertEquals(w, westSouth[0], 0d);
        Assert.assertEquals(s, westSouth[1], 0d);
        Assert.assertEquals(e, eastNorth[0], 0d);
        Assert.assertEquals(n, eastNorth[1], 0d);
        Assert.assertEquals(8, height);
        Assert.assertEquals(10, width);
        return gc;
    }

    protected void checkMatrixEqual(RenderedImage image, double[][] matrix, double delta) {
        RectIter rectIter = RectIterFactory.create(image, null);
        int y = 0;
        do {
            int x = 0;
            do {
                double value = rectIter.getSampleDouble();
                double expectedResult = matrix[y][x];
                if (Double.isNaN(value)) {
                    Assert.assertTrue(x + " " + y, Double.isNaN(expectedResult));
                } else {
                    Assert.assertEquals(x + " " + y, expectedResult, value, delta);
                }
                x++;
            } while (!rectIter.nextPixelDone());
            rectIter.startPixels();
            y++;
        } while (!rectIter.nextLineDone());
    }
}
