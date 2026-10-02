package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;

/**
 * SqmPath specialization for an SqmPath that wraps another SqmPath
 *
 * @author Steve Ebersole
 */
public interface SqmPathWrapper<W,T> extends SqmPath<T> {
	/**
	 * Access the wrapped SqmPath.
	 */
	@Nonnull
	SqmPath<W> getWrappedPath();
}
