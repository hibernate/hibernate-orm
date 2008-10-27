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
package org.jboss.envers.query;

import org.jboss.envers.query.criteria.VersionsCriterion;
import org.jboss.envers.query.projection.VersionsProjection;
import org.jboss.envers.query.order.VersionsOrder;
import org.jboss.envers.exception.VersionsException;
import org.hibernate.FlushMode;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;

import javax.persistence.NonUniqueResultException;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * @author Adam Warski (adam at warski dot org)
 * @see org.hibernate.Criteria
 */
public interface VersionsQuery {
    List getResultList() throws VersionsException;

    Object getSingleResult() throws VersionsException, NonUniqueResultException, NoResultException;

    VersionsQuery add(VersionsCriterion criterion);

    VersionsQuery addProjection(String function, String propertyName);

    VersionsQuery addProjection(VersionsProjection projection);

    VersionsQuery addOrder(String propertyName, boolean asc);

    VersionsQuery addOrder(VersionsOrder order);

    VersionsQuery setMaxResults(int maxResults);

	VersionsQuery setFirstResult(int firstResult);

    VersionsQuery setCacheable(boolean cacheable);

    VersionsQuery setCacheRegion(String cacheRegion);

    VersionsQuery setComment(String comment);

    VersionsQuery setFlushMode(FlushMode flushMode);

    VersionsQuery setCacheMode(CacheMode cacheMode);

    VersionsQuery setTimeout(int timeout);

    VersionsQuery setLockMode(LockMode lockMode);
}
