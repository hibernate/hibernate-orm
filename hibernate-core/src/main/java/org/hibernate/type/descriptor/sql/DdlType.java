/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for a DDL type.
 *
 * @author Christian Beikov
 */
public interface DdlType extends Serializable {

	/**
	 * The {@linkplain SqlTypes SQL type code} of the descriptor.
	 *
	 * @return a SQL type code
	 */
	int getSqlTypeCode();

	String getRawTypeName();

	String getTypeNamePattern();

	default String getTypeName(Size size) {
		return getTypeName( size.getLength(), size.getPrecision(), size.getScale() );
	}

	String getTypeName(Long size, Integer precision, Integer scale);

	String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType);

	/**
	 * Get the name of the database type appropriate for casting operations
	 * (via the CAST() SQL function) for the given {@link SqlExpressible}
	 * SQL type.
	 *
	 * @return The database type name
	 */
	default String getCastTypeName(SqlExpressible type, Long length, Integer precision, Integer scale) {
		return getCastTypeName(
				type.getJdbcMapping().getJdbcType(),
				type.getJdbcMapping().getJavaTypeDescriptor(),
				length,
				precision,
				scale
		);
	}

	String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale);
}
