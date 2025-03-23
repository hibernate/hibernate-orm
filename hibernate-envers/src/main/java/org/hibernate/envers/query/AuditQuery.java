/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query;

import java.util.List;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditQuery {
	List getResultList() throws AuditException;

	Object getSingleResult() throws AuditException, NonUniqueResultException, NoResultException;

	AuditAssociationQuery<? extends AuditQuery> traverseRelation(String associationName, JoinType joinType);

	AuditAssociationQuery<? extends AuditQuery> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias);

	@Incubating
	AuditAssociationQuery<? extends AuditQuery> traverseRelation(
			String associationName,
			JoinType joinType,
			String alias,
			AuditCriterion onClauseCriterion);

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

	String getAlias();
}
