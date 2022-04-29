/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.usertype.CompositeUserType;

/**
 * CacheKeyValueDescriptor used to describe CompositeUserType mappings
 */
public class CustomComponentCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	private final NavigableRole role;
	private final CompositeUserType<Object> compositeUserType;

	public CustomComponentCacheKeyValueDescriptor(
			NavigableRole role,
			CompositeUserType<Object> compositeUserType) {
		this.role = role;
		this.compositeUserType = compositeUserType;
	}

	@Override
	public int getHashCode(Object key) {
		return compositeUserType.hashCode( key );
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		return compositeUserType.equals( key1, key2 );
	}

	@Override
	public String toString() {
		return "CustomComponentCacheKeyValueDescriptor(" + role + ")";
	}
}
