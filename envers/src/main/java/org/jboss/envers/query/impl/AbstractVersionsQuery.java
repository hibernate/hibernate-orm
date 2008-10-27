/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.query.impl;

import org.jboss.envers.reader.VersionsReaderImplementor;
import org.jboss.envers.entities.EntityInstantiator;
import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.query.VersionsQuery;
import org.jboss.envers.query.order.VersionsOrder;
import org.jboss.envers.query.projection.VersionsProjection;
import org.jboss.envers.exception.VersionsException;
import org.jboss.envers.configuration.VersionsConfiguration;
import org.jboss.envers.tools.Pair;
import org.jboss.envers.tools.Triple;
import org.jboss.envers.tools.query.QueryBuilder;
import org.hibernate.*;

import javax.persistence.NonUniqueResultException;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractVersionsQuery implements VersionsQuery {
    protected EntityInstantiator entityInstantiator;
    protected List<VersionsCriterion> criterions;

    protected String entityName;
    protected String versionsEntityName;
    protected QueryBuilder qb;

    protected boolean hasProjection;
    protected boolean hasOrder;

    protected final VersionsConfiguration verCfg;
    private final VersionsReaderImplementor versionsReader;

    protected AbstractVersionsQuery(VersionsConfiguration verCfg, VersionsReaderImplementor versionsReader,
                                    Class<?> cls) {
        this.verCfg = verCfg;
        this.versionsReader = versionsReader;

        criterions = new ArrayList<VersionsCriterion>();
        entityInstantiator = new EntityInstantiator(verCfg, versionsReader);

        entityName = cls.getName();
        versionsEntityName = verCfg.getVerEntCfg().getVersionsEntityName(entityName);

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

    public abstract List list() throws VersionsException;

    public List getResultList() throws VersionsException {
        return list();
    }

    public Object getSingleResult() throws VersionsException, NonUniqueResultException, NoResultException {
        List result = list();

        if (result == null || result.size() == 0) {
            throw new NoResultException();
        }

        if (result.size() > 1) {
            throw new NonUniqueResultException();
        }

        return result.get(0);
    }

    public VersionsQuery add(VersionsCriterion criterion) {
        criterions.add(criterion);
        return this;
    }

    // Projection and order

    public VersionsQuery addProjection(String function, String propertyName) {
        hasProjection = true;
        qb.addProjection(function, propertyName, false);
        return this;
    }

    public VersionsQuery addProjection(VersionsProjection projection) {
        Triple<String, String, Boolean> projectionData = projection.getData(verCfg);
        hasProjection = true;
        qb.addProjection(projectionData.getFirst(), projectionData.getSecond(), projectionData.getThird());
        return this;
    }

    public VersionsQuery addOrder(String propertyName, boolean asc) {
        hasOrder = true;
        qb.addOrder(propertyName, asc);
        return this;
    }

    public VersionsQuery addOrder(VersionsOrder order) {
        Pair<String, Boolean> orderData = order.getData(verCfg);
        return addOrder(orderData.getFirst(), orderData.getSecond());
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
    private LockMode lockMode;

    public VersionsQuery setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public VersionsQuery setFirstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    public VersionsQuery setCacheable(boolean cacheable) {
        this.cacheable = cacheable;
        return this;
    }

    public VersionsQuery setCacheRegion(String cacheRegion) {
        this.cacheRegion = cacheRegion;
        return this;
    }

    public VersionsQuery setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public VersionsQuery setFlushMode(FlushMode flushMode) {
        this.flushMode = flushMode;
        return this;
    }

    public VersionsQuery setCacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
        return this;
    }

    public VersionsQuery setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public VersionsQuery setLockMode(LockMode lockMode) {
        this.lockMode = lockMode;
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
        if (lockMode != null) query.setLockMode("e", lockMode);
    }
}
