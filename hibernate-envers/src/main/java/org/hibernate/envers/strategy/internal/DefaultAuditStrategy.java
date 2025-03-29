/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.strategy.internal;

import org.hibernate.Session;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleComponentData;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.synchronization.SessionCacheCleaner;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.strategy.AuditStrategy;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.MIDDLE_ENTITY_ALIAS_DEF_AUD_STR;
import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REVISION_PARAMETER;

/**
 * Default implementation of the {@link org.hibernate.envers.strategy.spi.AuditStrategy} contract.
 * <p>
 * This strategy handles the persistence of audit entity data in a very simplistic way.
 *
 * @author Adam Warski
 * @author Stephanie Pau
 * @author Chris Cranford
 */
public class DefaultAuditStrategy implements AuditStrategy {
	private final SessionCacheCleaner sessionCacheCleaner;

	public DefaultAuditStrategy() {
		sessionCacheCleaner = new SessionCacheCleaner();
	}

	@Override
	public void perform(
			Session session,
			String entityName,
			Configuration configuration,
			Object id,
			Object data,
			Object revision) {
		session.persist( configuration.getAuditEntityName( entityName ), data );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, data );
	}

	@Override
	public void performCollectionChange(
			Session session,
			String entityName,
			String propertyName,
			Configuration configuration,
			PersistentCollectionChangeData persistentCollectionChangeData,
			Object revision) {
		session.persist( persistentCollectionChangeData.getEntityName(), persistentCollectionChangeData.getData() );
		sessionCacheCleaner.scheduleAuditDataRemoval( session, persistentCollectionChangeData.getData() );
	}

	/**
	 * {@inheritDoc}
	 *
	 * For this implementation, a subquery is used
	 * <p>
	 * {@code e.revision = (SELECT max(...)  ...}
	 */
	@Override
	public void addEntityAtRevisionRestriction(
			Configuration configuration,
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
			boolean inclusive) {
		// create a subquery builder
		// SELECT max(e.revision) FROM versionsReferencedEntity e2
		QueryBuilder maxERevQb = rootQueryBuilder.newSubQueryBuilder( idData.getAuditEntityName(), alias2 );
		maxERevQb.addProjection( "max", alias2, revisionPropertyPath, false );
		// WHERE
		Parameters maxERevQbParameters = maxERevQb.getRootParameters();
		// e2.revision <= :revision
		maxERevQbParameters.addWhereWithNamedParam( revisionPropertyPath, inclusive ? "<=" : "<", REVISION_PARAMETER );
		// e2.id_ref_ed = e.id_ref_ed
		idData.getOriginalMapper().addIdsEqualToQuery(
				maxERevQbParameters,
				alias1 + "." + originalIdPropertyName, alias2 + "." + originalIdPropertyName
		);

		// add subquery to rootParameters
		String subqueryOperator = configuration.getCorrelatedSubqueryOperator();
		parameters.addWhere( revisionProperty, addAlias, subqueryOperator, maxERevQb );
	}

	/**
	 * {@inheritDoc}
	 *
	 * For this implmeentation, the a subquery is used
	 * <p>
	 * {@code e.revision = (SELECT max(...) ...}
	 */
	@Override
	public void addAssociationAtRevisionRestriction(
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
			MiddleComponentData... componentDatas) {
		// SELECT max(ee2.revision) FROM middleEntity ee2
		QueryBuilder maxEeRevQb = rootQueryBuilder.newSubQueryBuilder(
				versionsMiddleEntityName,
				MIDDLE_ENTITY_ALIAS_DEF_AUD_STR
		);
		maxEeRevQb.addProjection( "max", MIDDLE_ENTITY_ALIAS_DEF_AUD_STR, revisionPropertyPath, false );
		// WHERE
		Parameters maxEeRevQbParameters = maxEeRevQb.getRootParameters();
		// ee2.revision <= :revision
		maxEeRevQbParameters.addWhereWithNamedParam( revisionPropertyPath, inclusive ? "<=" : "<", REVISION_PARAMETER );
		// ee2.originalId.* = ee.originalId.*
		String ee2OriginalIdPropertyPath = MIDDLE_ENTITY_ALIAS_DEF_AUD_STR + "." + originalIdPropertyName;
		referencingIdData.getPrefixedMapper().addIdsEqualToQuery(
				maxEeRevQbParameters,
				eeOriginalIdPropertyPath,
				ee2OriginalIdPropertyPath
		);
		for ( MiddleComponentData componentData : componentDatas ) {
			componentData.getComponentMapper().addMiddleEqualToQuery(
					maxEeRevQbParameters,
					eeOriginalIdPropertyPath,
					alias1,
					ee2OriginalIdPropertyPath,
					MIDDLE_ENTITY_ALIAS_DEF_AUD_STR
			);
		}

		// add subquery to rootParameters
		parameters.addWhere( revisionProperty, addAlias, "=", maxEeRevQb );
	}
}
