/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static java.util.Collections.emptyList;

/**
 * Tracks non-nullable transient entities that would cause a particular entity insert to fail.
 *
 * @author Gail Badner
 */
public final class NonNullableTransientDependencies {
	// Multiple property paths can refer to the same transient entity, so use Set<String>
	// for the map value.
	private Map<Object,Set<String>> propertyPathsByTransientEntity; // lazily initialized

	public void add(String propertyName, Object transientEntity) {
		getPropertyPaths( transientEntity ).add( propertyName );
	}

	private Set<String> getPropertyPaths(Object transientEntity) {
		if ( propertyPathsByTransientEntity == null ) {
			propertyPathsByTransientEntity = new IdentityHashMap<>();
		}
		Set<String> propertyPaths = propertyPathsByTransientEntity.get( transientEntity );
		if ( propertyPaths == null ) {
			propertyPaths = new HashSet<>();
			propertyPathsByTransientEntity.put( transientEntity, propertyPaths );
		}
		return propertyPaths;
	}

	public Iterable<Object> getNonNullableTransientEntities() {
		return propertyPathsByTransientEntity == null
				? emptyList()
				: propertyPathsByTransientEntity.keySet();
	}

	/**
	 * Retrieve the paths that refer to the transient entity
	 *
	 * @param entity The transient entity
	 *
	 * @return The property paths
	 */
	public Iterable<String> getNonNullableTransientPropertyPaths(final Object entity) {
		return propertyPathsByTransientEntity == null
				? emptyList()
				: propertyPathsByTransientEntity.get( entity );
	}

	/**
	 * Are there any paths currently tracked here?
	 *
	 * @return {@code true} indicates there are no path tracked here currently
	 */
	public boolean isEmpty() {
		return propertyPathsByTransientEntity == null
			|| propertyPathsByTransientEntity.isEmpty();
	}

	/**
	 * Clean up any tracked references for the given entity, throwing an exception if there were any paths.
	 *
	 * @param entity The entity
	 *
	 * @throws IllegalStateException If the entity had tracked paths
	 */
	public void resolveNonNullableTransientEntity(Object entity) {
		if ( propertyPathsByTransientEntity != null
				&& propertyPathsByTransientEntity.remove( entity ) == null ) {
			throw new IllegalStateException( "Attempt to resolve a non-nullable, transient entity that is not a dependency" );
		}
	}

	/**
	 * Build a loggable representation of the paths tracked here at the moment.
	 *
	 * @param session The session (used to resolve entity names)
	 *
	 * @return The loggable representation
	 */
	public String toLoggableString(SharedSessionContractImplementor session) {
		final StringBuilder result =
				new StringBuilder( getClass().getSimpleName() ).append( '[' );
		if ( propertyPathsByTransientEntity != null ) {
			for ( var entry : propertyPathsByTransientEntity.entrySet() ) {
				result.append( "transientEntityName=" )
						.append( session.bestGuessEntityName( entry.getKey() ) );
				result.append( " requiredBy=" )
						.append( entry.getValue() );
			}
		}
		result.append( ']' );
		return result.toString();
	}
}
