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
 * Strategy for how cache entries are "structured" for storing into the cache.
 *
 * @author Gavin King
 */
public interface CacheEntryStructure {
	/**
	 * Convert the cache item into its "structured" form.  Perfectly valid to return the item as-is.
	 *
	 * @param item The item to structure.
	 *
	 * @return The structured form.
	 */
	public Object structure(Object item);

	/**
	 * Convert the previous structured form of the item back into its item form.
	 *
	 * @param structured The structured form.
	 * @param factory The session factory.
	 *
	 * @return The item
	 */
	public Object destructure(Object structured, SessionFactoryImplementor factory);
}
