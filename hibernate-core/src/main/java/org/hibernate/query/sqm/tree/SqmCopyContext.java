/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.IdentityHashMap;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 *
 */
public interface SqmCopyContext {

	<T> T getCopy(T original);

	<T> T registerCopy(T original, T copy);

	static SqmCopyContext simpleContext() {
		final IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
		return new SqmCopyContext() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T getCopy(T original) {
				if (original instanceof SqmPath) {
					return (T) getPathCopy( (SqmPath<?>) original );
				}
				else {
					return (T) map.get( original );
				}
			}

			@Override
			public <T> T registerCopy(T original, T copy) {
				final Object old = map.put( original, copy );
				if ( old != null ) {
					throw new IllegalArgumentException( "Already registered a copy: " + old );
				}
				return copy;
			}

			@SuppressWarnings("unchecked")
			private <T extends SqmPath<?>> T getPathCopy(T original) {
				T existing = (T) map.get( original );
				if ( existing != null ) {
					return existing;
				}

				SqmPath<?> root = getRoot( original );
				if ( root != original ) {
					root.copy( this );
					// root path might have already copied original
					return (T) map.get( original );
				}
				else {
					return null;
				}
			}

			private SqmPath<?> getRoot(SqmPath<?> path) {
				if ( path.getLhs() != null ) {
					return getRoot( path.getLhs() );
				}
				else {
					return path;
				}
			}
		};
	}

	static SqmCopyContext noParamCopyContext() {
		final IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
		return new SqmCopyContext() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T getCopy(T original) {
				if ( original instanceof SqmParameter ) {
					return original;
				}
				if (original instanceof SqmPath) {
					return (T) getPathCopy( (SqmPath<?>) original );
				}
				else {
					return (T) map.get( original );
				}
			}

			@Override
			public <T> T registerCopy(T original, T copy) {
				final Object old = map.put( original, copy );
				if ( old != null ) {
					throw new IllegalArgumentException( "Already registered a copy: " + old );
				}
				return copy;
			}

			@SuppressWarnings("unchecked")
			private <T extends SqmPath<?>> T getPathCopy(T original) {
				T existing = (T) map.get( original );
				if ( existing != null ) {
					return existing;
				}

				SqmPath<?> root = getRoot( original );
				if ( root != original ) {
					root.copy( this );
					// root path might have already copied original
					return (T) map.get( original );
				}
				else {
					return null;
				}
			}

			private SqmPath<?> getRoot(SqmPath<?> path) {
				if ( path.getLhs() != null ) {
					return getRoot( path.getLhs() );
				}
				else {
					return path;
				}
			}
		};
	}
}
