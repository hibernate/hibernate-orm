/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.Triple;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.criteria.internal.CriteriaTools;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.tools.Pair;

import static org.hibernate.envers.internal.entities.mapper.relation.query.QueryConstants.REFERENCED_ENTITY_ALIAS;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 */
public abstract class AbstractAuditQuery implements AuditQuery {
	protected EntityInstantiator entityInstantiator;
	protected List<AuditCriterion> criterions;

	protected String entityName;
	protected String entityClassName;
	protected String versionsEntityName;
	protected QueryBuilder qb;

	protected boolean hasProjection;
	protected boolean hasOrder;

	protected final EnversService enversService;
	protected final AuditReaderImplementor versionsReader;

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

		criterions = new ArrayList<AuditCriterion>();
		entityInstantiator = new EntityInstantiator( enversService, versionsReader );

		entityClassName = cls.getName();
		this.entityName = entityName;
		versionsEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( entityName );

		qb = new QueryBuilder( versionsEntityName, REFERENCED_ENTITY_ALIAS );
	}

	protected Query buildQuery() {
		Query query = qb.toQuery( versionsReader.getSession() );
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
		Triple<String, String, Boolean> projectionData = projection.getData( enversService );
		hasProjection = true;
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				projectionData.getSecond()
		);
		qb.addProjection( projectionData.getFirst(), propertyName, projectionData.getThird() );
		return this;
	}

	public AuditQuery addOrder(AuditOrder order) {
		hasOrder = true;
		Pair<String, Boolean> orderData = order.getData( enversService );
		String propertyName = CriteriaTools.determinePropertyName(
				enversService,
				versionsReader,
				entityName,
				orderData.getFirst()
		);
		qb.addOrder( propertyName, orderData.getSecond() );
		return this;
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
}
