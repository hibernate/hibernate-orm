/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

/**
 * SqmPath specialization for an SqmPath that wraps another SqmPath
 *
 * @author Steve Ebersole
 */
public interface SqmPathWrapper<W,T> extends SqmPath<T> {
	/**
	 * Access the wrapped SqmPath.
	 */
	SqmPath<W> getWrappedPath();
}
