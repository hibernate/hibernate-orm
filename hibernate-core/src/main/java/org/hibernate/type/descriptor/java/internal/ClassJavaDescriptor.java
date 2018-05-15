/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Class} handling.
 *
 * @author Steve Ebersole
 */
public class ClassJavaDescriptor extends AbstractBasicJavaDescriptor<Class> {
	public static final ClassJavaDescriptor INSTANCE = new ClassJavaDescriptor();

	public ClassJavaDescriptor() {
		super( Class.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	public String toString(Class value) {
		return value.getName();
	}

	public Class fromString(String string) {
		if ( string == null ) {
			return null;
		}

		try {
			return ReflectHelper.classForName( string );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Unable to locate named class " + string );
		}
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(Class value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Class.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> Class wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Class.class.isInstance( value ) ) {
			return (Class) value;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String)value );
		}
		throw unknownWrap( value.getClass() );
	}
}
