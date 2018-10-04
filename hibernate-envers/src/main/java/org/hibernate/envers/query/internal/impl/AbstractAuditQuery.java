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
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.internal.entities.EntityConfiguration;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditAssociationQuery;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.tools.Pair;
import org.hibernate.query.Query;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 */
public abstract class AbstractAuditQuery implements AuditQueryImplementor {
	protected EntityInstantiator entityInstantiator;
	protected List<AuditCriterion> criterions;

	protected String entityName;
	protected String entityClassName;
	protected String versionsEntityName;
	protected QueryBuilder qb;
	protected final Map<String, String> aliasToEntityNameMap = new HashMap<>();

	protected boolean hasOrder;

	protected final EnversService enversService;
	protected final AuditReaderImplementor versionsReader;

	protected final List<AuditAssociationQueryImpl<?>> associationQueries = new ArrayList<>();
	protected final Map<String, AuditAssociationQueryImpl<AuditQueryImplementor>> associationQueryMap = new HashMap<>();
	protected final List<Pair<String, AuditProjection>> projections = new ArrayList<>();

	protected AbstractAuditQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls) {
		this( enversService, versionsReader, cls, cls.getName() );
	}

	protected AbstractAuditQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Class<?> cls,
			String entityName) {
		this.enversService = enversService;
		this.versionsReader = versionsReader;

		criterions = new ArrayList<>();
		entityInstantiator = new EntityInstantiator( enversService, versionsReader );

		entityClassName = cls.getName();
		this.entityName = entityName;
		versionsEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );
		if ( !enversService.getEntitiesConfigurations().isVersioned( entityName ) ) {
			throw new NotAuditedException( entityName, "Entity [" + entityName + "] is not versioned" );
		}
		aliasToEntityNameMap.put( REFERENCED_ENTITY_ALIAS, entityName );

		qb = new QueryBuilder( versionsEntityName, REFERENCED_ENTITY_ALIAS, versionsReader.getSessionImplementor().getFactory() );
	}

	@Override
	public String getAlias() {
		return REFERENCED_ENTITY_ALIAS;
	}
	
	protected Query buildQuery() {
		Query query = qb.toQuery( versionsReader.getSessionImplementor() );
		setQueryProperties( query );
		return query;
	}

	protected List buildAndExecuteQuery() {
		Query query = buildQuery();

		return query.list();
	}

	public abstract List list() throws AuditException;

	public List getResultList() throws AuditException {
		return list();
	}

	public Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException {
		List result = list();

		if ( result == null || result.size() == 0 ) {
			throw new NoResultException();
		}

		if ( result.size() > 1 ) {
			throw new NonUniqueResultException();
		}

		return result.get( 0 );
	}

	public AuditQuery add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	// Projection and order

	public AuditQuery addProjection(AuditProjection projection) {
		AuditProjection.ProjectionData projectionData = projection.getData( enversService );
		String projectionEntityAlias = projectionData.getAlias( REFERENCED_ENTITY_ALIAS );
		String projectionEntityName = aliasToEntityNameMap.get( projectionEntityAlias );
		registerProjection( projectionEntityName, projection );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				projectionEntityName,
				projectionData.getPropertyName()
		);
		qb.addProjection(
				projectionData.getFunction(),
				projectionEntityAlias,
				propertyName,
				projectionData.isDistinct()
		);
		return this;
	}

	@Override
	public void registerProjection(String entityName, AuditProjection projection) {
		projections.add( Pair.make( entityName, projection ) );
	}

	protected boolean hasProjection() {
		return !projections.isEmpty();
	}

	public AuditQuery addOrder(AuditOrder order) {
		hasOrder = true;
		AuditOrder.OrderData orderData = order.getData( enversService );
		String orderEntityAlias = orderData.getAlias( REFERENCED_ENTITY_ALIAS );
		String orderEntityName = aliasToEntityNameMap.get( orderEntityAlias );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				orderEntityName,
				orderData.getPropertyName()
		);
		qb.addOrder( orderEntityAlias, propertyName, orderData.isAscending() );
		return this;
	}

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType) {
		return traverseRelation(
				associationName,
				joinType,
				null
		);
	}

	@Override
	public AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType, String alias) {
		AuditAssociationQueryImpl<AuditQueryImplementor> result = associationQueryMap.get( associationName );
		if (result == null) {
			result = new AuditAssociationQueryImpl<>(
					enversService,
					versionsReader,
					this,
					qb,
					associationName,
					joinType,
					aliasToEntityNameMap,
					REFERENCED_ENTITY_ALIAS,
					alias
			);
			associationQueries.add( result );
			associationQueryMap.put( associationName, result );
		}
		return result;
	}

	// Query properties

	private Integer maxResults;
	private Integer firstResult;
	private Boolean cacheable;
	private String cacheRegion;
	private String comment;
	private FlushMode flushMode;
	private CacheMode cacheMode;
	private Integer timeout;
	private LockOptions lockOptions = new LockOptions( LockMode.NONE );

	public AuditQuery setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	public AuditQuery setFirstResult(int firstResult) {
		this.firstResult = firstResult;
		return this;
	}

	public AuditQuery setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public AuditQuery setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
		return this;
	}

	public AuditQuery setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public AuditQuery setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	public AuditQuery setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	public AuditQuery setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Set lock mode
	 *
	 * @param lockMode The {@link LockMode} used for this query.
	 *
	 * @return this object
	 *
	 * @deprecated Instead use setLockOptions
	 */
	@Deprecated
	public AuditQuery setLockMode(LockMode lockMode) {
		lockOptions.setLockMode( lockMode );
		return this;
	}

	/**
	 * Set lock options
	 *
	 * @param lockOptions The @{link LockOptions} used for this query.
	 *
	 * @return this object
	 */
	public AuditQuery setLockOptions(LockOptions lockOptions) {
		LockOptions.copy( lockOptions, this.lockOptions );
		return this;
	}

	protected void setQueryProperties(Query query) {
		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}
		if ( firstResult != null ) {
			query.setFirstResult( firstResult );
		}
		if ( cacheable != null ) {
			query.setCacheable( cacheable );
		}
		if ( cacheRegion != null ) {
			query.setCacheRegion( cacheRegion );
		}
		if ( comment != null ) {
			query.setComment( comment );
		}
		if ( flushMode != null ) {
			query.setFlushMode( flushMode );
		}
		if ( cacheMode != null ) {
			query.setCacheMode( cacheMode );
		}
		if ( timeout != null ) {
			query.setTimeout( timeout );
		}
		if ( lockOptions != null && lockOptions.getLockMode() != LockMode.NONE ) {
			query.setLockMode( REFERENCED_ENTITY_ALIAS, lockOptions.getLockMode() );
		}
	}

	protected List applyProjections(final List queryResult, final Number revision) {
		final List result = new ArrayList( queryResult.size() );
		if ( hasProjection() ) {
			for (final Object qr : queryResult) {
				if ( projections.size() == 1 ) {
					// qr is the value of the projection itself
					final Pair<String, AuditProjection> projection = projections.get( 0 );
					result.add( projection.getSecond().convertQueryResult( enversService, entityInstantiator, projection.getFirst(), revision, qr ) );
				}
				else {
					// qr is an array where each of its components holds the value of corresponding projection
					Object[] qresults = (Object[]) qr;
					Object[] tresults = new Object[qresults.length];
					for ( int i = 0; i < qresults.length; i++ ) {
						final Pair<String, AuditProjection> projection = projections.get( i );
						tresults[i] = projection.getSecond().convertQueryResult( enversService, entityInstantiator, projection.getFirst(), revision, qresults[i] );
					}
					result.add( tresults );
				}
			}
		}
		else {
			entityInstantiator.addInstancesFromVersionsEntities( entityName, result, queryResult, revision );
		}
		return result;
	}

	protected EntityConfiguration getEntityConfiguration() {
		return enversService.getEntitiesConfigurations().get( entityName );
	}

	protected String getEntityName() {
		// todo: can this be replaced by a call to getEntittyConfiguration#getEntityClassName()?
		return entityName;
	}
}
