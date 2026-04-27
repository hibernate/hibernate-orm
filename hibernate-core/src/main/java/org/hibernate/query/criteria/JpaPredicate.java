/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaPredicate extends JpaBooleanExpression, Predicate {
	@Override
	JpaPredicate not();
}
