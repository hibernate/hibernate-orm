/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterArray;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class ArrayJdbcType implements JdbcType {

	private final JdbcType elementJdbcType;

	public ArrayJdbcType(JdbcType elementJdbcType) {
		this.elementJdbcType = elementJdbcType;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.ARRAY;
	}

	public JdbcType getElementJdbcType() {
		return elementJdbcType;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		final JavaType<Object> elementJavaType = elementJdbcType.getJdbcRecommendedJavaTypeMapping(
				precision,
				scale,
				typeConfiguration
		);
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
				Array.newInstance( elementJavaType.getJavaTypeClass(), 0 ).getClass()
		);
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		final JavaType<T> elementJavaType;
		if ( javaTypeDescriptor instanceof ByteArrayJavaType ) {
			// Special handling needed for Byte[], because that would conflict with the VARBINARY mapping
			//noinspection unchecked
			elementJavaType = (JavaType<T>) ByteJavaType.INSTANCE;
		}
		else if (javaTypeDescriptor instanceof BasicPluralJavaType) {
			//noinspection unchecked
			elementJavaType = ((BasicPluralJavaType<T>) javaTypeDescriptor).getElementJavaType();
		}
		else {
			throw new IllegalArgumentException("not a BasicPluralJavaType");
		}
		return new JdbcLiteralFormatterArray<>(
				javaTypeDescriptor,
				elementJdbcType.getJdbcLiteralFormatter( elementJavaType )
		);
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return java.sql.Array.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setArray( index, getArray( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = getArray( value, options );
				try {
					st.setObject( name, arr, java.sql.Types.ARRAY );
				}
				catch (SQLException ex) {
					throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
				}
			}

			private java.sql.Array getArray(X value, WrapperOptions options) throws SQLException {
				final TypeConfiguration typeConfiguration = options.getSessionFactory().getTypeConfiguration();
				final JdbcType elementJdbcType = ( (ArrayJdbcType) getJdbcType() ).getElementJdbcType();
				final JdbcType underlyingJdbcType = typeConfiguration.getJdbcTypeRegistry()
						.getDescriptor( elementJdbcType.getDefaultSqlTypeCode() );
				final Class<?> preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( options );
				final Class<?> elementJdbcJavaTypeClass;
				if ( preferredJavaTypeClass == null ) {
					elementJdbcJavaTypeClass = underlyingJdbcType.getJdbcRecommendedJavaTypeMapping(
							null,
							null,
							typeConfiguration
					).getJavaTypeClass();
				}
				else {
					elementJdbcJavaTypeClass = preferredJavaTypeClass;
				}
				//noinspection unchecked
				final Class<Object[]> arrayClass = (Class<Object[]>)
						Array.newInstance( elementJdbcJavaTypeClass, 0 ).getClass();
				final Object[] objects = getJavaType().unwrap( value, arrayClass, options );

				final SharedSessionContractImplementor session = options.getSession();
				final String typeName = getElementTypeName( elementJdbcType, session );
				return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.createArrayOf( typeName, objects );
			}

			private String getElementTypeName(JdbcType elementJdbcType, SharedSessionContractImplementor session) {
				// TODO: ideally, we would have the actual size or the actual type/column accessible
				//       this is something that we would need for supporting composite types anyway
				final JavaType<X> elementJavaType;
				if ( getJavaType() instanceof ByteArrayJavaType ) {
					// Special handling needed for Byte[], because that would conflict with the VARBINARY mapping
					//noinspection unchecked
					elementJavaType = (JavaType<X>) ByteJavaType.INSTANCE;
				}
				else {
					//noinspection unchecked
					elementJavaType = ( (BasicPluralJavaType<X>) getJavaType() ).getElementJavaType();
				}
				final Size size = session.getJdbcServices()
						.getDialect()
						.getSizeStrategy()
						.resolveSize( elementJdbcType, elementJavaType, null, null, null );
				final DdlTypeRegistry ddlTypeRegistry = session.getTypeConfiguration().getDdlTypeRegistry();
				final String typeName = ddlTypeRegistry.getDescriptor( elementJdbcType.getDdlTypeCode() )
						.getTypeName( size, new BasicTypeImpl<>( elementJavaType, elementJdbcType), ddlTypeRegistry );
				int cutIndex = typeName.indexOf( '(' );
				if ( cutIndex > 0 ) {
					// getTypeName for this case required length, etc, parameters.
					// Cut them out and use database defaults.
					return typeName.substring( 0, cutIndex );
				}
				else {
					return typeName;
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getArray( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getArray( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getArray( name ), options );
			}
		};
	}

	@Override
	public String getFriendlyName() {
		return "ARRAY";
	}

	@Override
	public String toString() {
		return "ArrayTypeDescriptor";
	}

	/**
	 * Check equality. Needed so that ArrayJdbcType in collections correctly match each other.
	 *
	 * @param o other object
	 * @return true if the two array types share the same element type
	 */
	@Override
	public boolean equals(Object o) {
		return o != null &&
				getClass() == o.getClass() &&
				getElementJdbcType().equals( ((ArrayJdbcType) o).getElementJdbcType() );
	}

	@Override
	public int hashCode() {
		return getJdbcTypeCode() + getElementJdbcType().hashCode();
	}
}
