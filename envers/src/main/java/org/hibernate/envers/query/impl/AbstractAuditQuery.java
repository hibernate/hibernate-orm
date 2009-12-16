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
 */
package org.hibernate.envers.query.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.entities.EntityInstantiator;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.reader.AuditReaderImplementor;
import org.hibernate.envers.tools.Pair;
import org.hibernate.envers.tools.Triple;
import org.hibernate.envers.tools.query.QueryBuilder;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.LockOptions;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractAuditQuery implements AuditQuery {
    protected EntityInstantiator entityInstantiator;
    protected List<AuditCriterion> criterions;

    protected String entityName;
    protected String versionsEntityName;
    protected QueryBuilder qb;

    protected boolean hasProjection;
    protected boolean hasOrder;

    protected final AuditConfiguration verCfg;
    private final AuditReaderImplementor versionsReader;

    protected AbstractAuditQuery(AuditConfiguration verCfg, AuditReaderImplementor versionsReader,
                                    Class<?> cls) {
        this.verCfg = verCfg;
        this.versionsReader = versionsReader;

        criterions = new ArrayList<AuditCriterion>();
        entityInstantiator = new EntityInstantiator(verCfg, versionsReader);

        entityName = cls.getName();
        versionsEntityName = verCfg.getAuditEntCfg().getAuditEntityName(entityName);

        qb = new QueryBuilder(versionsEntityName, "e");
    }

    protected List buildAndExecuteQuery() {
        StringBuilder querySb = new StringBuilder();
        Map<String, Object> queryParamValues = new HashMap<String, Object>();

        qb.build(querySb, queryParamValues);

        Query query = versionsReader.getSession().createQuery(querySb.toString());
        for (Map.Entry<String, Object> paramValue : queryParamValues.entrySet()) {
            query.setParameter(paramValue.getKey(), paramValue.getValue());
        }

        setQueryProperties(query);

        return query.list();
    }

    public abstract List list() throws AuditException;

    public List getResultList() throws AuditException {
        return list();
    }

    public Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException {
        List result = list();

        if (result == null || result.size() == 0) {
            throw new NoResultException();
        }

        if (result.size() > 1) {
            throw new NonUniqueResultException();
        }

        return result.get(0);
    }

    public AuditQuery add(AuditCriterion criterion) {
        criterions.add(criterion);
        return this;
    }

    // Projection and order

    public AuditQuery addProjection(AuditProjection projection) {
        Triple<String, String, Boolean> projectionData = projection.getData(verCfg);
        hasProjection = true;
        qb.addProjection(projectionData.getFirst(), projectionData.getSecond(), projectionData.getThird());
        return this;
    }

    public AuditQuery addOrder(AuditOrder order) {
        hasOrder = true;

        Pair<String, Boolean> orderData = order.getData(verCfg);
        qb.addOrder(orderData.getFirst(), orderData.getSecond());
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
    private LockOptions lockOptions = new LockOptions(LockMode.NONE);

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
	 * @param lockMode
	 * @return this object
	 * @deprecated Instead use setLockOptions
	 */
    public AuditQuery setLockMode(LockMode lockMode) {
        lockOptions.setLockMode(lockMode);
        return this;
    }

	/**
	 * Set lock options
	 * @param lockOptions
	 * @return this object
	 */
	public AuditQuery setLockOptions(LockOptions lockOptions) {
		LockOptions.copy(lockOptions, this.lockOptions);
		return this;
	}
    protected void setQueryProperties(Query query) {
        if (maxResults != null) query.setMaxResults(maxResults);
        if (firstResult != null) query.setFirstResult(firstResult);
        if (cacheable != null) query.setCacheable(cacheable);
        if (cacheRegion != null) query.setCacheRegion(cacheRegion);
        if (comment != null) query.setComment(comment);
        if (flushMode != null) query.setFlushMode(flushMode);
        if (cacheMode != null) query.setCacheMode(cacheMode);
        if (timeout != null) query.setTimeout(timeout);
        if (lockOptions != null && lockOptions.getLockMode() != LockMode.NONE) {
			  query.setLockMode("e", lockOptions.getLockMode());
		  }
    }
}
