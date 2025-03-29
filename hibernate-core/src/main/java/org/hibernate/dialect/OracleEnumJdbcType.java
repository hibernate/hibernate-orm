/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;
import static org.hibernate.type.descriptor.converter.internal.EnumHelper.getEnumeratedValues;

/**
 * Represents a named {@code enum} type on Oracle 23ai+.
 * <p>
 * Hibernate does <em>not</em> automatically use this for enums
 * mapped as {@link jakarta.persistence.EnumType#STRING}, and
 * instead this type must be explicitly requested using:
 * <pre>
 * &#64;JdbcTypeCode(SqlTypes.NAMED_ENUM)
 * </pre>
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ENUM
 * @see OracleDialect#getEnumTypeDeclaration(String, String[])
 * @see OracleDialect#getCreateEnumTypeCommand(String, String[])
 *
 * @author Loïc Lefèvre
 */
public class OracleEnumJdbcType implements JdbcType {

	public static final OracleEnumJdbcType INSTANCE = new OracleEnumJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return NAMED_ENUM;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		@SuppressWarnings("unchecked")
		final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
		return (appender, value, dialect, wrapperOptions) -> {
			appender.appendSql( dialect.getEnumTypeDeclaration( enumClass ) );
			appender.appendSql( '.' );
			appender.appendSql( ((Enum<?>) value).name() );
		};
	}

	@Override
	public String getFriendlyName() {
		return "ENUM";
	}

	@Override
	public String toString() {
		return "EnumTypeDescriptor";
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, getJdbcTypeCode() );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, getJdbcTypeCode() );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setString( index, getJavaType().unwrap( value, String.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, getJavaType().unwrap( value, String.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( name ), options );
			}
		};
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			BasicValueConverter<?, ?> valueConverter,
			Size columnSize,
			Database database,
			JdbcTypeIndicators context) {
		@SuppressWarnings("unchecked")
		final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
		@SuppressWarnings("unchecked")
		final String[] enumeratedValues =
				valueConverter == null
						? getEnumeratedValues( enumClass )
						: getEnumeratedValues( enumClass, (BasicValueConverter<Enum<?>,?>) valueConverter ) ;
		if ( getDefaultSqlTypeCode() == NAMED_ENUM ) {
			Arrays.sort( enumeratedValues );
		}
		final Dialect dialect = database.getDialect();
		final String[] create =
				getCreateEnumTypeCommand( javaType.getJavaTypeClass().getSimpleName(), enumeratedValues, dialect );
		final String[] drop = dialect.getDropEnumTypeCommand( enumClass );
		if ( create != null && create.length > 0 ) {
			database.addAuxiliaryDatabaseObject(
					new NamedAuxiliaryDatabaseObject(
							enumClass.getSimpleName(),
							database.getDefaultNamespace(),
							create,
							drop,
							emptySet(),
							true
					)
			);
		}
	}

	String[] getCreateEnumTypeCommand(String name, String[] values, Dialect dialect) {
		return OracleDialect.getCreateVarcharEnumTypeCommand( name, values );
	}
}
