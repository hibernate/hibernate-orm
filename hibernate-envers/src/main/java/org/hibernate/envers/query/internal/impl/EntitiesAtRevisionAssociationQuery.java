/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;

import jakarta.persistence.criteria.JoinType;

/**
 * An {@link AuditAssociationQuery} implementation for
 * {@link EntitiesAtRevisionQuery} and {@link EntitiesModifiedAtRevisionQuery} query types.
 *
 * @author Chris Cranford
 */
@Incubating
public class EntitiesAtRevisionAssociationQuery<Q extends AuditQueryImplementor> extends AbstractAuditAssociationQuery<Q> {

	public EntitiesAtRevisionAssociationQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Q parent,
			QueryBuilder queryBuilder,
			String propertyName,
			JoinType joinType,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String ownerAlias,
			String userSuppliedAlias,
			AuditCriterion onClauseCriterion) {
		super(
				enversService,
				auditReader,
				parent,
				queryBuilder,
				propertyName,
				joinType,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				ownerAlias,
				userSuppliedAlias,
				onClauseCriterion
		);
	}

	@Override
	protected AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> createAssociationQuery(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClause) {
		return new EntitiesAtRevisionAssociationQuery<>(
				enversService,
				auditReader,
				this,
				queryBuilder,
				associationName,
				joinType,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				this.alias,
				alias,
				onClauseCriterion
		);
	}

	@Override
	protected Parameters createEntityJoin(Configuration configuration) {
		Parameters onClauseParameters = super.createEntityJoin( configuration );

		if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
			final String originalIdPropertyName = configuration.getOriginalIdPropertyName();
			final String revisionPropertyPath = configuration.getRevisionNumberPath();

			// filter revision of target entity
			Parameters parametersToUse = parameters;
			if ( joinType == JoinType.LEFT ) {
				parametersToUse = parameters.addSubParameters( Parameters.OR );
				parametersToUse.addNullRestriction( revisionPropertyPath, true );
				parametersToUse = parametersToUse.addSubParameters( Parameters.AND );
			}
			MiddleIdData referencedIdData = new MiddleIdData(
					configuration,
					enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(),
					null,
					entityName,
					true
			);
			enversService.getAuditStrategy().addEntityAtRevisionRestriction(
					configuration,
					queryBuilder,
					parametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					referencedIdData,
					revisionPropertyPath,
					originalIdPropertyName,
					alias,
					queryBuilder.generateAlias(),
					true
			);
		}

		return onClauseParameters;
	}
}
