/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

/**
 * Specialization of {@link SqmPath} for paths that are not explicitly defined
 * in the from-clause ({@link org.hibernate.query.sqm.tree.from.SqmFrom}}
 *
 * @author Steve Ebersole
 */
public interface SqmSimplePath<T> extends SqmPath<T> {
}
