/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi;

import java.util.Comparator;

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
	 * If {@code true}, it is illegal for {@link #getVersionComparator} to return {@code null}.
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
}
