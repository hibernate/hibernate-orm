/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.ComponentDescription;
import org.hibernate.envers.internal.entities.ComponentDescription.ComponentType;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.RelationType;
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
 * An abstract base class for all {@link AuditAssociationQuery} implementations.
 *
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 * @author Chris Cranford
 */
@Incubating
public abstract class AbstractAuditAssociationQuery<Q extends AuditQueryImplementor>
		implements AuditAssociationQuery<Q>, AuditQueryImplementor {

	protected final EnversService enversService;
	protected final AuditReaderImplementor auditReader;
	protected final Q parent;
	protected final QueryBuilder queryBuilder;
	protected final JoinType joinType;
	protected final String entityName;
	protected final RelationDescription relationDescription;
	protected final ComponentDescription componentDescription;
	protected final String ownerAlias;
	protected final String ownerEntityName;
	protected final String alias;
	protected final Map<String, String> aliasToEntityNameMap;
	protected final Map<String, String> aliasToComponentPropertyNameMap;
	protected final List<AuditCriterion> criterions = new ArrayList<>();
	protected final AuditCriterion onClauseCriterion;
	protected final Parameters parameters;

	// todo: can these association query collections be merged?
	protected final List<AbstractAuditAssociationQuery<Q>> associationQueries = new ArrayList<>();
	protected final Map<String, AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>>> associationQueryMap = new HashMap<>();

	public AbstractAuditAssociationQuery(
			final EnversService enversService,
			final AuditReaderImplementor auditReader,
			final Q parent,
			final QueryBuilder queryBuilder,
			final String propertyName,
			final JoinType joinType,
			final Map<String, String> aliasToEntityNameMap,
			final Map<String, String> aliasToComponentPropertyNameMap,
			final String ownerAlias,
			final String userSuppliedAlias,
			final AuditCriterion onClauseCriterion) {
		this.enversService = enversService;
		this.auditReader = auditReader;
		this.parent = parent;
		this.queryBuilder = queryBuilder;
		this.joinType = joinType;

		ownerEntityName = aliasToEntityNameMap.get( ownerAlias );
		this.ownerAlias = ownerAlias;
		this.alias = userSuppliedAlias == null ? queryBuilder.generateAlias() : userSuppliedAlias;

		String componentPrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				ownerAlias
		);
		String prefixedPropertyName = componentPrefix.concat( propertyName );

		relationDescription = CriteriaTools.getRelatedEntity(
				enversService,
				ownerEntityName,
				prefixedPropertyName
		);

		componentDescription = CriteriaTools.getComponent( enversService, ownerEntityName, prefixedPropertyName );

		if ( relationDescription == null && componentDescription == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ENGLISH,
							"Property %s of entity %s is not a valid association for queries",
							propertyName,
							ownerEntityName
					)
			);
		}

		if ( relationDescription != null ) {
			this.entityName = relationDescription.getToEntityName();
		}
		else {
			aliasToComponentPropertyNameMap.put( alias, componentDescription.getPropertyName() );
			this.entityName = ownerEntityName;
		}
		aliasToEntityNameMap.put( this.alias, entityName );
		this.aliasToEntityNameMap = aliasToEntityNameMap;
		this.aliasToComponentPropertyNameMap = aliasToComponentPropertyNameMap;
		parameters = queryBuilder.addParameters( this.alias );
		this.onClauseCriterion = onClauseCriterion;
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
	public AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> traverseRelation(
			String associationName,
			JoinType joinType) {
		return traverseRelation(
				associationName,
				joinType,
				null
		);
	}

	@Override
	public AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias) {
		return traverseRelation(
				associationName,
				joinType,
				alias,
				null
		);
	}

	public AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClause) {
		AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> query = associationQueryMap.get( associationName );
		if ( query == null ) {
			query = createAssociationQuery( associationName, joinType, alias, onClause );
			associationQueries.add( (AbstractAuditAssociationQuery<Q>) query );
			associationQueryMap.put( associationName, query );
		}
		return query;
	}

	protected abstract AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>> createAssociationQuery(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClause);

	@Override
	public AbstractAuditAssociationQuery<Q> add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> addProjection(AuditProjection projection) {
		String projectionEntityAlias = projection.getAlias( alias );
		String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		registerProjection( projectionEntityName, projection );
		projection.addProjectionToQuery(
				enversService,
				auditReader,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				alias,
				queryBuilder
		);
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> addOrder(AuditOrder order) {
		AuditOrder.OrderData orderData = order.getData( enversService.getConfig() );
		String orderEntityAlias = orderData.getAlias( alias );
		String orderEntityName = aliasToEntityNameMap.get( orderEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				orderEntityName,
				orderData.getPropertyName()
		);
		String componentPrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				orderEntityAlias
		);
		queryBuilder.addOrder(
				orderEntityAlias,
				componentPrefix.concat( propertyName ),
				orderData.isAscending(),
				orderData.getNullPrecedence()
		);
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setMaxResults(int maxResults) {
		parent.setMaxResults( maxResults );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setFirstResult(int firstResult) {
		parent.setFirstResult( firstResult );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setCacheable(boolean cacheable) {
		parent.setCacheable( cacheable );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setCacheRegion(String cacheRegion) {
		parent.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setComment(String comment) {
		parent.setComment( comment );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setFlushMode(FlushMode flushMode) {
		parent.setFlushMode( flushMode );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setCacheMode(CacheMode cacheMode) {
		parent.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setTimeout(int timeout) {
		parent.setTimeout( timeout );
		return this;
	}

	@Override
	public AbstractAuditAssociationQuery<Q> setLockMode(LockMode lockMode) {
		parent.setLockMode( lockMode );
		return this;
	}

	public Q up() {
		return parent;
	}

	protected void addCriterionToQuery(AuditReaderImplementor versionsReader) {
		Parameters onClauseParameters;
		if ( relationDescription != null ) {
			onClauseParameters = createEntityJoin( enversService.getConfig() );
		}
		else {
			onClauseParameters = createComponentJoin( enversService.getConfig() );
		}

		if ( onClauseCriterion != null ) {
			onClauseCriterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					alias,
					queryBuilder,
					onClauseParameters
			);
		}

		for ( AuditCriterion criterion : criterions ) {
			criterion.addToQuery(
					enversService,
					versionsReader,
					aliasToEntityNameMap,
					aliasToComponentPropertyNameMap,
					alias,
					queryBuilder,
					parameters
			);
		}

		for ( AbstractAuditAssociationQuery<Q> subQuery : associationQueries ) {
			subQuery.addCriterionToQuery( versionsReader );
		}
	}

	protected Parameters createEntityJoin(Configuration configuration) {
		boolean targetIsAudited = enversService.getEntitiesConfigurations().isVersioned( entityName );
		String targetEntityName = entityName;
		if ( targetIsAudited ) {
			targetEntityName = configuration.getAuditEntityName( entityName );
		}
		String originalIdPropertyName = configuration.getOriginalIdPropertyName();
		String revisionPropertyPath = configuration.getRevisionNumberPath();

		Parameters onClauseParameters;
		if ( relationDescription.getRelationType() == RelationType.TO_ONE ) {
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );
			onClauseParameters = joinConditionParameters;

			// owner.reference_id = target.originalId.id
			IdMapper idMapperTarget;
			String prefix;
			if ( targetIsAudited ) {
				idMapperTarget = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
				prefix = alias.concat( "." ).concat( originalIdPropertyName );
			}
			else {
				idMapperTarget = enversService.getEntitiesConfigurations()
						.getNotVersionEntityConfiguration( entityName )
						.getIdMapper();
				prefix = alias;
			}
			relationDescription.getIdMapper().addIdsEqualToQuery(
					joinConditionParameters,
					ownerAlias,
					idMapperTarget,
					prefix
			);
		}
		else if ( relationDescription.getRelationType() == RelationType.TO_MANY_NOT_OWNING ) {
			if ( !targetIsAudited ) {
				throw new AuditException(
						String.format(
								Locale.ENGLISH,
								"Cannot build queries for relation type %s to non audited target entities",
								relationDescription.getRelationType()
						)
				);
			}
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );
			onClauseParameters = joinConditionParameters;

			// owner.originalId.id = target.reference_id
			IdMapper idMapperOwner = enversService.getEntitiesConfigurations().get( ownerEntityName ).getIdMapper();
			String prefix = ownerAlias.concat( "." ).concat( originalIdPropertyName );
			relationDescription.getIdMapper().addIdsEqualToQuery(
					joinConditionParameters,
					alias,
					idMapperOwner,
					prefix );
		}
		else if ( relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE
				|| relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE_NOT_OWNING ) {
			if ( !targetIsAudited && relationDescription.getRelationType() == RelationType.TO_MANY_MIDDLE_NOT_OWNING ) {
				throw new AuditException(
						String.format(
								Locale.ENGLISH,
								"Cannot build queries for relation type %s to non audited target entities",
								relationDescription.getRelationType()
						)
				);
			}

			String middleEntityAlias = queryBuilder.generateAlias();

			// join middle_entity
			Parameters joinConditionParametersMiddle = queryBuilder.addJoin(
					joinType,
					relationDescription.getAuditMiddleEntityName(),
					middleEntityAlias,
					false
			);

			// join target_entity
			Parameters joinConditionParametersTarget = queryBuilder.addJoin( joinType, targetEntityName, alias, false );
			onClauseParameters = joinConditionParametersTarget;

			Parameters middleParameters = queryBuilder.addParameters( middleEntityAlias );
			String middleOriginalIdPropertyPath = middleEntityAlias + "." + originalIdPropertyName;

			// join condition: owner.reference_id = middle.id_ref_ing
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			MiddleIdData referencingIdData = relationDescription.getReferencingIdData();
			referencingIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParametersMiddle,
					middleOriginalIdPropertyPath,
					referencingIdData.getOriginalMapper(),
					ownerPrefix
			);

			// join condition: middle.id_ref_ed = target.id
			String targetPrefix = alias;
			if ( targetIsAudited ) {
				targetPrefix = alias + "." + originalIdPropertyName;
			}
			MiddleIdData referencedIdData = relationDescription.getReferencedIdData();
			referencedIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParametersTarget,
					middleOriginalIdPropertyPath,
					referencedIdData.getOriginalMapper(),
					targetPrefix
			);

			// filter revisions of middle entity
			Parameters middleParametersToUse = middleParameters;
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( revisionPropertyPath, true );
				middleParametersToUse = middleParametersToUse.addSubParameters( Parameters.AND );
			}

			enversService.getAuditStrategy().addAssociationAtRevisionRestriction(
					queryBuilder,
					middleParametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					referencingIdData,
					relationDescription.getAuditMiddleEntityName(),
					middleOriginalIdPropertyPath,
					revisionPropertyPath,
					originalIdPropertyName,
					middleEntityAlias,
					true
			);

			// filter deleted middle entities
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( configuration.getRevisionTypePropertyName(), true );
			}
			middleParametersToUse.addWhereWithParam( configuration.getRevisionTypePropertyName(), true, "!=", RevisionType.DEL );
		}
		else {
			throw new AuditException(
					String.format(
							Locale.ENGLISH,
							"Cannot build queries for relation type %s",
							relationDescription.getRelationType()
					)
			);
		}

		return onClauseParameters;
	}

	protected Parameters createComponentJoin(Configuration configuration) {
		String originalIdPropertyName = configuration.getOriginalIdPropertyName();
		String revisionPropertyPath = configuration.getRevisionNumberPath();
		Parameters onClauseParameters;
		if ( componentDescription.getType() == ComponentType.MANY ) {
			// join middle_entity
			Parameters joinConditionParameters = queryBuilder.addJoin(
					joinType,
					componentDescription.getAuditMiddleEntityName(),
					alias,
					false
			);
			onClauseParameters = joinConditionParameters;

			String middleOriginalIdPropertyPath = alias + "." + originalIdPropertyName;

			// join condition: owner.reference_id = middle.id_ref_ing
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			MiddleIdData middleIdData = componentDescription.getMiddleIdData();
			middleIdData.getPrefixedMapper().addIdsEqualToQuery(
					joinConditionParameters,
					middleOriginalIdPropertyPath,
					middleIdData.getOriginalMapper(),
					ownerPrefix
			);

			// filter revisions of middle entity
			Parameters middleParameters = queryBuilder.addParameters( alias );
			Parameters middleParametersToUse = middleParameters;
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( revisionPropertyPath, true );
				middleParametersToUse = middleParametersToUse.addSubParameters( Parameters.AND );
			}
			configuration.getAuditStrategy().addAssociationAtRevisionRestriction(
					queryBuilder,
					middleParametersToUse,
					revisionPropertyPath,
					configuration.getRevisionEndFieldName(),
					true,
					middleIdData,
					componentDescription.getAuditMiddleEntityName(),
					middleOriginalIdPropertyPath,
					revisionPropertyPath,
					originalIdPropertyName,
					alias,
					true
			);

			// filter deleted middle entities
			String middleRevTypePropertyPath = middleOriginalIdPropertyPath + "." + configuration.getRevisionTypePropertyName();
			if ( joinType == JoinType.LEFT ) {
				middleParametersToUse = middleParameters.addSubParameters( Parameters.OR );
				middleParametersToUse.addNullRestriction( middleRevTypePropertyPath, false );
			}
			middleParametersToUse.addWhereWithParam( middleRevTypePropertyPath, false, "!=", RevisionType.DEL );
		}
		else {
			// ComponentType.ONE
			/*
			 * The properties of a single component are directly mapped on the owner entity. Therefore no join would be
			 * required to access those properties (except the case an explicit on-clause has been specified). However,
			 * the user has supplied an alias and may be accessing properties of this component through that alias: If
			 * no join is generated, the 'virtual' alias has to be retranslated to the owning entity alias. To keep
			 * things simple a join on the owning entity itself is generated. The join is cheaper than other audit joins
			 * because we can join on the complete primary key (id + rev) and do not have to range filter on the target
			 * revision number.
			 */
			String targetEntityName = configuration.getAuditEntityName( entityName );
			Parameters joinConditionParameters = queryBuilder.addJoin( joinType, targetEntityName, alias, false );
			onClauseParameters = joinConditionParameters;

			// join condition: owner.reference_id = middle.id_reference_id
			String ownerPrefix = ownerAlias + "." + originalIdPropertyName;
			String middleOriginalIdPropertyPath = alias + "." + originalIdPropertyName;
			IdMapper idMapper = enversService.getEntitiesConfigurations().get( entityName ).getIdMapper();
			idMapper.addIdsEqualToQuery( joinConditionParameters, ownerPrefix, middleOriginalIdPropertyPath );

			// join condition: owner.rev=middle.rev
			joinConditionParameters.addWhere( ownerAlias, revisionPropertyPath, "=", alias, revisionPropertyPath );
		}
		return onClauseParameters;
	}

	@Override
	public void registerProjection(final String entityName, AuditProjection projection) {
		parent.registerProjection( entityName, projection );
	}

}
