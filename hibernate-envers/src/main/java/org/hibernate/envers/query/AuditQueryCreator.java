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
 */
package org.hibernate.envers.query;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.query.internal.impl.EntitiesAtRevisionQuery;
import org.hibernate.envers.query.internal.impl.EntitiesModifiedAtRevisionQuery;
import org.hibernate.envers.query.internal.impl.RevisionsOfEntityQuery;

import static org.hibernate.envers.internal.tools.ArgumentsTools.checkNotNull;
import static org.hibernate.envers.internal.tools.ArgumentsTools.checkPositive;
import static org.hibernate.envers.internal.tools.EntityTools.getTargetClassIfProxied;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AuditQueryCreator {
	private final AuditConfiguration auditCfg;
	private final AuditReaderImplementor auditReaderImplementor;

	public AuditQueryCreator(AuditConfiguration auditCfg, AuditReaderImplementor auditReaderImplementor) {
		this.auditCfg = auditCfg;
		this.auditReaderImplementor = auditReaderImplementor;
	}

	/**
	 * Creates a query, which will return entities satisfying some conditions (specified later),
	 * at a given revision. Deleted entities are not included.
	 *
	 * @param c Class of the entities for which to query.
	 * @param revision Revision number at which to execute the query.
	 *
	 * @return A query for entities at a given revision, to which conditions can be added and which
	 *         can then be executed. The result of the query will be a list of entities (beans), unless a
	 *         projection is added.
	 */
	public AuditQuery forEntitiesAtRevision(Class<?> c, Number revision) {
		checkNotNull( revision, "Entity revision" );
		checkPositive( revision, "Entity revision" );
		c = getTargetClassIfProxied( c );
		return new EntitiesAtRevisionQuery( auditCfg, auditReaderImplementor, c, revision, false );
	}

	/**
	 * Creates a query, which will return entities satisfying some conditions (specified later),
	 * at a given revision and a given entityName. Deleted entities are not included.
	 *
	 * @param c Class of the entities for which to query.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code c}).
	 * @param revision Revision number at which to execute the query.
	 *
	 * @return A query for entities at a given revision, to which conditions can be added and which
	 *         can then be executed. The result of the query will be a list of entities (beans), unless a
	 *         projection is added.
	 */
	public AuditQuery forEntitiesAtRevision(Class<?> c, String entityName, Number revision) {
		return forEntitiesAtRevision( c, entityName, revision, false );
	}

	/**
	 * Creates a query, which will return entities satisfying some conditions (specified later),
	 * at a given revision and a given entityName. Deleted entities may be optionally
	 * included.
	 *
	 * @param c Class of the entities for which to query.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code c}).
	 * @param revision Revision number at which to execute the query.
	 * @param includeDeletions Whether to include deleted entities in the search.
	 *
	 * @return A query for entities at a given revision, to which conditions can be added and which
	 *         can then be executed. The result of the query will be a list of entities (beans), unless a
	 *         projection is added.
	 */
	public AuditQuery forEntitiesAtRevision(Class<?> c, String entityName, Number revision, boolean includeDeletions) {
		checkNotNull( revision, "Entity revision" );
		checkPositive( revision, "Entity revision" );
		c = getTargetClassIfProxied( c );
		return new EntitiesAtRevisionQuery(
				auditCfg,
				auditReaderImplementor,
				c,
				entityName,
				revision,
				includeDeletions
		);
	}

	/**
	 * Creates a query, which will return entities modified at the specified revision.
	 * <p/>
	 * In comparison, the {@link #forEntitiesAtRevision(Class, String, Number)} query takes into all entities
	 * which were present at a given revision, even if they were not modified.
	 *
	 * @param c Class of the entities for which to query.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code c}).
	 * @param revision Revision number at which to execute the query.
	 *
	 * @return A query for entities changed at a given revision, to which conditions can be added and which
	 *         can then be executed.
	 *
	 * @see #forEntitiesAtRevision(Class, String, Number)
	 */
	public AuditQuery forEntitiesModifiedAtRevision(Class<?> c, String entityName, Number revision) {
		checkNotNull( revision, "Entity revision" );
		checkPositive( revision, "Entity revision" );
		c = getTargetClassIfProxied( c );
		return new EntitiesModifiedAtRevisionQuery( auditCfg, auditReaderImplementor, c, entityName, revision );
	}

	/**
	 * Creates a query, which will return entities modified at the specified revision.
	 * <p/>
	 * In comparison, the {@link #forEntitiesAtRevision(Class, String, Number)} query takes into all entities
	 * which were present at a given revision, even if they were not modified.
	 *
	 * @param c Class of the entities for which to query.
	 * @param revision Revision number at which to execute the query.
	 *
	 * @return A query for entities changed at a given revision, to which conditions can be added and which
	 *         can then be executed.
	 *
	 * @see #forEntitiesAtRevision(Class, Number)
	 */
	public AuditQuery forEntitiesModifiedAtRevision(Class<?> c, Number revision) {
		checkNotNull( revision, "Entity revision" );
		checkPositive( revision, "Entity revision" );
		c = getTargetClassIfProxied( c );
		return new EntitiesModifiedAtRevisionQuery( auditCfg, auditReaderImplementor, c, revision );
	}

	/**
	 * Creates a query, which selects the revisions, at which the given entity was modified.
	 * Unless an explicit projection is set, the result will be a list of three-element arrays, containing:
	 * <ol>
	 * <li>the entity instance</li>
	 * <li>revision entity, corresponding to the revision at which the entity was modified. If no custom
	 * revision entity is used, this will be an instance of {@link org.hibernate.envers.DefaultRevisionEntity}</li>
	 * <li>type of the revision (an enum instance of class {@link org.hibernate.envers.RevisionType})</li>.
	 * </ol>
	 * Additional conditions that the results must satisfy may be specified.
	 *
	 * @param c Class of the entities for which to query.
	 * @param selectEntitiesOnly If true, instead of a list of three-element arrays, a list of entites will be
	 * returned as a result of executing this query.
	 * @param selectDeletedEntities If true, also revisions where entities were deleted will be returned. The additional
	 * entities will have revision type "delete", and contain no data (all fields null), except for the id field.
	 *
	 * @return A query for revisions at which instances of the given entity were modified, to which
	 *         conditions can be added (for example - a specific id of an entity of class <code>c</code>), and which
	 *         can then be executed. The results of the query will be sorted in ascending order by the revision number,
	 *         unless an order or projection is added.
	 */
	public AuditQuery forRevisionsOfEntity(Class<?> c, boolean selectEntitiesOnly, boolean selectDeletedEntities) {
		c = getTargetClassIfProxied( c );
		return new RevisionsOfEntityQuery(
				auditCfg,
				auditReaderImplementor,
				c,
				selectEntitiesOnly,
				selectDeletedEntities
		);
	}

	/**
	 * Creates a query, which selects the revisions, at which the given entity was modified and with a given entityName.
	 * Unless an explicit projection is set, the result will be a list of three-element arrays, containing:
	 * <ol>
	 * <li>the entity instance</li>
	 * <li>revision entity, corresponding to the revision at which the entity was modified. If no custom
	 * revision entity is used, this will be an instance of {@link org.hibernate.envers.DefaultRevisionEntity}</li>
	 * <li>type of the revision (an enum instance of class {@link org.hibernate.envers.RevisionType})</li>.
	 * </ol>
	 * Additional conditions that the results must satisfy may be specified.
	 *
	 * @param c Class of the entities for which to query.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code c}).
	 * @param selectEntitiesOnly If true, instead of a list of three-element arrays, a list of entites will be
	 * returned as a result of executing this query.
	 * @param selectDeletedEntities If true, also revisions where entities were deleted will be returned. The additional
	 * entities will have revision type "delete", and contain no data (all fields null), except for the id field.
	 *
	 * @return A query for revisions at which instances of the given entity were modified, to which
	 *         conditions can be added (for example - a specific id of an entity of class <code>c</code>), and which
	 *         can then be executed. The results of the query will be sorted in ascending order by the revision number,
	 *         unless an order or projection is added.
	 */
	public AuditQuery forRevisionsOfEntity(
			Class<?> c,
			String entityName,
			boolean selectEntitiesOnly,
			boolean selectDeletedEntities) {
		c = getTargetClassIfProxied( c );
		return new RevisionsOfEntityQuery(
				auditCfg,
				auditReaderImplementor,
				c,
				entityName,
				selectEntitiesOnly,
				selectDeletedEntities
		);
	}
}
