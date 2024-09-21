/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CommonAbstractCriteria;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaBase extends CommonAbstractCriteria, JpaCriteriaNode {
	@Override
	<U> JpaSubQuery<U> subquery(Class<U> type);

	@Override
	JpaPredicate getRestriction();
}
