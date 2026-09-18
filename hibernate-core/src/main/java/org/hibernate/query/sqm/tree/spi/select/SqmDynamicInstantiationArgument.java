/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.select;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import java.util.Objects;

/**
 * Represents an individual argument to a dynamic instantiation.
 *
 * @author Steve Ebersole
 */
public class SqmDynamicInstantiationArgument<T> implements SqmAliasedNode<T> {
	private final SqmSelectableNode<T> selectableNode;
	private final @Nullable String alias;
	private final NodeBuilder nodeBuilder;

	public SqmDynamicInstantiationArgument(
			@Nonnull SqmSelectableNode<T> selectableNode,
			@Nullable String alias,
			@Nonnull NodeBuilder nodeBuilder) {
		this.selectableNode = selectableNode;
		this.alias = alias;
		this.nodeBuilder = nodeBuilder;
	}

	@Nonnull
	@Override
	public SqmDynamicInstantiationArgument<T> copy(@Nonnull SqmCopyContext context) {
		return new SqmDynamicInstantiationArgument<>(
				selectableNode.copy( context ),
				alias,
				nodeBuilder
		);
	}

	@Nonnull
	@Override
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	public @Nullable String getAlias() {
		return alias;
	}

	@Override
	public @Nonnull NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return selectableNode.accept( walker );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		selectableNode.appendHqlString( hql, context );
		if ( alias != null ) {
			hql.append( " as " ).append( alias );
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmDynamicInstantiationArgument<?> that
			&& selectableNode.equals( that.selectableNode )
			&& Objects.equals( alias, that.alias );
	}

	@Override
	public int hashCode() {
		int result = selectableNode.hashCode();
		result = 31 * result + Objects.hashCode( alias );
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmDynamicInstantiationArgument<?> that
			&& selectableNode.isCompatible( that.selectableNode )
			&& Objects.equals( alias, that.alias );
	}

	@Override
	public int cacheHashCode() {
		int result = selectableNode.cacheHashCode();
		result = 31 * result + Objects.hashCode( alias );
		return result;
	}
}
