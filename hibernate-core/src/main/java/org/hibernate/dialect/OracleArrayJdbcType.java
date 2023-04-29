/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import oracle.jdbc.OracleConnection;

import static java.sql.Types.ARRAY;
import static java.util.Collections.emptySet;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class OracleArrayJdbcType extends ArrayJdbcType {

	private final String typeName;

	public OracleArrayJdbcType() {
		this( null, null );
	}

	public OracleArrayJdbcType(JdbcType elementJdbcType, String typeName) {
		super( elementJdbcType );
		this.typeName = typeName;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return null;
	}

	@Override
	public JdbcType resolveType(TypeConfiguration typeConfiguration, Dialect dialect, BasicType<?> elementType, ColumnTypeInformation columnTypeInformation) {
		String typeName = columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			typeName = getTypeName( elementType.getJavaTypeDescriptor(), dialect );
		}
//		if ( typeName == null ) {
//			// Fallback to XML type for the representation of arrays as the native JSON type was only introduced in 21
//			// Also, use the XML type if the Oracle JDBC driver classes are not visible
//			return typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.SQLXML );
//		}
		return new OracleArrayJdbcType( elementType.getJdbcType(), typeName );
	}

	@Override
	public JdbcType resolveType(TypeConfiguration typeConfiguration, Dialect dialect, JdbcType elementType, ColumnTypeInformation columnTypeInformation) {
		// a bit wrong!
		return new OracleArrayJdbcType( elementType, columnTypeInformation.getTypeName() );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		final BasicPluralJavaType<X> containerJavaType = (BasicPluralJavaType<X>) javaTypeDescriptor;
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			private String typeName(WrapperOptions options) {
				return ( typeName == null ? getTypeName( options, containerJavaType ) : typeName )
						.toUpperCase(Locale.ROOT);
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
				st.setArray( index, getArray( value, containerJavaType, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = getArray( value, containerJavaType, options );
				try {
					st.setObject( name, arr, ARRAY );
				}
				catch (SQLException ex) {
					throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
				}
			}

			private java.sql.Array getArray(
					X value,
					BasicPluralJavaType<X> containerJavaType,
					WrapperOptions options) throws SQLException {
				//noinspection unchecked
				final Class<Object[]> arrayClass = (Class<Object[]>) Array.newInstance(
						getElementJdbcType().getPreferredJavaTypeClass( options ),
						0
				).getClass();
				final Object[] objects = javaTypeDescriptor.unwrap( value, arrayClass, options );
				final String arrayTypeName = typeName( options ).toUpperCase(Locale.ROOT);

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

	private static String getTypeName(WrapperOptions options, BasicPluralJavaType<?> containerJavaType) {
		Dialect dialect = options.getSessionFactory().getJdbcServices().getDialect();
		return getTypeName( containerJavaType.getElementJavaType(), dialect );
	}

	private static String getTypeName(JavaType<?> elementJavaType, Dialect dialect) {
		return dialect.getArrayTypeName(
				elementJavaType.getJavaTypeClass().getSimpleName(),
				null // not needed by OracleDialect.getArrayTypeName()
		);
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
		final Dialect dialect = database.getDialect();
		final BasicPluralJavaType<?> pluralJavaType = (BasicPluralJavaType<?>) javaType;
		final String elementTypeName = typeName==null
				? getTypeName( pluralJavaType.getElementJavaType(), dialect )
				: typeName;
		final String elementType =
				typeConfiguration.getDdlTypeRegistry().getTypeName(
						getElementJdbcType().getDdlTypeCode(),
						dialect.getSizeStrategy().resolveSize(
								getElementJdbcType(),
								pluralJavaType.getElementJavaType(),
								columnSize.getPrecision(),
								columnSize.getScale(),
								columnSize.getLength()
						)
				);
		final String[] create = new String[] { "create or replace type " + elementTypeName + " as varying array(255) of " + elementType };
		final String[] drop = new String[] {
//				"drop type " + elementTypeName
		};
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject( elementTypeName, database.getDefaultNamespace(), create, drop, emptySet(), true )
		);
	}

//	@Override
//	public String getExtraCreateTableInfo(JavaType<?> javaType, String columnName, String tableName, Database database) {
//		final Dialect dialect = database.getDialect();
//		final BasicPluralJavaType<?> pluralJavaType = (BasicPluralJavaType<?>) javaType;
//		String elementTypeName = getTypeName( pluralJavaType.getElementJavaType(), dialect );
//		return " nested table " + columnName + " store as " + tableName + columnName + elementTypeName;
//	}
}
