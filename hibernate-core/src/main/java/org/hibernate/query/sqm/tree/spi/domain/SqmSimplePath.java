/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import org.hibernate.query.sqm.tree.spi.from.SqmFrom;

/**
 * Specialization of {@link SqmPath} for paths that are not explicitly defined
 * in the from-clause ({@link SqmFrom}}
 *
 * @author Steve Ebersole
 */
public interface SqmSimplePath<T> extends SqmPath<T> {
}
