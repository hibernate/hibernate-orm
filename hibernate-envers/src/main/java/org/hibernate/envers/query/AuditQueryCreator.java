/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query;

import org.hibernate.Incubating;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.NotAuditedException;
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
 * @author Chris Cranford
 */
public class AuditQueryCreator {
	private final EnversService enversService;
	private final AuditReaderImplementor auditReaderImplementor;

	public AuditQueryCreator(EnversService enversService, AuditReaderImplementor auditReaderImplementor) {
		this.enversService = enversService;
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
		checkEntityAudited( c.getName() );
		return new EntitiesAtRevisionQuery( enversService, auditReaderImplementor, c, revision, false );
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
		checkEntityAudited( entityName );
		return new EntitiesAtRevisionQuery(
				enversService,
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
		checkEntityAudited( entityName );
		return new EntitiesModifiedAtRevisionQuery( enversService, auditReaderImplementor, c, entityName, revision );
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
		checkEntityAudited( c.getName() );
		return new EntitiesModifiedAtRevisionQuery( enversService, auditReaderImplementor, c, revision );
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
	 * @param selectEntitiesOnly If true, instead of a list of three-element arrays, a list of entities will be
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
		checkEntityAudited( c.getName() );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				c,
				selectEntitiesOnly,
				selectDeletedEntities,
				false,
				false
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
	 * @param selectEntitiesOnly If true, instead of a list of three-element arrays, a list of entities will be
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
		checkEntityAudited( entityName );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				c,
				entityName,
				selectEntitiesOnly,
				selectDeletedEntities,
				false,
				false
		);
	}

	/**
	 * Creates a query that selects the revision entities associated with the specified entity.  You may also
	 * specify whether the revision entities list should include those for deletions of the entity class.
	 *
	 * @param clazz Class of the entities for which to query.
	 * @param selectDeletedEntities If true, the result will include revision entities where deletions occurred.
	 *
	 * @return A query of revision entities based on the specified entity class.  The results of the query will
	 * 		   be stored in ascending order by the revision number unless an order is specified.
	 *
	 * @since 5.3
	 */
	@Incubating
	public AuditQuery forRevisionsOfEntity(Class<?> clazz, boolean selectDeletedEntities) {
		clazz = getTargetClassIfProxied( clazz );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				clazz,
				false,
				selectDeletedEntities,
				true,
				false
		);
	}

	/**
	 * Creates a query that selects the revision entities associated with the specified entity.  You may also
	 * specify whether the revision entities list should include those for deletions of the entity class.
	 *
	 * @param clazz Class of the entities for which to query.
	 * @param entityName Name of the entity (for cases where it cannot be guessed based on class clazz).
	 * @param selectDeletedEntities If true, the result will include revision entities where deletions occurred.
	 *
	 * @return A query of revision entities based on the specified entity class.  The results of the query will
	 * 		   be stored in ascending order by the revision number unless an order is specified.
	 *
	 * @since 5.3
	 */
	@Incubating
	public AuditQuery forRevisionsOfEntity(Class<?> clazz, String entityName, boolean selectDeletedEntities) {
		clazz = getTargetClassIfProxied( clazz );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				clazz,
				entityName,
				false,
				selectDeletedEntities,
				true,
				false
		);
	}

	/**
	 * Creates a query that selects the revisions at which the given entity was modified.  Unless a
	 * projection is set, the result will be a list of 4-element arrays, containing the following:
	 * <ol>
	 *     <li>The entity instance</li>
	 *     <li>Revision entity, corresponding to the revision where the entity was modified.  If no custom
	 *     revision entity is used, this will be an instance of {@link org.hibernate.envers.DefaultRevisionEntity}.</li>
	 *     <li>The revision type, an enum of class {@link org.hibernate.envers.RevisionType}.</li>
	 *     <li>The names of the properties changed in this revision</li>
	 * </ol>
	 * Additional criterion may be specified to filter the result set.
	 *
	 * @param clazz Class of the entities for which to query.
	 * @param selectDeletedEntities If true, the result set will include revisions where entities were deleted.
	 *
	 * @return the audit query
	 *
	 * @since 5.3
	 */
	@Incubating
	public AuditQuery forRevisionsOfEntityWithChanges(Class<?> clazz, boolean selectDeletedEntities) {
		clazz = getTargetClassIfProxied( clazz );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				clazz,
				false,
				selectDeletedEntities,
				false,
				true
		);
	}

	/**
	 * Creates a query that selects the revisions at which the given entity was modified.  Unless a
	 * projection is set, the result will be a list of 4-element arrays, containing the following:
	 * <ol>
	 *     <li>The entity instance</li>
	 *     <li>Revision entity, corresponding to the revision where the entity was modified.  If no custom
	 *     revision entity is used, this will be an instance of {@link org.hibernate.envers.DefaultRevisionEntity}.</li>
	 *     <li>The revision type, an enum of class {@link org.hibernate.envers.RevisionType}.</li>
	 *     <li>The names of the properties changed in this revision</li>
	 * </ol>
	 * Additional criterion may be specified to filter the result set.
	 *
	 * @param clazz Class of the entities for which to query.
	 * @param entityName Name of the entity (if it can't be guessed basing on the {@code clazz}).
	 * @param selectDeletedEntities If true, the result set will include revisions where entities were deleted.
	 *
	 * @return the audit query
	 *
	 * @since 5.3
	 */
	@Incubating
	public AuditQuery forRevisionsOfEntityWithChanges(Class<?> clazz, String entityName, boolean selectDeletedEntities) {
		clazz = getTargetClassIfProxied( clazz );
		return new RevisionsOfEntityQuery(
				enversService,
				auditReaderImplementor,
				clazz,
				entityName,
				false,
				selectDeletedEntities,
				false,
				true
		);
	}

	private void checkEntityAudited(String entityName) {
		if ( !auditReaderImplementor.isEntityNameAudited( entityName ) ) {
			throw new NotAuditedException(
					entityName,
					"Cannot query audit history on a non-audited entity [" + entityName + "]."
			);
		}
	}
}
