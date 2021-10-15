/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.geolatte.geom.codec.db.oracle.OracleJDBCTypeFactory;

/**
 * Descriptor for the Oracle Spatial SDO_GEOMETRY type
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class SDOGeometryType implements JdbcType {

	private final OracleJDBCTypeFactory typeFactory;

	/**
	 * Constructs a {@code SqlTypeDescriptor} for the Oracle SDOGeometry type.
	 *
	 * @param typeFactory the type factory to use.
	 */
	public SDOGeometryType(OracleJDBCTypeFactory typeFactory) {
		this.typeFactory = typeFactory;
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
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new SDOGeometryValueBinder( javaTypeDescriptor, this, typeFactory );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new SDOGeometryValueExtractor( javaTypeDescriptor, this );
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
