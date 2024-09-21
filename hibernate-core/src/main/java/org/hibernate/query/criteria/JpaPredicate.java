/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaPredicate extends JpaExpression<Boolean>, Predicate {
	@Override
	JpaPredicate not();
}
