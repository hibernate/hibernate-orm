/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Descriptor for a DDL column type. An instance of this type abstracts over
 * a parameterized family of {@linkplain Dialect dialect-specific} SQL types
 * with the same {@linkplain #getSqlTypeCode() type code} but varying length,
 * precision, and scale. Usually, the types belonging to the family share a
 * single {@linkplain #getRawTypeName() type name} in SQL, but in certain
 * cases, most notably, in the case of the MySQL LOB types {@code text} and
 * {@code blob}, it's the type name itself which is parameter-dependent.
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

	/**
	 * Return a type with length, precision, and scale specified by the given
	 * {@linkplain Size size object}. The given type may be used to
	 * determine additional aspects of the returned SQL type.
	 *
	 * @since 6.3
	 */
	default String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return getTypeName( columnSize );
	}

	/**
	 * Returns the default type name without precision/length and scale parameters.
	 *
	 * @deprecated not appropriate for named enum or array types
	 */
	@Deprecated(since = "6.3")
	String getRawTypeName();

	/**
	 * Returns all type names without precision/length and scale parameters.
	 *
	 * @deprecated not appropriate for named enum or array types
	 */
	@Incubating
	@Deprecated(since = "6.3")
	default String[] getRawTypeNames() {
		return new String[] { getRawTypeName() };
	}

	/**
	 * Return a type with length, precision, and scale specified by the given
	 * {@linkplain Size size object}.
	 *
	 * @deprecated not appropriate for named enum or array types,
	 *             use {@link #getTypeName(Size, Type, DdlTypeRegistry)} instead
	 */
	@Deprecated(since = "6.3")
	default String getTypeName(Size size) {
		return getTypeName( size.getLength(), size.getPrecision(), size.getScale() );
	}

	/**
	 * Return a type with the given length, precision, and scale.
	 *
	 * @deprecated not appropriate for named enum or array types,
	 *             use {@link #getTypeName(Size, Type, DdlTypeRegistry)} instead
	 */
	@Deprecated(since = "6.3")
	String getTypeName(Long size, Integer precision, Integer scale);

	default boolean isLob(Size size) {
		// Let's be defensive and assume that LONG32 are LOBs as well
		return JdbcType.isLobOrLong( getSqlTypeCode() );
	}

	/**
	 * Return the database type corresponding to the given {@link SqlExpressible}
	 * that may be used as a target type in casting operations using the SQL
	 * {@code CAST()} function. The length is usually
	 * chosen to be the maximum possible length for the dialect.
	 *
	 * @see JavaType#getDefaultSqlScale(Dialect, JdbcType)
	 * @see JavaType#getDefaultSqlPrecision(Dialect, JdbcType)
	 * @see Dialect#getMaxVarcharLength()
	 *
	 * @return The SQL type name
	 *
	 * @since 6.3
	 */
	default String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		return getCastTypeName( type, columnSize.getLength(), columnSize.getPrecision(), columnSize.getScale() );
	}

	/**
	 * Return the database type corresponding to the given {@link JdbcType}
	 * that may be used as a target type in casting operations using the SQL
	 * {@code CAST()} function, using the given {@link JavaType} to help
	 * determine the appropriate precision and scale. The length is usually
	 * chosen to be the maximum possible length for the dialect.
	 *
	 * @see JavaType#getDefaultSqlScale(Dialect, JdbcType)
	 * @see JavaType#getDefaultSqlPrecision(Dialect, JdbcType)
	 * @see Dialect#getMaxVarcharLength()
	 *
	 * @return The SQL type name
	 * @deprecated Use {@link #getCastTypeName(Size, SqlExpressible, DdlTypeRegistry)} instead
	 */
	@Deprecated(forRemoval = true)
	String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType);

	/**
	 * Return the database type with the given length, precision, and scale,
	 * if specified, corresponding to the {@link SqlExpressible#getJdbcMapping()
	 * JdbcMapping} of the given {@link SqlExpressible}, that may be used as a
	 * target type in casting operations using the SQL {@code CAST()} function.
	 *
	 * @param type the {@link SqlExpressible}
	 * @param length the length, or null, if unspecified
	 * @param precision the precision, or null, if unspecified
	 * @param scale the scale, or null, if unspecified
	 *
	 * @return The SQL type name
	 * @deprecated Use {@link #getCastTypeName(Size, SqlExpressible, DdlTypeRegistry)} instead
	 */
	@Deprecated(forRemoval = true)
	default String getCastTypeName(SqlExpressible type, Long length, Integer precision, Integer scale) {
		return getCastTypeName(
				type.getJdbcMapping().getJdbcType(),
				type.getJdbcMapping().getJavaTypeDescriptor(),
				length,
				precision,
				scale
		);
	}

	/**
	 * Return the database type with the given length, precision, and scale,
	 * if specified, corresponding to the given {@link JdbcType}, that may
	 * be used as a target type in casting operations using the SQL
	 * {@code CAST()} function, using the given {@link JavaType} to help
	 * determine the appropriate precision and scale. The length, if not
	 * explicitly specified, is usually chosen to be the maximum possible
	 * length for the dialect.
	 *
	 * @see JavaType#getDefaultSqlScale(Dialect, JdbcType)
	 * @see JavaType#getDefaultSqlPrecision(Dialect, JdbcType)
	 * @see Dialect#getMaxVarcharLength()
	 *
	 * @return The SQL type name
	 * @deprecated Use {@link #getCastTypeName(Size, SqlExpressible, DdlTypeRegistry)} instead
	 */
	@Deprecated(forRemoval = true)
	String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale);
}
