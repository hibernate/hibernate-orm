package org.hibernate.usertype.internal;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

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
	private final ZoneId target;

	public ContextualTemporalCompositeUserType(
			CompositeUserType<T> delegate, JavaType<T> javaType, ZoneId target, String role) {
		this.delegate = delegate;
		this.target = target;
		this.adjustment = target == null ? null : new TemporalAdjustmentConverter<>( javaType, target, false, role );
		this.role = role;
		this.precision = null;
	}

	private ContextualTemporalCompositeUserType(ContextualTemporalCompositeUserType<T> original, TemporalPrecisionPlan precision) {
		delegate = original.delegate;
		target = original.target;
		adjustment = original.adjustment;
		role = original.role;
		this.precision = precision;
	}

	/// Creates a mapping-owned instance after the primary column has resolved its grid.
	public ContextualTemporalCompositeUserType<T> withPrecision(TemporalPrecisionPlan precision) {
		return new ContextualTemporalCompositeUserType<>( this, precision );
	}

	public boolean hasPrecision() {
		return precision != null;
	}

	/// A database-owned tuple copy cannot recompute a region offset after rounding.
	/// Identical storage contracts need no such recomputation. Unadjusted tuples
	/// retain their existing SQL assignment semantics.
	public boolean canCopySqlTupleFrom(ContextualTemporalCompositeUserType<?> source) {
		return target == null || source != null && (signature().equals( source.signature() )
				|| target.equals( source.target ) && target.getRules().isFixedOffset());
	}

	/// Execution pair and grid for query-assignment diagnostics; contains no values.
	public String getSqlTupleContractDescription() {
		final var context = precision.getContext();
		return context.javaType().getJavaTypeClass().getTypeName() + "/" + context.jdbcType().getClass().getName()
				+ ", " + context.ddlTypeName() + ", precision=" + context.precision() + ", adjustment=" + target;
	}

	/// Prepare the primary and companion together. Ordinary property access stays
	/// unprepared because predicates and identity bindings also use decomposition.
	@SuppressWarnings("unchecked")
	public Object[] prepareTuple(Object value) {
		final T adjusted = adjust( (T) value );
		if ( adjusted == null ) {
			return new Object[] { null, null };
		}
		final Object primary = precision.prepare( delegate.getPropertyValue( adjusted, 0 ) );
		final Object offset = delegate.getPropertyValue( adjusted, 1 );
		final T reconstructed = adjust( delegate.instantiate( () -> new Object[] { primary, offset } ) );
		return new Object[] { delegate.getPropertyValue( reconstructed, 0 ), delegate.getPropertyValue( reconstructed, 1 ) };
	}

	/// Compare precision only. The source offset/zone remain significant even when
	/// explicit adjustment would store both assignments using the same target.
	@SuppressWarnings("unchecked")
	public boolean precisionEqual(Object first, Object second) {
		if ( first == second ) { return true; }
		if ( first == null || second == null ) { return false; }
		final T left = (T) first;
		final T right = (T) second;
		return Objects.equals( delegate.getPropertyValue( left, 1 ), delegate.getPropertyValue( right, 1 ) )
				&& (!(left instanceof ZonedDateTime zoned) || zoned.getZone().equals( ((ZonedDateTime) right).getZone() ))
				&& precision.precisionEqual( delegate.getPropertyValue( left, 0 ), delegate.getPropertyValue( right, 0 ) );
	}

	/// Serialize an actual prepared/extracted tuple, without preparing a domain
	/// value again. The target participates in compatibility independently of the
	/// primary mapping, whose plan may not itself carry the outer adjustment.
	public Serializable disassembleTuple(Object[] tuple) {
		final T reconstructed = instantiate( () -> tuple );
		return new PrecisionCacheValue( signature(), delegate.disassemble( reconstructed ) );
	}

	private String signature() {
		return "column-v2|" + returnedClass().getName() + '|' + target + '|' + precision.getSignature();
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
		if ( precision != null ) {
			precision.validateExtracted( primary );
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
		return precision == null || value == null ? delegate.disassemble( adjust( value ) )
				: disassembleTuple( prepareTuple( value ) );
	}

	@Override
	public T assemble(Serializable cached, Object owner) {
		if ( precision != null && cached != null ) {
			if ( !(cached instanceof PrecisionCacheValue value) || !signature().equals( value.signature() ) ) {
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
