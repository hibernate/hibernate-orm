/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.util.StringHelper;

/**
 * Implementation of the <tt>Criteria</tt> interface
 * @author Gavin King
 */
public class CriteriaImpl implements Criteria, Serializable {

	private final String entityOrClassName;
	private transient SessionImplementor session;
	private final String rootAlias;

	private List criterionEntries = new ArrayList();
	private List orderEntries = new ArrayList();
	private Projection projection;
	private Criteria projectionCriteria;

	private List subcriteriaList = new ArrayList();

	private Map fetchModes = new HashMap();
	private Map lockModes = new HashMap();

	private Integer maxResults;
	private Integer firstResult;
	private Integer timeout;
	private Integer fetchSize;

	private boolean cacheable;
	private String cacheRegion;
	private String comment;

	private FlushMode flushMode;
	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	private Boolean readOnly;

	private ResultTransformer resultTransformer = Criteria.ROOT_ENTITY;


	// Constructors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public CriteriaImpl(String entityOrClassName, SessionImplementor session) {
		this(entityOrClassName, ROOT_ALIAS, session);
	}

	public CriteriaImpl(String entityOrClassName, String alias, SessionImplementor session) {
		this.session = session;
		this.entityOrClassName = entityOrClassName;
		this.cacheable = false;
		this.rootAlias = alias;
	}

	public String toString() {
		return "CriteriaImpl(" +
			entityOrClassName + ":" +
			(rootAlias==null ? "" : rootAlias) +
			subcriteriaList.toString() +
			criterionEntries.toString() +
			( projection==null ? "" : projection.toString() ) +
			')';
	}


	// State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public SessionImplementor getSession() {
		return session;
	}

	public void setSession(SessionImplementor session) {
		this.session = session;
	}

	public String getEntityOrClassName() {
		return entityOrClassName;
	}

	public Map getLockModes() {
		return lockModes;
	}

	public Criteria getProjectionCriteria() {
		return projectionCriteria;
	}

	public Iterator iterateSubcriteria() {
		return subcriteriaList.iterator();
	}

	public Iterator iterateExpressionEntries() {
		return criterionEntries.iterator();
	}

	public Iterator iterateOrderings() {
		return orderEntries.iterator();
	}

	public Criteria add(Criteria criteriaInst, Criterion expression) {
		criterionEntries.add( new CriterionEntry(expression, criteriaInst) );
		return this;
	}


	// Criteria impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String getAlias() {
		return rootAlias;
	}

	public Projection getProjection() {
		return projection;
	}

	public Criteria setProjection(Projection projection) {
		this.projection = projection;
		this.projectionCriteria = this;
		setResultTransformer( PROJECTION );
		return this;
	}

	public Criteria add(Criterion expression) {
		add( this, expression );
		return this;
	}

	public Criteria addOrder(Order ordering) {
		orderEntries.add( new OrderEntry( ordering, this ) );
		return this;
	}

	public FetchMode getFetchMode(String path) {
		return (FetchMode) fetchModes.get(path);
	}

	public Criteria setFetchMode(String associationPath, FetchMode mode) {
		fetchModes.put( associationPath, mode );
		return this;
	}

	public Criteria setLockMode(LockMode lockMode) {
		return setLockMode( getAlias(), lockMode );
	}

	public Criteria setLockMode(String alias, LockMode lockMode) {
		lockModes.put( alias, lockMode );
		return this;
	}

	public Criteria createAlias(String associationPath, String alias) {
		return createAlias( associationPath, alias, INNER_JOIN );
	}

	public Criteria createAlias(String associationPath, String alias, int joinType) {
		new Subcriteria( this, associationPath, alias, joinType );
		return this;
	}

	public Criteria createAlias(String associationPath, String alias, int joinType, Criterion withClause) {
		new Subcriteria( this, associationPath, alias, joinType, withClause );
		return this;
	}

	public Criteria createCriteria(String associationPath) {
		return createCriteria( associationPath, INNER_JOIN );
	}

	public Criteria createCriteria(String associationPath, int joinType) {
		return new Subcriteria( this, associationPath, joinType );
	}

	public Criteria createCriteria(String associationPath, String alias) {
		return createCriteria( associationPath, alias, INNER_JOIN );
	}

	public Criteria createCriteria(String associationPath, String alias, int joinType) {
		return new Subcriteria( this, associationPath, alias, joinType );
	}

	public Criteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause) {
		return new Subcriteria( this, associationPath, alias, joinType, withClause );
	}

	public ResultTransformer getResultTransformer() {
		return resultTransformer;
	}

	public Criteria setResultTransformer(ResultTransformer tupleMapper) {
		this.resultTransformer = tupleMapper;
		return this;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public Criteria setMaxResults(int maxResults) {
		this.maxResults = new Integer(maxResults);
		return this;
	}

	public Integer getFirstResult() {
		return firstResult;
	}

	public Criteria setFirstResult(int firstResult) {
		this.firstResult = new Integer(firstResult);
		return this;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public Criteria setFetchSize(int fetchSize) {
		this.fetchSize = new Integer(fetchSize);
		return this;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Criteria setTimeout(int timeout) {
		this.timeout = new Integer(timeout);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnlyInitialized() {
		return readOnly != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnly() {
		if ( ! isReadOnlyInitialized() && getSession() == null ) {
			throw new IllegalStateException(
					"cannot determine readOnly/modifiable setting when it is not initialized and is not initialized and getSession() == null"
			);
		}
		return ( isReadOnlyInitialized() ?
				readOnly.booleanValue() :
				getSession().getPersistenceContext().isDefaultReadOnly()
		);
	}

	/**
	 * {@inheritDoc}
	 */
	public Criteria setReadOnly(boolean readOnly) {
		this.readOnly = Boolean.valueOf( readOnly );
		return this;
	}

	public boolean getCacheable() {
		return this.cacheable;
	}

	public Criteria setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public String getCacheRegion() {
		return this.cacheRegion;
	}

	public Criteria setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion.trim();
		return this;
	}

	public String getComment() {
		return comment;
	}

	public Criteria setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Criteria setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	public Criteria setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	public List list() throws HibernateException {
		before();
		try {
			return session.list( this );
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll() {
		return scroll( ScrollMode.SCROLL_INSENSITIVE );
	}

	public ScrollableResults scroll(ScrollMode scrollMode) {
		before();
		try {
			return session.scroll(this, scrollMode);
		}
		finally {
			after();
		}
	}

	public Object uniqueResult() throws HibernateException {
		return AbstractQueryImpl.uniqueElement( list() );
	}

	protected void before() {
		if ( flushMode != null ) {
			sessionFlushMode = getSession().getFlushMode();
			getSession().setFlushMode( flushMode );
		}
		if ( cacheMode != null ) {
			sessionCacheMode = getSession().getCacheMode();
			getSession().setCacheMode( cacheMode );
		}
	}

	protected void after() {
		if ( sessionFlushMode != null ) {
			getSession().setFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getSession().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
	}

	public boolean isLookupByNaturalKey() {
		if ( projection != null ) {
			return false;
		}
		if ( subcriteriaList.size() > 0 ) {
			return false;
		}
		if ( criterionEntries.size() != 1 ) {
			return false;
		}
		CriterionEntry ce = (CriterionEntry) criterionEntries.get(0);
		return ce.getCriterion() instanceof NaturalIdentifier;
	}


	// Inner classes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final class Subcriteria implements Criteria, Serializable {

		private String alias;
		private String path;
		private Criteria parent;
		private LockMode lockMode;
		private int joinType;
		private Criterion withClause;

		// Constructors ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		private Subcriteria(Criteria parent, String path, String alias, int joinType, Criterion withClause) {
			this.alias = alias;
			this.path = path;
			this.parent = parent;
			this.joinType = joinType;
			this.withClause = withClause;
			CriteriaImpl.this.subcriteriaList.add( this );
		}

		private Subcriteria(Criteria parent, String path, String alias, int joinType) {
			this( parent, path, alias, joinType, null );
		}

		private Subcriteria(Criteria parent, String path, int joinType) {
			this( parent, path, null, joinType );
		}

		public String toString() {
			return "Subcriteria("
					+ path + ":"
					+ (alias==null ? "" : alias)
					+ ')';
		}


		// State ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getPath() {
			return path;
		}

		public Criteria getParent() {
			return parent;
		}

		public LockMode getLockMode() {
			return lockMode;
		}

		public Criteria setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}

		public int getJoinType() {
			return joinType;
		}

		public Criterion getWithClause() {
			return this.withClause;
		}


		// Criteria impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		public Criteria add(Criterion expression) {
			CriteriaImpl.this.add(this, expression);
			return this;
		}

		public Criteria addOrder(Order order) {
			CriteriaImpl.this.orderEntries.add( new OrderEntry(order, this) );
			return this;
		}

		public Criteria createAlias(String associationPath, String alias) {
			return createAlias( associationPath, alias, INNER_JOIN );
		}

		public Criteria createAlias(String associationPath, String alias, int joinType) throws HibernateException {
			new Subcriteria( this, associationPath, alias, joinType );
			return this;
		}

		public Criteria createAlias(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
			new Subcriteria( this, associationPath, alias, joinType, withClause );
			return this;
		}

		public Criteria createCriteria(String associationPath) {
			return createCriteria( associationPath, INNER_JOIN );
		}

		public Criteria createCriteria(String associationPath, int joinType) throws HibernateException {
			return new Subcriteria( Subcriteria.this, associationPath, joinType );
		}

		public Criteria createCriteria(String associationPath, String alias) {
			return createCriteria( associationPath, alias, INNER_JOIN );
		}

		public Criteria createCriteria(String associationPath, String alias, int joinType) throws HibernateException {
			return new Subcriteria( Subcriteria.this, associationPath, alias, joinType );
		}

		public Criteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
			return new Subcriteria( this, associationPath, alias, joinType, withClause );
		}

		public boolean isReadOnly() {
			return CriteriaImpl.this.isReadOnly();
		}

		public boolean isReadOnlyInitialized() {
			return CriteriaImpl.this.isReadOnlyInitialized();
		}

		public Criteria setReadOnly(boolean readOnly) {
			CriteriaImpl.this.setReadOnly( readOnly );
			return this;
		}

		public Criteria setCacheable(boolean cacheable) {
			CriteriaImpl.this.setCacheable(cacheable);
			return this;
		}

		public Criteria setCacheRegion(String cacheRegion) {
			CriteriaImpl.this.setCacheRegion(cacheRegion);
			return this;
		}

		public List list() throws HibernateException {
			return CriteriaImpl.this.list();
		}

		public ScrollableResults scroll() throws HibernateException {
			return CriteriaImpl.this.scroll();
		}

		public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
			return CriteriaImpl.this.scroll(scrollMode);
		}

		public Object uniqueResult() throws HibernateException {
			return CriteriaImpl.this.uniqueResult();
		}

		public Criteria setFetchMode(String associationPath, FetchMode mode) {
			CriteriaImpl.this.setFetchMode( StringHelper.qualify(path, associationPath), mode);
			return this;
		}

		public Criteria setFlushMode(FlushMode flushMode) {
			CriteriaImpl.this.setFlushMode(flushMode);
			return this;
		}

		public Criteria setCacheMode(CacheMode cacheMode) {
			CriteriaImpl.this.setCacheMode(cacheMode);
			return this;
		}

		public Criteria setFirstResult(int firstResult) {
			CriteriaImpl.this.setFirstResult(firstResult);
			return this;
		}

		public Criteria setMaxResults(int maxResults) {
			CriteriaImpl.this.setMaxResults(maxResults);
			return this;
		}

		public Criteria setTimeout(int timeout) {
			CriteriaImpl.this.setTimeout(timeout);
			return this;
		}

		public Criteria setFetchSize(int fetchSize) {
			CriteriaImpl.this.setFetchSize(fetchSize);
			return this;
		}

		public Criteria setLockMode(String alias, LockMode lockMode) {
			CriteriaImpl.this.setLockMode(alias, lockMode);
			return this;
		}

		public Criteria setResultTransformer(ResultTransformer resultProcessor) {
			CriteriaImpl.this.setResultTransformer(resultProcessor);
			return this;
		}

		public Criteria setComment(String comment) {
			CriteriaImpl.this.setComment(comment);
			return this;
		}

		public Criteria setProjection(Projection projection) {
			CriteriaImpl.this.projection = projection;
			CriteriaImpl.this.projectionCriteria = this;
			setResultTransformer(PROJECTION);
			return this;
		}
	}

	public static final class CriterionEntry implements Serializable {
		private final Criterion criterion;
		private final Criteria criteria;

		private CriterionEntry(Criterion criterion, Criteria criteria) {
			this.criteria = criteria;
			this.criterion = criterion;
		}

		public Criterion getCriterion() {
			return criterion;
		}

		public Criteria getCriteria() {
			return criteria;
		}

		public String toString() {
			return criterion.toString();
		}
	}

	public static final class OrderEntry implements Serializable {
		private final Order order;
		private final Criteria criteria;

		private OrderEntry(Order order, Criteria criteria) {
			this.criteria = criteria;
			this.order = order;
		}

		public Order getOrder() {
			return order;
		}

		public Criteria getCriteria() {
			return criteria;
		}

		public String toString() {
			return order.toString();
		}
	}
}
