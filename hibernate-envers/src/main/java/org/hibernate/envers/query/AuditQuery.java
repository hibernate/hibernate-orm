/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query;

import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

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
