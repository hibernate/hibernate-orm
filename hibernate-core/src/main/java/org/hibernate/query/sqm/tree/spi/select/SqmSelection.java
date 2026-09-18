/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.select;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.Objects;

/**
 * Represents an individual selection within a select clause.
 *
 * @author Steve Ebersole
 */
public class SqmSelection<T> extends AbstractSqmNode implements SqmAliasedNode<T> {
	private final SqmSelectableNode<T> selectableNode;
	private final @Nullable String alias;

	public SqmSelection(
			@Nonnull SqmSelectableNode<T> selectableNode,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = selectableNode.getAlias();
	}

	public SqmSelection(
			@Nonnull SqmSelectableNode<T> selectableNode,
			@Nullable String alias,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = alias;
		if ( alias != null ) {
			selectableNode.alias( alias );
		}
	}

	@Nonnull
	@Override
	public SqmSelection<T> copy(@Nonnull SqmCopyContext context) {
		return new SqmSelection<>( selectableNode.copy( context ), alias, nodeBuilder() );
	}

	@Nonnull
	@Override
	public SqmSelectableNode<T> getSelectableNode() {
		return selectableNode;
	}

	@Override
	public @Nullable JavaType<T> getNodeJavaType() {
		return selectableNode.getNodeJavaType();
	}

	@Override
	public @Nullable String getAlias() {
		return alias;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitSelection( this );
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
		return object instanceof SqmSelection<?> that
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
		return object instanceof SqmSelection<?> that
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
