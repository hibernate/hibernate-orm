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
package org.hibernate.envers.query;

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;

/**
 * @author Adam Warski (adam at warski dot org)
 * @see org.hibernate.Criteria
 */
public interface AuditQuery {
    List getResultList() throws AuditException;

    Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException;

    AuditQuery add(AuditCriterion criterion);

    AuditQuery addProjection(AuditProjection projection);

    AuditQuery addOrder(AuditOrder order);

    AuditQuery setMaxResults(int maxResults);

	AuditQuery setFirstResult(int firstResult);

    AuditQuery setCacheable(boolean cacheable);

    AuditQuery setCacheRegion(String cacheRegion);

    AuditQuery setComment(String comment);

    AuditQuery setFlushMode(FlushMode flushMode);

    AuditQuery setCacheMode(CacheMode cacheMode);

    AuditQuery setTimeout(int timeout);

    AuditQuery setLockMode(LockMode lockMode);
}
