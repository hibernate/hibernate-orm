/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.Map;
import javax.persistence.criteria.JoinType;

import org.hibernate.Incubating;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;

/**
 * An {@link org.hibernate.envers.query.AuditAssociationQuery} implementation for {@link EntitiesAtRevisionQuery}
 * and {@link EntitiesModifiedAtRevisionQuery} parent query types.
 *
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 * @author Chris Cranford
 */
@Incubating
public class EntitiesAtRevisionAssociationQuery<Q extends AuditQueryImplementor> extends AbstractAuditAssociationQuery<Q> {

	public EntitiesAtRevisionAssociationQuery(
			final EnversService enversService,
			final AuditReaderImplementor auditReader,
			final Q parent,
			final QueryBuilder queryBuilder,
			final String propertyName,
			final JoinType joinType,
			final Map<String, String> aliasToEntityNameMap,
			final String ownerAlias,
			final String userSuppliedAlias) {
		super( enversService,
			auditReader,
			parent,
			queryBuilder,
			propertyName,
			joinType,
			aliasToEntityNameMap,
			ownerAlias,
			userSuppliedAlias );
	}

	@Override
	public AuditAssociationQuery<? extends AuditAssociationQuery<Q>> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias) {
		return associationQueryMap.computeIfAbsent(
				associationName,
				name -> {
					AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> query = new EntitiesAtRevisionAssociationQuery<>(
							enversService,
							auditReader,
							this,
							queryBuilder,
							associationName,
							joinType,
							aliasToEntityNameMap,
							this.alias,
							alias
					);

					associationQueries.add( query );
					return query;
				}
		);
	}

	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
		if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
			final EntityConfiguration entityConfig = enversService.getEntitiesConfigurations().get( entityName );

			String auditEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, auditEntityName, alias, false );

			// owner.reference_id = target.originalId.id
			AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
			final String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
			final String prefix = alias + "." + originalIdPropertyName;

			addOwnerReferenceIdentifierToTargetIdentifier(
					joinConditionParameters,
					entityConfig,
					prefix
			);

			// filter revision of target entity
			Parameters parametersToUse = parameters;
			String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
			if ( joinType == JoinType.LEFT ) {
				parametersToUse = parameters.addSubParameters( Parameters.OR );
				parametersToUse.addNullRestriction( revisionPropertyPath, true );
				parametersToUse = parametersToUse.addSubParameters( Parameters.AND );
			}

			MiddleIdData referencedIdData = new MiddleIdData(
					verEntCfg,
					entityConfig.getIdMappingData(),
					null,
					entityName,
					true
			);

			enversService.getAuditStrategy().addEntityAtRevisionRestriction(
					enversService.getGlobalConfiguration(),
					queryBuilder,
					parametersToUse,
					revisionPropertyPath,
					verEntCfg.getRevisionEndFieldName(),
					true,
					referencedIdData,
					revisionPropertyPath,
					originalIdPropertyName,
					alias,
					queryBuilder.generateAlias(),
					true
			);
		}
		else {
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, entityName, alias, false );
			// owner.reference_id = target.id
			addOwnerReferenceIdentifierToTargetIdentifier(
					joinConditionParameters,
					enversService.getEntitiesConfigurations().getNotVersionEntityConfiguration( entityName ),
					alias
			);
		}

		super.addCriterionsToQuery( versionsReader );
	}

	private void addOwnerReferenceIdentifierToTargetIdentifier(Parameters params, EntityConfiguration entityConfig, String prefix) {
		final IdMapper idMapperTarget = entityConfig.getIdMapper();
		ownerAssociationIdMapper.addIdsEqualToQuery( params, ownerAlias, idMapperTarget, prefix );
	}
}
