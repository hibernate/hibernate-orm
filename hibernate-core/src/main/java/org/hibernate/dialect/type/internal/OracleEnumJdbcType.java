/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;

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
 * @see SqlTypes#NAMED_ENUM
 * @see OracleDialect#getEnumSupport()
 *
 * @author Loïc Lefèvre
 */
public class OracleEnumJdbcType implements SqlTypedJdbcType {

	public static final OracleEnumJdbcType INSTANCE = new OracleEnumJdbcType();

	private final String typeName;
	private final List<String> resolvedValues;

	public OracleEnumJdbcType() {
		this( null );
	}

	public OracleEnumJdbcType(String typeName) {
		this( typeName, null );
	}

	protected OracleEnumJdbcType(String typeName, List<String> values) {
		this.typeName = typeName;
		this.resolvedValues = values == null ? null : List.copyOf( values );
	}

	public OracleEnumJdbcType withResolvedName(String name, List<String> values) {
		return new OracleEnumJdbcType( name, values );
	}

	@Override
	public String getSqlTypeName() {
		return typeName;
	}

	public OracleEnumJdbcType withTypeName(String name) {
		return new OracleEnumJdbcType( name );
	}

	private String typeName(Class<?> enumClass) {
		return typeName == null ? enumClass.getSimpleName() : typeName;
	}

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
			appender.appendSql( typeName == null ? dialect.getEnumSupport().getTypeDeclaration( enumClass ) : typeName );
			appender.appendSql( '.' );
			appender.appendSql( value instanceof Enum<?> enumValue ? enumValue.name() : value.toString() );
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
			Database database) {
		@SuppressWarnings("unchecked")
		final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) javaType.getJavaType();
		@SuppressWarnings("unchecked")
		final String[] enumeratedValues =
				resolvedValues != null ? resolvedValues.toArray( String[]::new )
						: valueConverter == null
						? getEnumeratedValues( enumClass )
						: getEnumeratedValues( enumClass, (BasicValueConverter<Enum<?>,?>) valueConverter ) ;
		if ( getDefaultSqlTypeCode() == NAMED_ENUM ) {
			Arrays.sort( enumeratedValues );
		}
		final Dialect dialect = database.getDialect();
		final String[] create =
				getCreateTypeCommands( typeName( enumClass ), enumeratedValues, dialect );
		final String[] drop = dialect.getEnumSupport().getDropTypeCommands( typeName( enumClass ) );
		if ( create != null && create.length > 0 ) {
			database.addAuxiliaryDatabaseObject(
					new NamedAuxiliaryDatabaseObject(
							typeName( enumClass ),
							database.getDefaultNamespace(),
							create,
							drop,
							emptySet(),
							true
					)
			);
		}
	}

	String[] getCreateTypeCommands(String name, String[] values, Dialect dialect) {
		return OracleDialect.getCreateVarcharEnumTypeCommand( name, values );
	}
}
