/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.criteria.JpaPluralExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmPluralPath<C,E> extends SqmPath<C>, JpaPluralExpression<C,E> {
}
