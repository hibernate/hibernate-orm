/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.util.Comparator;

import org.hibernate.type.Type;

/**
 * Describes attributes regarding the type of data to be cached.
 *
 * @author Steve Ebersole
 */
public interface CacheDataDescription {
	/**
	 * Is the data marked as being mutable?
	 *
	 * @return {@code true} if the data is mutable; {@code false} otherwise.
	 */
	public boolean isMutable();

	/**
	 * Is the data to be cached considered versioned?
	 *
	 * If {@code true}, it is illegal for {@link #getVersionComparator} to return {@code null}
	 * or an instance of {@link org.hibernate.type.descriptor.java.IncomparableComparator}.
	 *
	 * @return {@code true} if the data is versioned; {@code false} otherwise.
	 */
	public boolean isVersioned();

	/**
	 * Get the comparator used to compare two different version values.  May return {@code null} <b>if</b>
	 * {@link #isVersioned()} returns false.
	 *
	 * @return The comparator for versions, or {@code null}
	 */
	public Comparator getVersionComparator();

	/**
	 * @return Type of the key that will be used as the key in the cache, or {@code null} if the natural comparison
	 * ({@link Object#hashCode()} and {@link Object#equals(Object)} methods should be used.
	 */
	Type getKeyType();
}
