/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import oracle.jdbc.OracleConnection;
import org.hibernate.type.spi.TypeConfiguration;

import static java.sql.Types.ARRAY;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class OracleArrayJdbcType extends ArrayJdbcType implements SqlTypedJdbcType {

	private final String typeName;
	private final String upperTypeName;

	public OracleArrayJdbcType(JdbcType elementJdbcType, String typeName) {
		super( elementJdbcType );
		this.typeName = typeName;
		this.upperTypeName = typeName == null ? null : typeName.toUpperCase( Locale.ROOT );
	}

	@Override
	public String getSqlTypeName() {
		return typeName;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new Binder<>( javaTypeDescriptor,
				(BasicPluralJavaType<?>) javaTypeDescriptor );
	}

	private class Binder<X,E> extends BasicBinder<X> {
		private final BasicPluralJavaType<E> pluralJavaType;

		private Binder(JavaType<X> javaType, BasicPluralJavaType<E> pluralJavaType) {
			super( javaType, OracleArrayJdbcType.this );
			this.pluralJavaType = pluralJavaType;
		}

		private String typeName(WrapperOptions options) {
				final var javaType = (BasicPluralJavaType<?>) getJavaType();
				final var jdbcType = (ArrayJdbcType) getJdbcType();
				return upperTypeName == null
						? getTypeName( options, javaType, jdbcType ).toUpperCase( Locale.ROOT )
						: upperTypeName;
		}

		@Override
		protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
			st.setNull( index, ARRAY, typeName( options ) );
		}

		@Override
		protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
			st.setNull( name, ARRAY, typeName( options ) );
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
			st.setArray( index, getBindValue( value, options ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			final java.sql.Array arr = getBindValue( value, options );
			try {
				st.setObject( name, arr, ARRAY );
			}
			catch (SQLException ex) {
				throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
			}
		}

		@Override
		public java.sql.Array getBindValue(X value, WrapperOptions options) throws SQLException {
			final var elementBinder = getElementJdbcType().getBinder( pluralJavaType.getElementJavaType() );
			final var objects = convertToArray( this, elementBinder, pluralJavaType, value, options );
			final String arrayTypeName = typeName( options );
			final var oracleConnection =
					options.getSession().getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
							.unwrap( OracleConnection.class );
			try {
				return oracleConnection.createOracleArray( arrayTypeName, objects );
			}
			catch (Exception e) {
				throw new HibernateException( "Couldn't create a java.sql.Array", e );
			}
		}
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getArray( this, rs.getArray( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getArray( this, statement.getArray( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getArray( this, statement.getArray( name ), options );
			}

		};
	}

	static String getTypeName(WrapperOptions options, BasicPluralJavaType<?> containerJavaType, ArrayJdbcType arrayJdbcType) {
		return getTypeName( containerJavaType.getElementJavaType(), arrayJdbcType.getElementJdbcType(), options.getDialect() );
	}

	static String getTypeName(BasicType<?> elementType, Dialect dialect) {
		final var converter = elementType.getValueConverter();
		return converter != null
				? dialect.getArrayTypeName(
						converterClassName( converter ),
						null, // not needed by OracleDialect.getArrayTypeName()
						null // not needed by OracleDialect.getArrayTypeName()
				)
				: getTypeName(
						elementType.getJavaTypeDescriptor(),
						elementType.getJdbcType(),
						dialect
				);
	}

	private static String converterClassName(BasicValueConverter<?, ?> converter) {
		return converter instanceof JpaAttributeConverter<?, ?> jpaConverter
				? jpaConverter.getConverterJavaType().getJavaTypeClass().getSimpleName()
				: converter.getClass().getSimpleName();
	}

	static String getTypeName(JavaType<?> elementJavaType, JdbcType elementJdbcType, Dialect dialect) {
		return dialect.getArrayTypeName(
				arrayClassName( elementJavaType, elementJdbcType, dialect ),
				null, // not needed by OracleDialect.getArrayTypeName()
				null // not needed by OracleDialect.getArrayTypeName()
		);
	}

	private static String arrayClassName(JavaType<?> elementJavaType, JdbcType elementJdbcType, Dialect dialect) {
		final var javaClass = elementJavaType.getJavaTypeClass();
		if ( javaClass.isArray() ) {
			return dialect.getArrayTypeName(
					javaClass.getComponentType().getSimpleName(),
					null, // not needed by OracleDialect.getArrayTypeName()
					null // not needed by OracleDialect.getArrayTypeName()
			);
		}
		else if ( elementJdbcType instanceof StructuredJdbcType structJdbcType ) {
			return structJdbcType.getStructTypeName();
		}
		else {
			final var preferredJavaTypeClass =
					elementJdbcType.getPreferredJavaTypeClass( null );
			if ( preferredJavaTypeClass == javaClass) {
				return javaClass.getSimpleName();
			}
			else {
				if ( preferredJavaTypeClass.isArray() ) {
					return javaClass.getSimpleName()
							+ dialect.getArrayTypeName(
									preferredJavaTypeClass.getComponentType().getSimpleName(),
									null,
									null
							);
				}
				else {
					return javaClass.getSimpleName()
							+ preferredJavaTypeClass.getSimpleName();
				}
			}
		}
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			BasicValueConverter<?, ?> valueConverter,
			Size columnSize,
			Database database,
			JdbcTypeIndicators context) {
		final var elementJdbcType = getElementJdbcType();
		if ( !(elementJdbcType instanceof StructuredJdbcType) ) {
			final var dialect = database.getDialect();
			final var pluralJavaType = (BasicPluralJavaType<?>) javaType;
			final var elementJavaType = pluralJavaType.getElementJavaType();
			final String elementTypeName =
					elementType( elementJavaType, elementJdbcType, columnSize, context.getTypeConfiguration(),
							dialect );
			final String arrayTypeName = arrayTypeName( elementJavaType, elementJdbcType, dialect );
			createUserDefinedArrayType( arrayTypeName, elementTypeName, columnSize, elementJdbcType, database );
		}
		// else OracleAggregateSupport will take care of contributing the auxiliary database object
	}

	private String arrayTypeName(JavaType<?> elementJavaType, JdbcType elementJdbcType, Dialect dialect) {
		return typeName == null
				? getTypeName( elementJavaType, elementJdbcType, dialect )
				: typeName;
	}

	private void createUserDefinedArrayType(
			String arrayTypeName, String elementTypeName, Size columnSize, JdbcType elementJdbcType, Database database) {
		final var defaultNamespace = database.getDefaultNamespace();
		final var userDefinedArrayType =
				defaultNamespace.createUserDefinedArrayType(
						toIdentifier( arrayTypeName ),
						name -> new UserDefinedArrayType( "orm", defaultNamespace, name )
				);
		userDefinedArrayType.setArraySqlTypeCode( getDdlTypeCode() );
		userDefinedArrayType.setElementTypeName( elementTypeName );
		userDefinedArrayType.setElementSqlTypeCode( elementJdbcType.getDefaultSqlTypeCode() );
		userDefinedArrayType.setElementDdlTypeCode( elementJdbcType.getDdlTypeCode() );
		userDefinedArrayType.setArrayLength( columnSize.getArrayLength() == null ? 127 : columnSize.getArrayLength() );
	}

	private static String elementType(
			JavaType<?> elementJavaType, JdbcType elementJdbcType, Size columnSize,
			TypeConfiguration typeConfiguration, Dialect dialect) {
		return typeConfiguration.getDdlTypeRegistry()
				.getTypeName( elementJdbcType.getDdlTypeCode(),
						dialect.getSizeStrategy().resolveSize( elementJdbcType, elementJavaType, columnSize ),
						new BasicTypeImpl<>(elementJavaType, elementJdbcType) );
	}

	@Override
	public void registerOutParameter(CallableStatement callableStatement, String name) throws SQLException {
		callableStatement.registerOutParameter( name, ARRAY, upperTypeName );
	}

	@Override
	public void registerOutParameter(CallableStatement callableStatement, int index) throws SQLException {
		callableStatement.registerOutParameter( index, ARRAY, upperTypeName );
	}

	@Override
	public String getExtraCreateTableInfo(JavaType<?> javaType, String columnName, String tableName, Database database) {
		final var pluralJavaType = (BasicPluralJavaType<?>) javaType;
		return getElementJdbcType()
				.getExtraCreateTableInfo( pluralJavaType.getElementJavaType(), columnName, tableName, database );
	}

	@Override
	public String getFriendlyName() {
		return typeName;
	}

	@Override
	public String toString() {
		return "OracleArrayTypeDescriptor(" + typeName + ")";
	}

	@Override
	public boolean equals(Object that) {
		return super.equals( that )
			&& that instanceof OracleArrayJdbcType jdbcType
			&& Objects.equals( typeName, jdbcType.typeName );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( typeName ) + 31 * super.hashCode();
	}
}
