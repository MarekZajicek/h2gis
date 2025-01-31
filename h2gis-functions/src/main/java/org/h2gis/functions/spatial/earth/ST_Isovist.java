/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <a href="http://www.h2database.com">http://www.h2database.com</a>. H2GIS is developed by CNRS
 * <a href="http://www.cnrs.fr/">http://www.cnrs.fr/</a>.
 * <p>
 * This code is part of the H2GIS project. H2GIS is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 * <p>
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * For more information, please consult: <a href="http://www.h2gis.org/">http://www.h2gis.org/</a>
 * or contact directly: info_at_h2gis.org
 */
package org.h2gis.functions.spatial.earth;

import org.h2gis.api.DeterministicScalarFunction;
import org.h2gis.utilities.jts_utils.VisibilityAlgorithm;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.util.GeometricShapeFactory;

import java.sql.SQLException;
import org.locationtech.jts.geom.Polygon;


/**
 * ST_Isovist
 * ST_Isovist takes LINESTRING(S) or POLYGON(S) as input and a maximum distance
 * (spatial ref units). This function compute the visibility polygon obstructed
 * by provided "walls". Provided segments will be enclosed by a circle defined by
 * maximum distance parameter.
 * @author Nicolas Fortin, Ifsttar UMRAE
 */
public class ST_Isovist extends DeterministicScalarFunction {
    public ST_Isovist() {
        addProperty(PROP_REMARKS, "ST_Isovist takes LINESTRING(S) or POLYGON(S) as input\n"
                + " and a maximum distance (spatial ref units). This function compute the visibility polygon" +
                " obstructed by provided \"walls\". Provided segments will be enclosed by a circle" +
                " defined by maximum distance parameter.");
    }

    @Override
    public String getJavaStaticMethod() {
        return "isovist";
    }

    /**
     * This function compute the visibility polygon
     * @param viewPoint Point instance, isovist location
     * @param lineSegments Occlusion segments. Geometry collection or polygon or linestring
     * @param maxDistance Maximum distance of view from viewPoint (spatial ref units)
     * @return The visibility polygon
     * @throws SQLException In case of wrong parameters
     */
    public static Geometry isovist(Geometry viewPoint, Geometry lineSegments, double maxDistance) throws SQLException {
        if (!(viewPoint instanceof Point) || viewPoint.isEmpty()) {
            throw new SQLException("First parameter of ST_Isovist must be a Point");
        }        
        if(viewPoint.getSRID()!=lineSegments.getSRID()){
            throw new SQLException("Operation on mixed SRID geometries not supported");
        }
        if (maxDistance <= 0) {
            throw new SQLException("Third parameter of ST_Isovist must be a valid distance superior than 0");
        }
        VisibilityAlgorithm visibilityAlgorithm = new VisibilityAlgorithm(maxDistance);
        visibilityAlgorithm.addGeometry(lineSegments);
        Polygon geomIsovist = visibilityAlgorithm.getIsoVist(viewPoint.getCoordinate(), true);
        geomIsovist.setSRID(viewPoint.getSRID());
        return geomIsovist;
    }

    /**
     * This function compute the visibility polygon
     * @param viewPoint Point instance, isovist location
     * @param lineSegments Occlusion segments. Geometry collection or polygon or linestring
     * @param maxDistance Maximum distance of view from viewPoint (spatial ref units)
     * @param radBegin Constraint view angle start in radian
     * @param radSize Constraint view angle size in radian
     * @return The visibility polygon
     * @throws SQLException In case of wrong parameters
     */
    public static Geometry isovist(Geometry viewPoint, Geometry lineSegments, double maxDistance, double radBegin, double radSize) throws SQLException {
        if(radSize <=0 ) {
            throw new SQLException("Angle size must be superior than 0 rad");
        }

        Geometry isopoly = isovist(viewPoint, lineSegments, maxDistance);

        // Intersects with view constrain
        GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
        geometricShapeFactory.setCentre(viewPoint.getCoordinate());
        geometricShapeFactory.setWidth(maxDistance * 2);
        geometricShapeFactory.setHeight(maxDistance * 2);
        return geometricShapeFactory.createArcPolygon(radBegin, radSize).intersection(isopoly);
    }
}
