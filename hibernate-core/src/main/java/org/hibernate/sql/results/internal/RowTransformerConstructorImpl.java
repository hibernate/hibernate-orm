/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import jakarta.annotation.Nullable;

import jakarta.persistence.TupleElement;
import org.hibernate.InstantiationException;
import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.spi.SqmExpressible;
import org.hibernate.query.sqm.tree.spi.SqmExpressibleAccessor;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteralNull;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.spi.TypeConfiguration;

import java.lang.reflect.Constructor;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hibernate.sql.results.graph.instantiation.internal.InstantiationHelper.findMatchingConstructor;

/**
 * {@link RowTransformer} instantiating an arbitrary class
 *
 * @author Gavin King
 */
public class RowTransformerConstructorImpl<T> implements RowTransformer<T> {
	private final Class<T> type;
	private final Constructor<T> constructor;

	public RowTransformerConstructorImpl(
			Class<T> type,
			TupleMetadata tupleMetadata,
			TypeConfiguration typeConfiguration) {
		this.type = type;
		assert tupleMetadata != null : "TupleMetadata must not be null";
		final List<TupleElement<?>> elements = tupleMetadata.getList();
		final List<Class<?>> argumentTypes = elements.stream()
				.map( RowTransformerConstructorImpl::resolveElementJavaType )
				.collect( toList() );
		if ( argumentTypes.contains( null )
				|| argumentTypes.size() == 1 && argumentTypes.get( 0 ) == Void.class ) {
			// A single untyped null must remain a scalar result instead of selecting an arbitrary constructor.
			throw new InstantiationException( "Cannot instantiate query result type, argument types are unknown ", type );
		}

		constructor = findMatchingConstructor( type, argumentTypes, typeConfiguration );
		if ( constructor == null ) {
			throw new InstantiationException( "Cannot instantiate query result type, found no matching constructor", type );
		}
		constructor.setAccessible( true );
	}

	@Nullable
	private static Class<?> resolveElementJavaType(TupleElement<?> element) {
		if ( element instanceof SqmExpressibleAccessor<?> accessor ) {
			final SqmExpressible<?> expressible = accessor.getExpressible();
			if ( expressible != null && expressible.getExpressibleJavaType() != null ) {
				return expressible.getExpressibleJavaType().getJavaTypeClass();
			}
		}

		final var javaType = element instanceof JpaTupleElement<?> sqmElement
				? sqmElement.getJavaTypeIfKnown()
				: element.getJavaType();
		// An untyped null literal is compatible with any reference parameter type.
		return javaType == null && element instanceof SqmLiteralNull<?> ? Void.class : javaType;
	}

	@Override
	public T transformRow(Object[] row) {
		try {
			return constructor.newInstance( row );
		}
		catch (Exception e) {
			throw new InstantiationException( "Cannot instantiate query result type", type, e );
		}
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
