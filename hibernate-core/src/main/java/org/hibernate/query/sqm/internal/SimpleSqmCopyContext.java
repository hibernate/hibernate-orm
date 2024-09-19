/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.IdentityHashMap;

import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Marco Belladelli
 */
public class SimpleSqmCopyContext implements SqmCopyContext {
	private final IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
	private final @Nullable SqmQuerySource querySource;

	public SimpleSqmCopyContext() {
		this( null );
	}

	public SimpleSqmCopyContext(@Nullable SqmQuerySource querySource) {
		this.querySource = querySource;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> @Nullable T getCopy(T original) {
		return (T) map.get( original );
	}

	@Override
	public <T> T registerCopy(T original, T copy) {
		final Object old = map.put( original, copy );
		if ( old != null ) {
			throw new IllegalArgumentException( "Already registered a copy: " + old );
		}
		return copy;
	}

	@Override
	public @Nullable SqmQuerySource getQuerySource() {
		return querySource;
	}
}
