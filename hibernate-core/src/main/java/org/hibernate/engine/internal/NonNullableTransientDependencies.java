/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Tracks non-nullable transient entities that would cause a particular entity insert to fail.
 *
 * @author Gail Badner
 */
public final class NonNullableTransientDependencies {
	// Multiple property paths can refer to the same transient entity, so use Set<String>
	// for the map value.
	private Map<Object,Set<String>> propertyPathsByTransientEntity; // lazily initialized

	void add(String propertyName, Object transientEntity) {
		if ( propertyPathsByTransientEntity == null ) {
			propertyPathsByTransientEntity = new IdentityHashMap<Object, Set<String>>();
		}
		Set<String> propertyPaths = propertyPathsByTransientEntity.get( transientEntity );
		if ( propertyPaths == null ) {
			propertyPaths = new HashSet<String>();
			propertyPathsByTransientEntity.put( transientEntity, propertyPaths );
		}
		propertyPaths.add( propertyName );
	}

	public Iterable<Object> getNonNullableTransientEntities() {
		if ( propertyPathsByTransientEntity == null ) {
			return Collections.emptyList();
		}
		else {
			return propertyPathsByTransientEntity.keySet();
		}
	}

	/**
	 * Retrieve the paths that refer to the transient entity
	 *
	 * @param entity The transient entity
	 *
	 * @return The property paths
	 */
	public Iterable<String> getNonNullableTransientPropertyPaths(final Object entity) {
		if ( propertyPathsByTransientEntity == null ) {
			return Collections.emptyList();
		}
		else {
			return propertyPathsByTransientEntity.get( entity );
		}
	}

	/**
	 * Are there any paths currently tracked here?
	 *
	 * @return {@code true} indicates there are no path tracked here currently
	 */
	public boolean isEmpty() {
		return propertyPathsByTransientEntity == null || propertyPathsByTransientEntity.isEmpty();
	}

	/**
	 * Clean up any tracked references for the given entity, throwing an exception if there were any paths.
	 *
	 * @param entity The entity
	 *
	 * @throws IllegalStateException If the entity had tracked paths
	 */
	public void resolveNonNullableTransientEntity(Object entity) {
		if ( propertyPathsByTransientEntity != null && propertyPathsByTransientEntity.remove( entity ) == null ) {
			throw new IllegalStateException( "Attempt to resolve a non-nullable, transient entity that is not a dependency." );
		}
	}

	/**
	 * Build a loggable representation of the paths tracked here at the moment.
	 *
	 * @param session The session (used to resolve entity names)
	 *
	 * @return The loggable representation
	 */
	public String toLoggableString(SessionImplementor session) {
		final StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( '[' );
		if ( propertyPathsByTransientEntity != null ) {
			for ( Map.Entry<Object,Set<String>> entry : propertyPathsByTransientEntity.entrySet() ) {
				sb.append( "transientEntityName=" ).append( session.bestGuessEntityName( entry.getKey() ) );
				sb.append( " requiredBy=" ).append( entry.getValue() );
			}
		}
		sb.append( ']' );
		return sb.toString();
	}
}
