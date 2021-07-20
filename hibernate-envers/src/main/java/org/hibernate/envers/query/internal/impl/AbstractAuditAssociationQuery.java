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
import java.util.Locale;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.RelationDescription;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

import static org.hibernate.envers.query.criteria.internal.CriteriaTools.getRelatedEntity;

/**
 * Abstract base class for all {@link AuditAssociationQuery} implementations.
 *
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
	protected final IdMapper ownerAssociationIdMapper;
	protected final String ownerAlias;
	protected final String alias;
	protected final Map<String, String> aliasToEntityNameMap;
	protected final List<AuditCriterion> criterions = new ArrayList<>();
	protected final Parameters parameters;
	protected final List<AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>>> associationQueries = new ArrayList<>();
	protected final Map<String, AbstractAuditAssociationQuery<AbstractAuditAssociationQuery<Q>>> associationQueryMap = new HashMap<>();

	public AbstractAuditAssociationQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Q parent,
			QueryBuilder queryBuilder,
			String propertyName,
			JoinType joinType,
			Map<String, String> aliasToEntityNameMap,
			String ownerAlias,
			String userSuppliedAlias) {
		this.enversService = enversService;
		this.auditReader = auditReader;
		this.parent = parent;
		this.queryBuilder = queryBuilder;
		this.joinType = joinType;

		final String ownerEntityName = aliasToEntityNameMap.get( ownerAlias );
		final RelationDescription relationDescription = getRelatedEntity( enversService, ownerEntityName, propertyName );
		if ( relationDescription == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Property %s of entity %s is not a valid association for queries.",
							propertyName,
							ownerEntityName
					)
			);
		}

		this.entityName = relationDescription.getToEntityName();
		this.ownerAssociationIdMapper = relationDescription.getIdMapper();
		this.ownerAlias = ownerAlias;
		this.alias = StringTools.defaultIfNull( userSuppliedAlias, queryBuilder.generateAlias() );

		aliasToEntityNameMap.put( this.alias, entityName );
		this.aliasToEntityNameMap = aliasToEntityNameMap;

		this.parameters = queryBuilder.addParameters( this.alias );
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
	public AuditAssociationQuery<? extends AuditAssociationQuery<Q>> traverseRelation(
			String associationName,
			JoinType joinType) {
		return traverseRelation( associationName, joinType, null );
	}

	@Override
	public AuditAssociationQuery<Q> add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> addProjection(AuditProjection projection) {
		AuditProjection.ProjectionData projectionData = projection.getData( enversService );

		final String projectionEntityAlias = projectionData.getAlias( alias );
		final String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		final String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				projectionEntityName,
				projectionData.getPropertyName() );

		queryBuilder.addProjection(
				projectionData.getFunction(),
				projectionEntityAlias,
				propertyName,
				projectionData.isDistinct() );

		registerProjection( projectionEntityName, projection );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> addOrder(AuditOrder order) {
		AuditOrder.OrderData orderData = order.getData( enversService );

		final String orderEntityAlias = orderData.getAlias( alias );
		final String orderEntityName = aliasToEntityNameMap.get( orderEntityAlias );
		final String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				auditReader,
				orderEntityName,
				orderData.getPropertyName() );

		queryBuilder.addOrder( orderEntityAlias, propertyName, orderData.isAscending() );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setMaxResults(int maxResults) {
		parent.setMaxResults( maxResults );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setFirstResult(int firstResult) {
		parent.setFirstResult( firstResult );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setCacheable(boolean cacheable) {
		parent.setCacheable( cacheable );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setCacheRegion(String cacheRegion) {
		parent.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setComment(String comment) {
		parent.setComment( comment );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setFlushMode(FlushMode flushMode) {
		parent.setFlushMode( flushMode );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setCacheMode(CacheMode cacheMode) {
		parent.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setTimeout(int timeout) {
		parent.setTimeout( timeout) ;
		return this;
	}

	@Override
	public AuditAssociationQuery<Q> setLockMode(LockMode lockMode) {
		parent.setLockMode( lockMode );
		return this;
	}

	@Override
	public Q up() {
		return parent;
	}

	@Override
	public void registerProjection(String entityName, AuditProjection projection) {
		parent.registerProjection( entityName, projection );
	}

	protected void addCriterionsToQuery(AuditReaderImplementor versionsReader) {
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

		for ( AbstractAuditAssociationQuery<?> subQuery : associationQueries ) {
			subQuery.addCriterionsToQuery( versionsReader );
		}
	}
}
