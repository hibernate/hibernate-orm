/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
public interface JpaPredicate extends JpaBooleanExpression, Predicate {
	@Nonnull
	@Override
	JpaPredicate not();
}
