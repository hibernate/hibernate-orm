/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.update;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmAssignment<T> implements SqmCacheable {
	private final SqmPath<T> targetPath;
	private final SqmExpression<? extends T> value;

	public SqmAssignment(SqmPath<T> targetPath, SqmExpression<? extends T> value) {
		this.targetPath = targetPath;
		this.value = value;
		this.value.applyInferableType( targetPath.getNodeType() );
	}

	public SqmAssignment<T> copy(SqmCopyContext context) {
		return new SqmAssignment<>( targetPath.copy( context ), value.copy( context ) );
	}

	/**
	 * The attribute/path to be updated
	 */
	public SqmPath<T> getTargetPath() {
		return targetPath;
	}

	/**
	 * The new value
	 */
	public SqmExpression<? extends T> getValue() {
		return value;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmAssignment<?> that
			&& targetPath.equals( that.targetPath )
			&& value.equals( that.value );
	}

	@Override
	public int hashCode() {
		int result = targetPath.hashCode();
		result = 31 * result + value.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmAssignment<?> that
			&& targetPath.isCompatible( that.targetPath )
			&& value.isCompatible( that.value );
	}

	@Override
	public int cacheHashCode() {
		int result = targetPath.cacheHashCode();
		result = 31 * result + value.cacheHashCode();
		return result;
	}
}
