package org.hibernate.usertype.internal;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hibernate.TemporalValueException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.TemporalPrecisionPlan;
import org.hibernate.MappingException;
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
	private final TemporalPrecisionPlan precision;

	public ContextualTemporalCompositeUserType(
			CompositeUserType<T> delegate, JavaType<T> javaType, ZoneId target, String role) {
		this.delegate = delegate;
		this.adjustment = target == null ? null : new TemporalAdjustmentConverter<>( javaType, target, false, role );
		this.role = role;
		this.precision = null;
	}

	private ContextualTemporalCompositeUserType(ContextualTemporalCompositeUserType<T> original, TemporalPrecisionPlan precision) {
		delegate = original.delegate;
		adjustment = original.adjustment;
		role = original.role;
		this.precision = precision;
	}

	/// Creates a mapping-owned instance after the primary column has resolved its grid.
	public ContextualTemporalCompositeUserType<T> withPrecision(TemporalPrecisionPlan precision) {
		return new ContextualTemporalCompositeUserType<>( this, precision );
	}

	private T canonical(T value) {
		final T adjusted = adjust( value );
		if ( adjusted == null || precision == null ) {
			return adjusted;
		}
		final Object primary = precision.prepare( delegate.getPropertyValue( adjusted, 0 ) );
		final Object offset = delegate.getPropertyValue( adjusted, 1 );
		return adjust( delegate.instantiate( () -> new Object[] { primary, offset } ) );
	}

	/// Mapping role retained for bootstrap and execution diagnostics.
	public String getRole() {
		return role;
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
		final Serializable payload = delegate.disassemble( canonical( value ) );
		return precision == null || value == null ? payload : new PrecisionCacheValue( precision.getSignature(), payload );
	}

	@Override
	public T assemble(Serializable cached, Object owner) {
		if ( precision != null && cached != null ) {
			if ( !(cached instanceof PrecisionCacheValue value) || !precision.getSignature().equals( value.signature() ) ) {
				throw new MappingException( "Incompatible temporal COLUMN cache payload for '" + role
						+ "'; evict affected cache regions before changing precision" );
			}
			cached = ((PrecisionCacheValue) cached).value();
		}
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
	private record PrecisionCacheValue(String signature, Serializable value) implements Serializable {
	}

}
