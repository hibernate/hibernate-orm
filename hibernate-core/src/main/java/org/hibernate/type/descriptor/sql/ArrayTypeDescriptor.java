/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.GenericArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterNumericData;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class ArrayTypeDescriptor implements SqlTypeDescriptor {

	public static final ArrayTypeDescriptor INSTANCE = new ArrayTypeDescriptor();
	private static final BasicJavaDescriptor RECOMMENDED_JAVA_TYPE_MAPPING = new GenericArrayTypeDescriptor(
			JavaObjectType.INSTANCE
	);
	private static final ClassValue<Method> NAME_BINDER = new ClassValue<Method>() {
		@Override
		protected Method computeValue(Class<?> type) {
			try {
				return type.getMethod( "setArray", String.class, java.sql.Array.class );
			}
			catch ( Exception ex ) {
				// add logging? Did we get NoSuchMethodException or SecurityException?
				// Doesn't matter which. We can't use it.
			}
			return null;
		}
	};

	public ArrayTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) RECOMMENDED_JAVA_TYPE_MAPPING;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		//noinspection unchecked
		// TODO: array literal formatter? introduce a dialect contract?
		return new JdbcLiteralFormatterNumericData( javaTypeDescriptor, Long.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return java.sql.Array.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final java.sql.Array arr = javaTypeDescriptor.unwrap( value, java.sql.Array.class, options );
				st.setArray( index, arr );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = javaTypeDescriptor.unwrap( value, java.sql.Array.class, options );
				final Method nameBinder = NAME_BINDER.get( st.getClass() );
				if ( nameBinder == null ) {
					try {
						st.setObject( name, arr, java.sql.Types.ARRAY );
						return;
					}
					catch (SQLException ex) {
						throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
					}
				}
				// Not that it's supposed to have setArray(String,Array) by standard.
				// There are numerous missing methods that only have versions for positional parameter,
				// but not named ones.

				try {
					nameBinder.invoke( st, name, arr );
				}
				catch ( Throwable t ) {
					throw new HibernateException( t );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {
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
		return "ArrayTypeDescriptor(" + getFriendlyName() + ")";
	}
}
