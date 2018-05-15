/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link Character} handling.
 *
 * @author Steve Ebersole
 */
public class CharacterJavaDescriptor extends AbstractBasicJavaDescriptor<Character> implements Primitive<Character> {
	public static final CharacterJavaDescriptor INSTANCE = new CharacterJavaDescriptor();

	public CharacterJavaDescriptor() {
		super( Character.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.CHAR );
	}

	@Override
	public String toString(Character value) {
		return value.toString();
	}
	@Override
	public Character fromString(String string) {
		if ( string.length() != 1 ) {
			throw new HibernateException( "multiple or zero characters found parsing string" );
		}
		return string.charAt( 0 );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Character value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		if ( Number.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( (short)value.charValue() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Character wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Character.class.isInstance( value ) ) {
			return (Character) value;
		}
		if ( String.class.isInstance( value ) ) {
			final String str = (String) value;
			return str.charAt( 0 );
		}
		if ( Number.class.isInstance( value ) ) {
			final Number nbr = (Number) value;
			return (char) nbr.shortValue();
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class getPrimitiveClass() {
		return char.class;
	}

	@Override
	public Character getDefaultValue() {
		throw new UnsupportedOperationException( "char has no non-null default" );
	}


}
