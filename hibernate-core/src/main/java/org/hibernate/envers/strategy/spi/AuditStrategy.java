/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy.spi;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.service.ServiceRegistry;

/**
 * A strategy abstraction for how to audit entity changes.
 *
 * @since 5.4
 *
 * @author Chris Cranford
 */
@Incubating(since = "5.4")
public interface AuditStrategy {

	// todo (6.0) : these methods need to change to accept ExecutionContext rather than just Session
	//		this (^^) will also allow Envers to work against StatelessSession whereas
	//		it is currently limited to just Session

	// todo (6.0) - cleanup this spi
	//		1. Introduce parameter objects to reduce parameter bloat on strategy methods
	//		2. Eliminate the use of internal objects on the SPI interface.

	/**
	 * Add additional columns to the audit mappings.
	 *
	 * @param mappingContext The mapping context.
	 */
	void addAdditionalColumns(MappingContext mappingContext);

	/**
	 * Performs post initialization of the audit strategy implementation.
	 *
	 * @param revisionInfoClass  The revision entity class.
	 * @param timestampData The timestamp property data on the revision entity class.
	 * @param serviceRegistry The service registry.
	 */
	void postInitialize(Class<?> revisionInfoClass, PropertyData timestampData, ServiceRegistry serviceRegistry);

	/**
	 * Perform the persistence of audited data for regular entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens
	 * @param auditService The AuditService
	 * @param id Id of the entity.
	 * @param data Audit data to persist.
	 * @param revision Current revision data.
	 */
	void perform(
			Session session,
			String entityName,
			AuditService auditService,
			Object id,
			Object data,
			Object revision);


	/**
	 * Perform the persistence of audited data for collection ("middle") entities.
	 *
	 * @param session Session, which can be used to persist the data.
	 * @param entityName Name of the entity, in which the audited change happens.
	 * @param propertyName The name of the property holding the persistent collection
	 * @param auditService The AuditService
	 * @param persistentCollectionChangeData Collection change data to be persisted.
	 * @param revision Current revision data
	 */
	void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			AuditService auditService,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision);

	/**
	 * Update the rootQueryBuilder with an extra WHERE clause to restrict the revision for a two-entity relation.
	 * This WHERE clause depends on the AuditStrategy.
	 *
	 * @param options the {@link AuditServiceOptions}
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
			AuditServiceOptions options,
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
	 * association. This WHERE clause depends on the AuditStrategy.
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
