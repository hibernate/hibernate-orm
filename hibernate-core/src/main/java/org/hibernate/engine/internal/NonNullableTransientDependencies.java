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
public class NonNullableTransientDependencies {

	// Multiple property paths can refer to the same transient entity, so use Set<String>
	// for the map value.
	private final Map<Object,Set<String>> propertyPathsByTransientEntity =
			new IdentityHashMap<Object,Set<String>>();

	/* package-protected */
	void add(String propertyName, Object transientEntity) {
		Set<String> propertyPaths = propertyPathsByTransientEntity.get( transientEntity );
		if ( propertyPaths == null ) {
			propertyPaths = new HashSet<String>();
			propertyPathsByTransientEntity.put( transientEntity, propertyPaths );
		}
		propertyPaths.add( propertyName );
	}

	public Iterable<Object> getNonNullableTransientEntities() {
		return propertyPathsByTransientEntity.keySet();
	}

	public Iterable<String> getNonNullableTransientPropertyPaths(Object entity) {
		return propertyPathsByTransientEntity.get( entity );
	}

	public boolean isEmpty() {
		return propertyPathsByTransientEntity.isEmpty();
	}

	public void resolveNonNullableTransientEntity(Object entity) {
		if ( propertyPathsByTransientEntity.remove( entity ) == null ) {
			throw new IllegalStateException( "Attempt to resolve a non-nullable, transient entity that is not a dependency." );
		}
	}

	public String toLoggableString(SessionImplementor session) {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( '[' );
		for ( Map.Entry<Object,Set<String>> entry : propertyPathsByTransientEntity.entrySet() ) {
			sb.append( "transientEntityName=" ).append( session.bestGuessEntityName( entry.getKey() ) );
			sb.append( " requiredBy=" ).append( entry.getValue() );
		}
		sb.append( ']' );
		return sb.toString();
	}
}
