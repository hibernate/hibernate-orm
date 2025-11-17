/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Decoders;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;


//TODO -- requires cleanup and must be made package local

/**
 * ValueExtractor for SDO_GEOMETRY
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/22/11
 */
public class SDOGeometryValueExtractor<X> extends BasicExtractor<X> {

	/**
	 * Creates instance
	 *
	 * @param javaType the {@code JavaType} to use
	 * @param jdbcType the {@code JdbcType} to use
	 */
	public SDOGeometryValueExtractor(JavaType<X> javaType, JdbcType jdbcType) {
		super( javaType, jdbcType );
	}

	@Override
	protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		final Object geomObj = rs.getObject( paramIndex );
		return getJavaType().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( index );
		return getJavaType().wrap( convert( geomObj ), options );
	}

	@Override
	protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		final Object geomObj = statement.getObject( name );
		return getJavaType().wrap( convert( geomObj ), options );
	}

	/**
	 * Converts an oracle to a JTS Geometry
	 *
	 * @param struct The Oracle STRUCT representation of an SDO_GEOMETRY
	 *
	 * @return The JTS Geometry value
	 */
	public Geometry convert(Object struct) {
		if ( struct == null ) {
			return null;
		}
		final SDOGeometry sdogeom = SDOGeometry.load( (Struct) struct );
		return toGeometry( sdogeom );
	}

	private Geometry toGeometry(SDOGeometry sdoGeom) {
		return Decoders.decode( sdoGeom );
	}

}
