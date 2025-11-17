/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CriteriaSelect;

/**
 * Extension of the JPA {@link CriteriaSelect}.
 *
 * @apiNote This is different from {@link JpaSelectCriteria}.  JPA added
 * {@link CriteriaSelect} in version 3.2 while {@link JpaSelectCriteria} has
 * existed for many releases prior.  JPA's {@link CriteriaSelect} is intended
 * for supporting its newly added set operations (union, interest, except).
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaSelect<T> extends CriteriaSelect<T> {

}
