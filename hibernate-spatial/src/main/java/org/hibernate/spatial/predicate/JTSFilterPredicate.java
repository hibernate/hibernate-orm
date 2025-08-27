/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.predicate;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * JPA Spatial Filter {@link Predicate}
 *
 * @deprecated Use {@link org.hibernate.spatial.criteria.JTSSpatialCriteriaBuilder JTSSpatialCriteriaBuilder} instead
 */
@Deprecated(since = "6.2")
class JTSFilterPredicate {

	private Expression<?> geometry;
	private Expression<?> filter;

//require to completely re-implement


}
