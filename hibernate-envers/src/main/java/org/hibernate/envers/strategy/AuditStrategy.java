/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.configuration.internal.GlobalConfiguration;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

/**
 * Behaviours of different audit strategy for populating audit data.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface AuditStrategy {
	/**
	 * Perform the persistence of audited data for regular entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens
	 * @param enversService The EnversService
	 * @param id Id of the entity.
	 * @param data Audit data to persist
	 * @param revision Current revision data
	 * @deprecated (since 5.2.1), use {@link #perform(Session, String, AuditEntitiesConfiguration, Serializable, Object, Object)}
	 */
	@Deprecated
	default void perform(
			Session session,
			String entityName,
			EnversService enversService,
			Serializable id,
			Object data,
			Object revision) {
		perform(
				session,
				entityName,
				enversService.getAuditEntitiesConfiguration(),
				id,
				data,
				revision
		);
	}

	/**
	 * Perform the persistence of audited data for regular entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens
	 * @param auditEntitiesConfiguration The audit entity configuration.
	 * @param id Id of the entity.
	 * @param data Audit data to persist.
	 * @param revision Current revision data.
	 */
	void perform(
			Session session,
			String entityName,
			AuditEntitiesConfiguration auditEntitiesConfiguration,
			Serializable id,
			Object data,
			Object revision);

	/**
	 * Perform the persistence of audited data for collection ("middle") entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens.
	 * @param propertyName The name of the property holding the persistent collection
	 * @param enversService The EnversService
	 * @param persistentCollectionChangeData Collection change data to be persisted.
	 * @param revision Current revision data
	 * @deprecated (since 5.2.1), use {@link #performCollectionChange(Session, String, String, AuditEntitiesConfiguration, PersistentCollectionChangeData, Object)}
	 */
	@Deprecated
	default void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			EnversService enversService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		performCollectionChange(
				session,
				entityName,
				propertyName,
				enversService.getAuditEntitiesConfiguration(),
				persistentCollectionChangeData,
				revision
		);
	}

	/**
	 * Perform the persistence of audited data for collection ("middle") entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens.
	 * @param propertyName The name of the property holding the persistent collection
	 * @param auditEntitiesConfiguration audit entity configuration
	 * @param persistentCollectionChangeData Collection change data to be persisted.
	 * @param revision Current revision data
	 */
	void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			AuditEntitiesConfiguration auditEntitiesConfiguration,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision);

	/**
	 * Update the rootQueryBuilder with an extra WHERE clause to restrict the revision for a two-entity relation.
	 * This WHERE clause depends on the AuditStrategy, as follows:
	 * <ul>
	 * <li>For {@link DefaultAuditStrategy} a subquery is created:
	 * <p><code>e.revision = (SELECT max(...) ...)</code></p>
	 * </li>
	 * <li>for {@link ValidityAuditStrategy} the revision-end column is used:
	 * <p><code>e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null)</code></p>
	 * </li>
	 * </ul>
	 *
	 * @param globalCfg the {@link GlobalConfiguration}
	 * @param rootQueryBuilder the {@link QueryBuilder} that will be updated
	 * @param parameters root parameters to which restrictions shall be added
	 * @param revisionProperty property of the revision column
	 * @param revisionEndProperty property of the revisionEnd column (only used for {@link ValidityAuditStrategy})
	 * @param addAlias {@code boolean} indicator if a left alias is needed
	 * @param idData id-information for the two-entity relation (only used for {@link DefaultAuditStrategy})
	 * @param revisionPropertyPath path of the revision property (only used for {@link ValidityAuditStrategy})
	 * @param originalIdPropertyName name of the id property (only used for {@link ValidityAuditStrategy})
	 * @param alias1 an alias used for subquery (only used for {@link ValidityAuditStrategy})
	 * @param alias2 an alias used for subquery (only used for {@link ValidityAuditStrategy})
	 * @param inclusive indicates whether revision number shall be treated as inclusive or exclusive
	 */
	void addEntityAtRevisionRestriction(
			GlobalConfiguration globalCfg,
			QueryBuilder rootQueryBuilder,
			Parameters parameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData idData,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			String alias2,
			boolean inclusive);

	/**
	 * Update the rootQueryBuilder with an extra WHERE clause to restrict the revision for a middle-entity
	 * association. This WHERE clause depends on the AuditStrategy, as follows:
	 * <ul>
	 * <li>For {@link DefaultAuditStrategy} a subquery is created:
	 * <p><code>e.revision = (SELECT max(...) ...)</code></p>
	 * </li>
	 * <li>for {@link ValidityAuditStrategy} the revision-end column is used:
	 * <p><code>e.revision <= :revision and (e.endRevision > :revision or e.endRevision is null)</code></p>
	 * </li>
	 * </ul>
	 *
	 * @param rootQueryBuilder the {@link QueryBuilder} that will be updated
	 * @param parameters root parameters to which restrictions shall be added
	 * @param revisionProperty property of the revision column
	 * @param revisionEndProperty property of the revisionEnd column (only used for {@link ValidityAuditStrategy})
	 * @param addAlias {@code boolean} indicator if a left alias is needed
	 * @param referencingIdData id-information for the middle-entity association (only used for {@link DefaultAuditStrategy})
	 * @param versionsMiddleEntityName name of the middle-entity
	 * @param eeOriginalIdPropertyPath name of the id property (only used for {@link ValidityAuditStrategy})
	 * @param revisionPropertyPath path of the revision property (only used for {@link ValidityAuditStrategy})
	 * @param originalIdPropertyName name of the id property (only used for {@link ValidityAuditStrategy})
	 * @param alias1 an alias used for subqueries (only used for {@link DefaultAuditStrategy})
	 * @param inclusive indicates whether revision number shall be treated as inclusive or exclusive
	 * @param componentDatas information about the middle-entity relation
	 */
	void addAssociationAtRevisionRestriction(
			QueryBuilder rootQueryBuilder,
			Parameters parameters,
			String revisionProperty,
			String revisionEndProperty,
			boolean addAlias,
			MiddleIdData referencingIdData,
			String versionsMiddleEntityName,
			String eeOriginalIdPropertyPath,
			String revisionPropertyPath,
			String originalIdPropertyName,
			String alias1,
			boolean inclusive,
			MiddleComponentData... componentDatas);
}
