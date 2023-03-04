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
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import oracle.jdbc.OracleConnection;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class OracleArrayJdbcType extends ArrayJdbcType {

	private final String typeName;

	public OracleArrayJdbcType() {
		super( ObjectJdbcType.INSTANCE );
		this.typeName = null;
	}

	private OracleArrayJdbcType(String typeName, JdbcType elementJdbcType) {
		super( elementJdbcType );
		this.typeName = typeName;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		// No array literal support
		return null;
	}

	@Override
	public JdbcType resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			JdbcType elementType,
			ColumnTypeInformation columnTypeInformation) {
		String typeName = columnTypeInformation.getTypeName();
		if ( typeName == null || typeName.isBlank() ) {
			typeName = dialect.getArrayTypeName(
					typeConfiguration.getDdlTypeRegistry().getTypeName(
							elementType.getDdlTypeCode(),
							dialect
					)
			);
		}
		if ( typeName == null ) {
			// Fallback to XML type for the representation of arrays as the native JSON type was only introduced in 21
			// Also, use the XML type if the Oracle JDBC driver classes are not visible
			return typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.SQLXML );
		}
		return new OracleArrayJdbcType( typeName, elementType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		final BasicPluralJavaType<X> containerJavaType = (BasicPluralJavaType<X>) javaTypeDescriptor;
		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.ARRAY, typeName );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.ARRAY, typeName );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final java.sql.Array arr = getArray( value, containerJavaType, options );
				st.setArray( index, arr );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = getArray( value, containerJavaType, options );
				try {
					st.setObject( name, arr, Types.ARRAY );
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

				final SharedSessionContractImplementor session = options.getSession();
				final OracleConnection oracleConnection = session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.unwrap( OracleConnection.class );
				try {
					return oracleConnection.createOracleArray( typeName, objects );
				}
				catch (Exception e) {
					throw new HibernateException( "Couldn't create a java.sql.Array", e );
				}
			}
		};
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		OracleArrayJdbcType that = (OracleArrayJdbcType) o;

		return Objects.equals( typeName, that.typeName );
	}

	@Override
	public int hashCode() {
		return typeName != null ? typeName.hashCode() : 0;
	}
}
