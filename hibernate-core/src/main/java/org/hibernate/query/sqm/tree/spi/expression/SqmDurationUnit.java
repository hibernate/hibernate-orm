/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;


/**
 * @author Gavin King
 */
public class SqmDurationUnit<T> extends AbstractSqmNode implements SqmTypedNode<T> {
	private final TemporalUnit unit;
	private final ReturnableType<T> type;

	public SqmDurationUnit(@Nonnull TemporalUnit unit, @Nonnull ReturnableType<T> type, @Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.type = type;
		this.unit = unit;
	}

	@Nonnull
	@Override
	public SqmDurationUnit<T> copy(@Nonnull SqmCopyContext context) {
		return this;
	}

	@Nonnull
	public ReturnableType<T> getType() {
		return type;
	}

	@Nullable
	@Override
	public <R> R accept(@Nonnull SemanticQueryWalker<R> walker) {
		return walker.visitDurationUnit( this );
	}

	@Nonnull
	public TemporalUnit getUnit() {
		return unit;
	}

	@Override
	public @Nullable SqmBindableType<T> getNodeType() {
		return nodeBuilder().resolveExpressible( type );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( unit );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmDurationUnit<?> that
			&& this.unit == that.unit;
	}

	@Override
	public int hashCode() {
		return unit.hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
