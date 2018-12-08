/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CommonAbstractCriteria;

/**
 * Hibernate API extension to the JPA CommonAbstractCriteria, which is the
 * common base contract for {@link JpaCriteriaQuery},
 * {@link JpaCriteriaDelete} and
 * {@link JpaCriteriaUpdate}
 *
 * @author Steve Ebersole
 */
public interface JpaCriteria extends CommonAbstractCriteria, JpaCriteriaNode {
	@Override
	<U> JpaSubQuery<U> subquery(Class<U> type);

	@Override
	JpaPredicate getRestriction();
}
