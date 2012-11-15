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
package org.hibernate.cache.spi.entry;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * {@link CacheEntry} structure, used for construct / deconstruct the cache entry to different format that store in the 2LC.
 *
 * @author Gavin King
 */
public interface CacheEntryStructure<S,T> {
	/**
	 * Convert the giving {@param source} to the target format of {@link T}.
	 * <br>
	 *     The generic type of {@link S} should be either of :
	 *     <ul>
	 *         <li>{@link CacheEntry}</li>
	 *         <li>{@link CollectionCacheEntry}</li>
	 *     </ul>
	 * </br>
	 *
	 * This is called just before cache entry being stored into 2LC.
	 *
	 * @param source The raw cache entry.
	 * @return The target type of value that being persisted into 2LC.
	 */
	public T structure(S source);

	/**
	 * Deconstruct the {@param target} that load from 2LC to its source type of {@link S}.
	 *
	 * @param target The item that load from the 2LC.
	 * @param factory The SessionFactoryImplementor.
	 * @return The source type of cache entry.
	 */
	public S destructure(T target, SessionFactoryImplementor factory);
}
