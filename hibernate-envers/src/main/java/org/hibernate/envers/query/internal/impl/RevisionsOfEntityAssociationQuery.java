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
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;

/**
 * An {@link org.hibernate.envers.query.AuditAssociationQuery} implementation for {@link RevisionsOfEntityQuery}.
 *
 * @author Chris Cranford
 */
@Incubating
public class RevisionsOfEntityAssociationQuery<Q extends AuditQueryImplementor> extends AbstractAuditAssociationQuery<Q> {

	public RevisionsOfEntityAssociationQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Q parent,
			QueryBuilder queryBuilder,
			String propertyName,
			JoinType joinType,
			Map<String, String> aliasToEntityNameMap,
			String ownerAlias,
			String userSuppliedAlias) {
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
					AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> query = new RevisionsOfEntityAssociationQuery<>(
							enversService,
							auditReader,
							this,
							queryBuilder,
							name,
							joinType,
							aliasToEntityNameMap,
							this.alias,
							alias );

					associationQueries.add( query );
					return query;
				}
		);
	}

	@Override
	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
		if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
			String auditEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, auditEntityName, alias, false );

			// owner.reference_id = target.originalId.id
			AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
			final String prefix = alias + "." + verEntCfg.getOriginalIdPropName();
			addOwnerReferenceIdentifierToTargetIdentifier(
					joinConditionParameters,
					enversService.getEntitiesConfigurations().get( entityName ),
					prefix
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
