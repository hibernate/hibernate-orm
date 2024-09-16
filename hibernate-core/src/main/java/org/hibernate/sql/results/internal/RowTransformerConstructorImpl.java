/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import jakarta.persistence.TupleElement;
import org.hibernate.InstantiationException;
import org.hibernate.sql.results.spi.RowTransformer;

import java.lang.reflect.Constructor;
import java.util.List;

import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmExpressibleAccessor;

/**
 * {@link RowTransformer} instantiating an arbitrary class
 *
 * @author Gavin King
 */
public class RowTransformerConstructorImpl<T> implements RowTransformer<T> {
	private final Class<T> type;
	private final Constructor<T> constructor;

	public RowTransformerConstructorImpl(Class<T> type, TupleMetadata tupleMetadata) {
		this.type = type;
		final List<TupleElement<?>> elements = tupleMetadata.getList();
		final Class<?>[] sig = new Class[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			sig[i] = resolveElementJavaType( elements.get( i ) );
		}
		try {
			constructor = type.getDeclaredConstructor( sig );
			constructor.setAccessible( true );
		}
		catch (Exception e) {
			//TODO try again with primitive types
			throw new InstantiationException( "Cannot instantiate query result type ", type, e );
		}
	}

	private static Class<?> resolveElementJavaType(TupleElement<?> element) {
		if ( element instanceof SqmExpressibleAccessor ) {
			final SqmExpressible<?> expressible = ( (SqmExpressibleAccessor<?>) element ).getExpressible();
			if ( expressible != null && expressible.getExpressibleJavaType() != null ) {
				return expressible.getExpressibleJavaType().getJavaTypeClass();
			}
		}

		return element.getJavaType();
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
