package org.hibernate.usertype.internal;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.TemporalValueException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.usertype.CompositeUserType;

/// Used to model offset and zoned temporal mappings when the offset / zone are stored as a
/// separate [COLUMN][org.hibernate.annotations.TimeZoneRepresentation.Type#COLUMN].
///
/// @see org.hibernate.annotations.TimeZoneRepresentation
///
/// @author Steve Ebersole
public final class ContextualTemporalCompositeUserType<T> implements CompositeUserType<T> {
	private final CompositeUserType<T> delegate;
	private final TemporalAdjustmentConverter<T> adjustment;
	private final String role;

	public ContextualTemporalCompositeUserType(
			CompositeUserType<T> delegate, JavaType<T> javaType, ZoneId target, String role) {
		this.delegate = delegate;
		this.adjustment = target == null ? null : new TemporalAdjustmentConverter<>( javaType, target, false, role );
		this.role = role;
	}

	private T adjust(T value) {
		return adjustment == null ? value : adjustment.toDomainValue( value );
	}

	@Override
	public Object getPropertyValue(T component, int property) {
		return component == null ? null : delegate.getPropertyValue( adjust( component ), property );
	}

	@Override
	public T instantiate(ValueAccess values) {
		final Object primary = values.getValue( 0, Object.class );
		final ZoneOffset offset = values.getValue( 1, ZoneOffset.class );
		if ( primary == null && offset == null ) {
			return null;
		}
		if ( primary == null || offset == null ) {
			throw new TemporalValueException( "Partially null temporal COLUMN tuple for '" + role + "'" );
		}
		return adjust( delegate.instantiate( values ) );
	}

	@Override
	public Class<?> embeddable() {
		return delegate.embeddable();
	}

	@Override
	public Class<T> returnedClass() {
		return delegate.returnedClass();
	}

	@Override
	public boolean equals(T x, T y) {
		return x == y || x != null && y != null && delegate.equals( x, y );
	}

	@Override
	public int hashCode(T value) {
		return value == null ? 0 : delegate.hashCode( value );
	}

	@Override
	public T deepCopy(T value) {
		return delegate.deepCopy( value );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(T value) {
		return delegate.disassemble( adjust( value ) );
	}

	@Override
	public T assemble(Serializable cached, Object owner) {
		T value = delegate.assemble( cached, owner );
		if ( value instanceof ZonedDateTime zoned ) {
			value = returnedClass().cast( ZonedDateTime.ofInstant( zoned.toInstant(), zoned.getOffset() ) );
		}
		return adjust( value );
	}

	@Override
	public T replace(T detached, T managed, Object owner) {
		return delegate.replace( detached, managed, owner );
	}
}
