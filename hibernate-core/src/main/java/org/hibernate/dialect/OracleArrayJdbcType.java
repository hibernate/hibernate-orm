/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
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
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.StructJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import oracle.jdbc.OracleConnection;

import static java.sql.Types.ARRAY;

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
		//noinspection unchecked
		final ValueBinder<Object> elementBinder = getElementJdbcType().getBinder( ( (BasicPluralJavaType<Object>) javaTypeDescriptor ).getElementJavaType() );
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			private String typeName(WrapperOptions options) {
				return ( upperTypeName == null
						? getTypeName( options, (BasicPluralJavaType<?>) getJavaType(), (ArrayJdbcType) getJdbcType() ).toUpperCase( Locale.ROOT )
						: upperTypeName
				);
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
				final Object[] objects = ( (OracleArrayJdbcType) getJdbcType() ).getArray( this, elementBinder, value, options );
				final String arrayTypeName = typeName( options );

				final OracleConnection oracleConnection = options.getSession()
						.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.unwrap( OracleConnection.class );
				try {
					return oracleConnection.createOracleArray( arrayTypeName, objects );
				}
				catch (Exception e) {
					throw new HibernateException( "Couldn't create a java.sql.Array", e );
				}
			}
		};
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
		Dialect dialect = options.getSessionFactory().getJdbcServices().getDialect();
		return getTypeName( containerJavaType.getElementJavaType(), arrayJdbcType.getElementJdbcType(), dialect );
	}

	static String getTypeName(BasicType<?> elementType, Dialect dialect) {
		final BasicValueConverter<?, ?> converter = elementType.getValueConverter();
		if ( converter != null ) {
			final String simpleName;
			if ( converter instanceof JpaAttributeConverter<?, ?> ) {
				simpleName = ( (JpaAttributeConverter<?, ?>) converter ).getConverterJavaType()
						.getJavaTypeClass()
						.getSimpleName();
			}
			else {
				simpleName = converter.getClass().getSimpleName();
			}
			return dialect.getArrayTypeName(
					simpleName,
					null, // not needed by OracleDialect.getArrayTypeName()
					null // not needed by OracleDialect.getArrayTypeName()
			);
		}
		return getTypeName( elementType.getJavaTypeDescriptor(), elementType.getJdbcType(), dialect );
	}

	static String getTypeName(JavaType<?> elementJavaType, JdbcType elementJdbcType, Dialect dialect) {
		final String simpleName;
		if ( elementJavaType.getJavaTypeClass().isArray() ) {
			simpleName = dialect.getArrayTypeName(
					elementJavaType.getJavaTypeClass().getComponentType().getSimpleName(),
					null, // not needed by OracleDialect.getArrayTypeName()
					null // not needed by OracleDialect.getArrayTypeName()
			);
		}
		else if ( elementJdbcType instanceof StructJdbcType ) {
			simpleName = ( (StructJdbcType) elementJdbcType ).getStructTypeName();
		}
		else {
			final Class<?> preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( null );
			if ( preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
				simpleName = elementJavaType.getJavaTypeClass().getSimpleName();
			}
			else {
				if ( preferredJavaTypeClass.isArray() ) {
					simpleName = elementJavaType.getJavaTypeClass().getSimpleName() + dialect.getArrayTypeName(
							preferredJavaTypeClass.getComponentType().getSimpleName(),
							null,
							null
					);
				}
				else {
					simpleName = elementJavaType.getJavaTypeClass().getSimpleName() + preferredJavaTypeClass.getSimpleName();
				}
			}
		}
		return dialect.getArrayTypeName(
				simpleName,
				null, // not needed by OracleDialect.getArrayTypeName()
				null // not needed by OracleDialect.getArrayTypeName()
		);
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
		final JdbcType elementJdbcType = getElementJdbcType();
		if ( elementJdbcType instanceof StructJdbcType ) {
			// OracleAggregateSupport will take care of contributing the auxiliary database object
			return;
		}
		final Dialect dialect = database.getDialect();
		final BasicPluralJavaType<?> pluralJavaType = (BasicPluralJavaType<?>) javaType;
		final JavaType<?> elementJavaType = pluralJavaType.getElementJavaType();
		final String arrayTypeName = typeName == null ? getTypeName( elementJavaType, elementJdbcType, dialect ) : typeName;
		final String elementType = typeConfiguration.getDdlTypeRegistry().getTypeName(
				elementJdbcType.getDdlTypeCode(),
				dialect.getSizeStrategy().resolveSize(
						elementJdbcType,
						elementJavaType,
						columnSize.getPrecision(),
						columnSize.getScale(),
						columnSize.getLength()
				),
				new BasicTypeImpl<>( elementJavaType, elementJdbcType )
		);
		final UserDefinedArrayType userDefinedArrayType = database.getDefaultNamespace().createUserDefinedArrayType(
				Identifier.toIdentifier( arrayTypeName ),
				name -> new UserDefinedArrayType( "orm", database.getDefaultNamespace(), name )
		);
		userDefinedArrayType.setArraySqlTypeCode( getDdlTypeCode() );
		userDefinedArrayType.setElementTypeName( elementType );
		userDefinedArrayType.setElementSqlTypeCode( elementJdbcType.getDefaultSqlTypeCode() );
		userDefinedArrayType.setArrayLength( columnSize.getArrayLength() == null ? 127 : columnSize.getArrayLength() );
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
		return getElementJdbcType().getExtraCreateTableInfo(
				( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
				columnName,
				tableName,
				database
		);
	}

	@Override
	public String getFriendlyName() {
		return typeName;
	}

	@Override
	public String toString() {
		return "OracleArrayTypeDescriptor(" + typeName + ")";
	}
}

