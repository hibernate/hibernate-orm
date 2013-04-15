/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

import java.util.Collection;

/**
 * A unifying interface for queries which can define tables (query spaces) to synchronize on.
 *
 * These query spaces affect the process of auto-flushing by determining which entities will be
 * processed by auto-flush based on the table to which those entities are mapped and which are
 * determined to have pending state changes.
 *
 * In a similar manner, these query spaces also affect how query result caching can recognize invalidated results.
 *
 * @author Steve Ebersole
 */
public interface SynchronizeableQuery {
	/**
	 * Obtain the list of query spaces the query is synchronized on.
	 *
	 * @return The list of query spaces upon which the query is synchronized.
	 */
	public Collection<String> getSynchronizedQuerySpaces();

	/**
	 * Adds a query space.
	 *
	 * @param querySpace The query space to be auto-flushed for this query.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SynchronizeableQuery addSynchronizedQuerySpace(String querySpace);

	/**
	 * Adds an entity name for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityName The name of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given name could not be resolved as an entity
	 */
	public SynchronizeableQuery addSynchronizedEntityName(String entityName) throws MappingException;

	/**
	 * Adds an entity for (a) auto-flush checking and (b) query result cache invalidation checking.  Same as
	 * {@link #addSynchronizedQuerySpace} for all tables associated with the given entity.
	 *
	 * @param entityClass The class of the entity upon whose defined query spaces we should additionally synchronize.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @throws MappingException Indicates the given class could not be resolved as an entity
	 */
	public SynchronizeableQuery addSynchronizedEntityClass(Class entityClass) throws MappingException;
}
