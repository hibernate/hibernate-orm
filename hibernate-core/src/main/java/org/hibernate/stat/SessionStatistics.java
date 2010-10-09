/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.stat;

import java.util.Set;

/**
 * Information about the first-level (session) cache
 * for a particular session instance
 * @author Gavin King
 */
public interface SessionStatistics {

	/**
	 * Get the number of entity instances associated with the session
	 */
	public int getEntityCount();
	/**
	 * Get the number of collection instances associated with the session
	 */
	public int getCollectionCount();

	/**
	 * Get the set of all <tt>EntityKey</tt>s
	 * @see org.hibernate.engine.EntityKey
	 */
	public Set getEntityKeys();
	/**
	 * Get the set of all <tt>CollectionKey</tt>s
	 * @see org.hibernate.engine.CollectionKey
	 */
	public Set getCollectionKeys();
	
}
