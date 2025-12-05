/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
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
			SqmSelectableNode<T> selectableNode,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = selectableNode.getAlias();
	}

	public SqmSelection(
			SqmSelectableNode<T> selectableNode,
			@Nullable String alias,
			NodeBuilder nodeBuilder) {
		super( nodeBuilder );

		assert selectableNode != null;
		this.selectableNode = selectableNode;
		this.alias = alias;
		if ( alias != null ) {
			selectableNode.alias( alias );
		}
	}

	@Override
	public SqmSelection<T> copy(SqmCopyContext context) {
		return new SqmSelection<>( selectableNode.copy( context ), alias, nodeBuilder() );
	}

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

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSelection( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
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
	public boolean isCompatible(Object object) {
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
