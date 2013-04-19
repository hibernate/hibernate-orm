package org.hibernate.spatial;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryCollection;
import org.geolatte.geom.LineString;
import org.geolatte.geom.MultiLineString;
import org.geolatte.geom.MultiPoint;
import org.geolatte.geom.MultiPolygon;
import org.geolatte.geom.Point;
import org.geolatte.geom.Polygon;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/12/12
 */
public class GeolatteGeometryType extends AbstractSingleColumnStandardBasicType<Geometry> implements Spatial {

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				Geometry.class.getCanonicalName(),
				Point.class.getCanonicalName(),
				Polygon.class.getCanonicalName(),
				MultiPolygon.class.getCanonicalName(),
				LineString.class.getCanonicalName(),
				MultiLineString.class.getCanonicalName(),
				MultiPoint.class.getCanonicalName(),
				GeometryCollection.class.getCanonicalName(),
				"geolatte_geometry"
		};
	}

	public GeolatteGeometryType(SqlTypeDescriptor sqlTypeDescriptor) {
		super( sqlTypeDescriptor, GeolatteGeometryJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "geolatte_geometry";
	}
}
