/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractNumericJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.LongVersionSupport;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;

/**
 * Descriptor for {@link Integer} handling.
 *
 * @author Steve Ebersole
 */
public class LongJavaDescriptor extends AbstractNumericJavaDescriptor<Long> implements Primitive<Long> {
	public static final LongJavaDescriptor INSTANCE = new LongJavaDescriptor();

	public LongJavaDescriptor() {
		super( Long.class );
	}

	private static final Long ZERO = (long) 0;

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.BIGINT );
	}

	@Override
	public String toString(Long value) {
		return value == null ? null : value.toString();
	}

	@Override
	public Long fromString(String string) {
		return string == null ? null : Long.valueOf( string );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <X> X unwrap(Long value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) Byte.valueOf( value.byteValue() );
		}
		if ( Short.class.isAssignableFrom( type ) ) {
			return (X) Short.valueOf( value.shortValue() );
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
		if ( BigInteger.class.isAssignableFrom( type ) ) {
			return (X) BigInteger.valueOf( value );
		}
		if ( BigDecimal.class.isAssignableFrom( type ) ) {
			return (X) BigDecimal.valueOf( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Long wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( Long.class.isInstance( value ) ) {
			return (Long) value;
		}
		if ( Number.class.isInstance( value ) ) {
			return ( (Number) value ).longValue();
		}
		if ( String.class.isInstance( value ) ) {
			return Long.valueOf( ( (String) value ) );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public Class getPrimitiveClass() {
		return long.class;
	}

	@Override
	public Long getDefaultValue() {
		return ZERO;
	}

	@Override
	public VersionSupport<Long> getVersionSupport() {
		return LongVersionSupport.INSTANCE;
	}
}
