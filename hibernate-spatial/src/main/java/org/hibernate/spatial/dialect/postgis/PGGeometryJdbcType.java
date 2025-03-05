/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis;

import org.hibernate.type.SqlTypes;

import org.geolatte.geom.codec.Wkb;

/**
 * Type Descriptor for the Postgis Geometry type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class PGGeometryJdbcType extends AbstractPostGISJdbcType {

	// Type descriptor instance using EWKB v2 (postgis versions >= 2.2.2, see: https://trac.osgeo.org/postgis/ticket/3181)
	public static final PGGeometryJdbcType INSTANCE_WKB_2 = new PGGeometryJdbcType( Wkb.Dialect.POSTGIS_EWKB_2 );

	private PGGeometryJdbcType(Wkb.Dialect dialect) {
		super( dialect );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOMETRY;
	}

	@Override
	protected String getConstructorFunction() {
		return "st_geomfromewkt";
	}

	@Override
	protected String getPGTypeName() {
		return "geometry";
	}

}
