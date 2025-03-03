/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.oracle.Encoders;
import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;
import org.geolatte.geom.codec.db.oracle.SDOGeometry;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/22/11
 */
class SDOGeometryValueBinder<J> implements ValueBinder<J> {

	private static final String SQL_TYPE_NAME = "MDSYS.SDO_GEOMETRY";

	private final OracleJDBCTypeFactory typeFactory;
	private final JavaType<J> javaType;

	public SDOGeometryValueBinder(
			JavaType<J> javaType,
			JdbcType jdbcType,
			OracleJDBCTypeFactory typeFactory) {
		this.javaType = javaType;
		this.typeFactory = typeFactory;
	}

	@Override
	public void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.STRUCT, SQL_TYPE_NAME );
		}
		else {
			final Geometry geometry = javaType.unwrap( value, Geometry.class, options );
			final Object dbGeom = toNative( geometry, st.getConnection() );
			st.setObject( index, dbGeom );
		}
	}

	@Override
	public void bind(
			CallableStatement st, J value, String name, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			st.setNull( name, Types.STRUCT, SQL_TYPE_NAME );
		}
		else {
			final Geometry geometry = javaType.unwrap( value, Geometry.class, options );
			final Object dbGeom = toNative( geometry, st.getConnection() );
			st.setObject( name, dbGeom );
		}
	}

	public Object store(SDOGeometry geom, Connection conn) throws SQLException {
		return typeFactory.createStruct( geom, conn );
	}

	private Object toNative(Geometry geom, Connection connection) {
		try {
			final SDOGeometry sdoGeom = Encoders.encode( geom );
			return store( sdoGeom, connection );
		}
		catch (SQLException e) {
			throw new HibernateException( "Problem during conversion from JTS to SDOGeometry", e );
		}
		catch (IllegalArgumentException e) {
			//we get here if the type of geometry is unsupported by geolatte encoders
			throw new HibernateException( e.getMessage() );
		}
		catch (Exception e) {
			throw new HibernateException( e );
		}
	}

}
