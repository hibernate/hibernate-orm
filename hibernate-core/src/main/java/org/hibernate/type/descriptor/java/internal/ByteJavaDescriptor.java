/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractNumericJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ByteVersionSupport;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

/**
 * Descriptor for {@link Byte} handling.
 *
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ByteJavaDescriptor extends AbstractNumericJavaDescriptor<Byte> implements Primitive<Byte> {
	public static final ByteJavaDescriptor INSTANCE = new ByteJavaDescriptor();
	public static final Byte ZERO = (byte) 0;

	private ByteJavaDescriptor() {
		super( Byte.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.TINYINT );
	}

	@Override
	public String toString(Byte value) {
		return value == null ? null : value.toString();
	}
	@Override
	public Byte fromString(String string) {
		return Byte.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Byte value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( value.shortValue() );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.intValue() );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.longValue() );
		}
		if ( Double.class.isAssignableFrom( type ) ) {
			return (X) Double.valueOf( value.doubleValue() );
		}
		if ( Float.class.isAssignableFrom( type ) ) {
			return (X) Float.valueOf( value.floatValue() );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Byte wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Byte.class.isInstance( value ) ) {
			return (Byte) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return ( (Number) value ).byteValue();
		}
		if ( String.class.isInstance( value ) ) {
			return Byte.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class getPrimitiveClass() {
		return byte.class;
	}

	@Override
	public Byte getDefaultValue() {
		return ZERO;
	}

	@Override
	public VersionSupport<Byte> getVersionSupport() {
		return ByteVersionSupport.INSTANCE ;
	}
}
