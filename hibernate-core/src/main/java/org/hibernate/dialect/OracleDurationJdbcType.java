/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import oracle.jdbc.OracleTypes;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.DurationJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

public class OracleDurationJdbcType extends DurationJdbcType {

	public static final OracleDurationJdbcType INSTANCE = new OracleDurationJdbcType();

	public OracleDurationJdbcType() {
		super();
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, javaType.unwrap( value, Duration.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, javaType.unwrap( value, Duration.class, options ) );
			}


		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				// Handle the fact that a duration could also come as number of nanoseconds
				final Object nativeValue = rs.getObject( paramIndex );
				if ( nativeValue instanceof Number ) {
					return javaType.wrap( nativeValue, options );
				}
				return javaType.wrap( rs.getObject( paramIndex, Duration.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				// Handle the fact that a duration could also come as number of nanoseconds
				final Object nativeValue = statement.getObject( index );
				if ( nativeValue instanceof Number ) {
					return javaType.wrap( nativeValue, options );
				}
				return javaType.wrap( statement.getObject( index, Duration.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				// Handle the fact that a duration could also come as number of nanoseconds
				final Object nativeValue = statement.getObject( name );
				if ( nativeValue instanceof Number ) {
					return javaType.wrap( nativeValue, options );
				}
				return javaType.wrap( statement.getObject( name, Duration.class ), options );
			}
		};
	}

	/**
	 * The {@linkplain SqlTypes JDBC type code} used when interacting with JDBC APIs.
	 * <p>
	 * For example, it's used when calling {@link java.sql.PreparedStatement#setNull(int, int)}.
	 *
	 * @return a JDBC type code
	 */
	public int getJdbcTypeCode() {
		return SqlTypes.DURATION;
	}
	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type to
	 * be used for schema generation.
	 * <p>
	 * This value is passed to {@link DdlTypeRegistry#getTypeName(int, Size, Type)}
	 * to obtain the SQL column type.
	 *
	 * @return a JDBC type code
	 * @since 6.2
	 */
	public int getDdlTypeCode() {
		return OracleTypes.INTERVALDS;
	}
	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type.
	 * <p>
	 * This value might be different from {@link #getDdlTypeCode()} if the actual type
	 * e.g. JSON is emulated through a type like CLOB.
	 *
	 * @return a JDBC type code
	 */
	public int getDefaultSqlTypeCode() {
		return OracleTypes.INTERVALDS;
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Duration.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> dialect.appendIntervalLiteral(
				appender,
				javaType.unwrap(
						value,
						Duration.class,
						wrapperOptions
				)
		);
	}
}
