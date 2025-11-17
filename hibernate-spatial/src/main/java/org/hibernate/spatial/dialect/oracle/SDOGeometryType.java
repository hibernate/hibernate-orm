/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;

/**
 * Descriptor for the Oracle Spatial SDO_GEOMETRY type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class SDOGeometryType implements JdbcType {

	private final OracleJDBCTypeFactory typeFactory;
	private final boolean useSTGeometry;

	/**
	 * Constructs a {@code SqlTypeDescriptor} for the Oracle SDOGeometry type.
	 *
	 * @param typeFactory the type factory to use.
	 * @param useSTGeometry
	 */
	public SDOGeometryType(OracleJDBCTypeFactory typeFactory, boolean useSTGeometry) {
		this.typeFactory = typeFactory;
		this.useSTGeometry = useSTGeometry;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.STRUCT;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOMETRY;
	}


	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new OracleJdbcLiteralFormatter<>( javaTypeDescriptor );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new SDOGeometryValueBinder( javaTypeDescriptor, this, typeFactory );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return (ValueExtractor<X>) new SDOGeometryValueExtractor( javaType, this );
	}

	/**
	 * Returns the Oracle type name for SDOGeometry.
	 *
	 * @return the Oracle type name
	 */
	public String getTypeName() {
		return "MDSYS.SDO_GEOMETRY";
	}

}
