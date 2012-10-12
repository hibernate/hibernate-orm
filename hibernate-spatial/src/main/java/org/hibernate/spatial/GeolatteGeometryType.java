package org.hibernate.spatial;

import org.geolatte.geom.*;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/12/12
 */
public class GeolatteGeometryType extends AbstractSingleColumnStandardBasicType<Geometry> {

	public static final GeolatteGeometryType INSTANCE = new GeolatteGeometryType();

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

	public GeolatteGeometryType() {
		super( GeometrySqlTypeDescriptor.INSTANCE, GeolatteGeometryJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "geolatte_geometry";
	}
}
