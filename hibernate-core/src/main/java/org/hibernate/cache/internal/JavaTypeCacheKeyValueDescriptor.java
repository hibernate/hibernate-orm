/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.type.descriptor.java.JavaType;

public class JavaTypeCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	private final JavaType javaType;

	public JavaTypeCacheKeyValueDescriptor(JavaType javaType) {
		this.javaType = javaType;
	}

	@Override
	public int getHashCode(Object key) {
		return javaType.hashCode();
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		return javaType.areEqual( key1, key2 );
	}
}
