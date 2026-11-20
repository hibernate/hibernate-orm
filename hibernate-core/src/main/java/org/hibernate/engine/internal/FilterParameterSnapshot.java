package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.type.descriptor.java.JavaType;

/** Copies scalar and list filter arguments using their mapped Java type. */
public record FilterParameterSnapshot(
		String filterName, String parameterName, JavaType<Object> javaType, Object value, boolean plural) {

	@SuppressWarnings("unchecked")
	public static FilterParameterSnapshot capture(String filterName, String parameterName, JavaType<?> type, Object value) {
		final var javaType = (JavaType<Object>) type;
		final boolean plural = isPlural( value, javaType );
		final Object snapshot;
		if ( plural ) {
			final List<Object> elements = new ArrayList<>();
			for ( Object element : (Iterable<?>) value ) {
				elements.add( javaType.getMutabilityPlan().deepCopy( element ) );
			}
			snapshot = elements;
		}
		else {
			snapshot = javaType.getMutabilityPlan().deepCopy( value );
		}
		return new FilterParameterSnapshot( filterName, parameterName, javaType, snapshot, plural );
	}

	public boolean matches(Object current) {
		if ( plural != isPlural( current, javaType ) ) {
			return false;
		}
		if ( !plural ) {
			return javaType.areEqual( value, current );
		}
		final var previous = ((Iterable<?>) value).iterator();
		final var next = ((Iterable<?>) current).iterator();
		while ( previous.hasNext() && next.hasNext() ) {
			if ( !javaType.areEqual( previous.next(), next.next() ) ) {
				return false;
			}
		}
		return !previous.hasNext() && !next.hasNext();
	}

	private static boolean isPlural(Object value, JavaType<?> javaType) {
		return value instanceof Iterable<?> && !javaType.isInstance( value );
	}
}
