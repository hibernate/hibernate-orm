/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.entities.mapper.relation.MiddleIdData;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Incubating
public class AuditAssociationQueryImpl<Q extends AuditQueryImplementor>
		implements AuditAssociationQuery<Q>, AuditQueryImplementor {

	private final EnversService enversService;
	private final AuditReaderImplementor auditReader;
	private final Q parent;
	private final QueryBuilder queryBuilder;
	private final JoinType joinType;
	private final String entityName;
	private final IdMapper ownerAssociationIdMapper;
	private final String ownerAlias;
	private final String alias;
	private final Map<String, String> aliasToEntityNameMap;
	private final List<AuditCriterion> criterions = new ArrayList<>();
	private final Parameters parameters;
	private final List<AuditAssociationQueryImpl<?>> associationQueries = new ArrayList<>();
	private final Map<String, AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>>> associationQueryMap = new HashMap<>();

	public AuditAssociationQueryImpl(
			final EnversService enversService,
			final AuditReaderImplementor auditReader,
			final Q parent,
			final QueryBuilder queryBuilder,
			final String propertyName,
			final JoinType joinType,
			final Map<String, String> aliasToEntityNameMap,
			final String ownerAlias,
			final String userSuppliedAlias) {
		this.enversService = enversService;
		this.auditReader = auditReader;
		this.parent = parent;
		this.queryBuilder = queryBuilder;
		this.joinType = joinType;

		String ownerEntityName = aliasToEntityNameMap.get( ownerAlias );
		final RelationDescription relationDescription = CriteriaTools.getRelatedEntity(
				enversService,
				ownerEntityName,
				propertyName
		);
		if ( relationDescription == null ) {
			throw new IllegalArgumentException( "Property " + propertyName + " of entity " + ownerEntityName + " is not a valid association for queries" );
		}
		this.entityName = relationDescription.getToEntityName();
		this.ownerAssociationIdMapper = relationDescription.getIdMapper();
		this.ownerAlias = ownerAlias;
		this.alias = userSuppliedAlias == null ? queryBuilder.generateAlias() : userSuppliedAlias;
		aliasToEntityNameMap.put( this.alias, entityName );
		this.aliasToEntityNameMap = aliasToEntityNameMap;
		parameters = queryBuilder.addParameters( this.alias );
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public List getResultList() throws AuditException {
		return parent.getResultList();
	}

	@Override
	public Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException {
		return parent.getSingleResult();
	}

	@Override
	public AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> traverseRelation(
			String associationName,
			JoinType joinType) {
		return traverseRelation(
				associationName,
				joinType,
				null
		);
	}

	@Override
	public AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias) {
		AuditAssociationQueryImpl<AuditAssociationQueryImpl<Q>> result = associationQueryMap.get( associationName );
		if ( result == null ) {
			result = new AuditAssociationQueryImpl<>(
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
			associationQueries.add( result );
			associationQueryMap.put( associationName, result );
		}
		return result;
	}

	@Override
	public AuditAssociationQueryImpl<Q> add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> addProjection(AuditProjection projection) {
		AuditProjection.ProjectionData projectionData = projection.getData( enversService );
		String projectionEntityAlias = projectionData.getAlias( alias );
		String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				projectionEntityName,
				projectionData.getPropertyName()
		);
		queryBuilder.addProjection(
				projectionData.getFunction(),
				projectionEntityAlias,
				propertyName,
				projectionData.isDistinct()
		);
		registerProjection( projectionEntityName, projection );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> addOrder(AuditOrder order) {
		AuditOrder.OrderData orderData = order.getData( enversService );
		String orderEntityAlias = orderData.getAlias( alias );
		String orderEntityName = aliasToEntityNameMap.get( orderEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				orderEntityName,
				orderData.getPropertyName()
		);
		queryBuilder.addOrder( orderEntityAlias, propertyName, orderData.isAscending() );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setMaxResults(int maxResults) {
		parent.setMaxResults( maxResults );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setFirstResult(int firstResult) {
		parent.setFirstResult( firstResult );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheable(boolean cacheable) {
		parent.setCacheable( cacheable );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheRegion(String cacheRegion) {
		parent.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setComment(String comment) {
		parent.setComment( comment );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setFlushMode(FlushMode flushMode) {
		parent.setFlushMode( flushMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setCacheMode(CacheMode cacheMode) {
		parent.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setTimeout(int timeout) {
		parent.setTimeout( timeout );
		return this;
	}

	@Override
	public AuditAssociationQueryImpl<Q> setLockMode(LockMode lockMode) {
		parent.setLockMode( lockMode );
		return this;
	}

	public Q up() {
		return parent;
	}

	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
		if ( enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
			String auditEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, auditEntityName, alias, false );

			// owner.reference_id = target.originalId.id
			AuditEntitiesConfiguration verEntCfg = enversService.getAuditEntitiesConfiguration();
			String originalIdPropertyName = verEntCfg.getOriginalIdPropName();
			IdMapper idMapperTarget = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
			final String prefix = alias.concat( "." ).concat( originalIdPropertyName );
			ownerAssociationIdMapper.addIdsEqualToQuery(
					joinConditionParameters,
					ownerAlias,
					idMapperTarget,
					prefix
			);

			// filter revision of target entity
			Parameters parametersToUse = parameters;
			String revisionPropertyPath = verEntCfg.getRevisionNumberPath();
			if (joinType == JoinType.LEFT) {
				parametersToUse = parameters.addSubParameters( Parameters.OR );
				parametersToUse.addNullRestriction( revisionPropertyPath, true );
				parametersToUse = parametersToUse.addSubParameters( Parameters.AND );
			}
			MiddleIdData referencedIdData = new MiddleIdData(
					verEntCfg,
					enversService.getEntitiesConfigurations().get( entityName ).getIdMappingData(),
					null,
					entityName,
					enversService.getEntitiesConfigurations().isVersioned( entityName )
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
			final IdMapper idMapperTarget = enversService.getEntitiesConfigurations()
					.getNotVersionEntityConfiguration( entityName )
					.getIdMapper();
			ownerAssociationIdMapper.addIdsEqualToQuery(
					joinConditionParameters,
					ownerAlias,
					idMapperTarget,
					alias
			);
		}

		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					alias,
					queryBuilder,
					parameters 
			);
		}

		for ( final AuditAssociationQueryImpl<?> sub : associationQueries ) {
			sub.addCriterionsToQuery( versionsReader );
		}

	}

	@Override
	public void registerProjection(final String entityName, AuditProjection projection) {
		parent.registerProjection( entityName, projection );
	}

}
