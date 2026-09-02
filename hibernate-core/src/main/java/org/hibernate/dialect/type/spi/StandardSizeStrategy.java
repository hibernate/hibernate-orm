/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Standard column-size resolution used by [Dialect].
///
/// Pass the same Dialect which returns this strategy from
/// [Dialect#getSizeStrategy()]. The strategy retains that Dialect so Java types
/// can calculate database-specific default lengths, precisions, and scales.
/// Hibernate may reuse the strategy for the Dialect's lifetime.
///
/// Extend this class when a database changes selected JDBC-type cases. Override
/// [#resolveSize(JdbcType, JavaType, Integer, Integer, Long)] for those cases and
/// delegate all other cases to `super.resolveSize(...)`.
///
/// @see Dialect#getSizeStrategy()
///
/// @since 8.0
/// @author Steve Ebersole
/// @author Gavin King
@SPI({ USE, IMPLEMENT, SUPPLY })
public class StandardSizeStrategy implements SizeStrategy {
	private static final double LOG_BASE2OF10 = log( 10 ) / log( 2 );

	private final Dialect dialect;

	/// Create the standard strategy for the Dialect which supplies it.
	///
	/// Construction retains the Dialect but performs no size resolution and
	/// invokes no overridable Dialect or strategy method.
	///
	/// @param dialect the non-null supplying Dialect
	public StandardSizeStrategy(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
		this.dialect = dialect;
	}

	@Override
	public Size resolveSize(
			JdbcType jdbcType,
			JavaType<?> javaType,
			Integer precision,
			Integer scale,
			Long length) {
		final var size = new Size();
		// Set the explicit length to null if we encounter the JPA default of 255
		if ( length != null && length == Size.DEFAULT_LENGTH ) {
			length = null;
		}

		switch ( jdbcType.getDdlTypeCode() ) {
			case SqlTypes.ARRAY:
				break;
			case SqlTypes.BIT:
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.CLOB:
			case SqlTypes.BLOB:
				size.setLength( javaType.getDefaultSqlLength( dialect, jdbcType ) );
				break;
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONGVARBINARY:
				size.setLength( javaType.getLongSqlLength() );
				break;
			case SqlTypes.FLOAT:
			case SqlTypes.DOUBLE:
			case SqlTypes.REAL:
				// this is almost always the thing we use:
				length = null;
				size.setPrecision( javaType.getDefaultSqlPrecision( dialect, jdbcType ) );
				if ( scale != null && scale != 0 ) {
					throw new IllegalArgumentException( "scale has no meaning for SQL floating point types" );
				}
				// but if the user explicitly specifies the precision, we need to convert it:
				if ( precision != null ) {
					// convert from base 10 (as specified in @Column) to base 2 (as specified by SQL)
					// using the magic of high school math: log_2(10^n) = n*log_2(10) = n*ln(10)/ln(2)
					precision = (int) ceil( precision * LOG_BASE2OF10 );
				}
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
			case SqlTypes.TIMESTAMP:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				length = null;
				size.setPrecision( javaType.getDefaultSqlPrecision( dialect, jdbcType ) );
				if ( scale != null && scale != 0 ) {
					throw new IllegalArgumentException( "scale has no meaning for SQL time or timestamp types" );
				}
				break;
			case SqlTypes.NUMERIC:
			case SqlTypes.DECIMAL:
			case SqlTypes.INTERVAL_SECOND:
				size.setPrecision( javaType.getDefaultSqlPrecision( dialect, jdbcType ) );
				size.setScale( javaType.getDefaultSqlScale( dialect, jdbcType ) );
				break;
		}

		if ( precision != null ) {
			size.setPrecision( precision );
		}
		if ( scale != null ) {
			size.setScale( scale );
		}
		if ( length != null ) {
			size.setLength( length );
		}
		return size;
	}
}
