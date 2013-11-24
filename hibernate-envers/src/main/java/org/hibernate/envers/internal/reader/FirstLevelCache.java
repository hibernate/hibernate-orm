/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.reader;

import java.util.Map;

import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.tools.Triple;

import org.jboss.logging.Logger;

import static org.hibernate.envers.internal.tools.Tools.newHashMap;
import static org.hibernate.envers.internal.tools.Triple.make;

/**
 * First level cache for versioned entities, versions reader-scoped. Each entity is uniquely identified by a
 * revision number and entity id.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 */
public class FirstLevelCache {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			FirstLevelCache.class.getName()
	);

	/**
	 * cache for resolve an object for a given id, revision and entityName.
	 */
	private final Map<Triple<String, Number, Object>, Object> cache;

	/**
	 * used to resolve the entityName for a given id, revision and entity.
	 */
	private final Map<Triple<Object, Number, Object>, String> entityNameCache;

	public FirstLevelCache() {
		cache = newHashMap();
		entityNameCache = newHashMap();
	}

	public Object get(String entityName, Number revision, Object id) {
		LOG.debugf(
				"Resolving object from First Level Cache: EntityName:%s - primaryKey:%s - revision:%s",
				entityName,
				id,
				revision
		);
		return cache.get( make( entityName, revision, id ) );
	}

	public void put(String entityName, Number revision, Object id, Object entity) {
		LOG.debugf(
				"Caching entity on First Level Cache:  - primaryKey:%s - revision:%s - entityName:%s",
				id,
				revision,
				entityName
		);
		cache.put( make( entityName, revision, id ), entity );
	}

	public boolean contains(String entityName, Number revision, Object id) {
		return cache.containsKey( make( entityName, revision, id ) );
	}

	/**
	 * Adds the entityName into the cache. The key is a triple make with primaryKey, revision and entity
	 *
	 * @param id primaryKey
	 * @param revision revision number
	 * @param entity object retrieved by envers
	 * @param entityName value of the cache
	 */
	public void putOnEntityNameCache(Object id, Number revision, Object entity, String entityName) {
		LOG.debugf(
				"Caching entityName on First Level Cache:  - primaryKey:%s - revision:%s - entity:%s -> entityName:%s",
				id,
				revision,
				entity.getClass().getName(),
				entityName
		);
		entityNameCache.put( make( id, revision, entity ), entityName );
	}

	/**
	 * Gets the entityName from the cache. The key is a triple make with primaryKey, revision and entity
	 *
	 * @param id primaryKey
	 * @param revision revision number
	 * @param entity object retrieved by envers
	 *
	 * @return The appropriate entity name
	 */
	public String getFromEntityNameCache(Object id, Number revision, Object entity) {
		LOG.debugf(
				"Trying to resolve entityName from First Level Cache: - primaryKey:%s - revision:%s - entity:%s",
				id,
				revision,
				entity
		);
		return entityNameCache.get( make( id, revision, entity ) );
	}

	/**
	 * @param id primaryKey
	 * @param revision revision number
	 * @param entity object retrieved by envers
	 *
	 * @return true if entityNameCache contains the triple
	 */
	public boolean containsEntityName(Object id, Number revision, Object entity) {
		return entityNameCache.containsKey( make( id, revision, entity ) );
	}
}
