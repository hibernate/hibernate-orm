/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.sql.ast.spi.query.expression.JsonObjectAggUniqueKeysBehavior;

import jakarta.annotation.Nullable;

/**
 * Specifies if a {@code json_objectagg} may aggregate duplicate keys.
 *
 * @since 7.0
 */
public enum SqmJsonObjectAggUniqueKeysBehavior implements SqmTypedNode<Object> {
	/**
	 * Aggregate only unique keys. Fail aggregation if a duplicate is encountered.
	 */
	WITH,
	/**
	 * Aggregate duplicate keys without failing.
	 */
	WITHOUT;

	@Override
	public @Nullable SqmBindableType<Object> getNodeType() {
		return null;
	}

	@Override
	public @Nonnull NodeBuilder nodeBuilder() {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public SqmJsonObjectAggUniqueKeysBehavior copy(@Nonnull SqmCopyContext context) {
		return this;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		//noinspection unchecked
		return (X) (this == WITH ? JsonObjectAggUniqueKeysBehavior.WITH : JsonObjectAggUniqueKeysBehavior.WITHOUT);
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		if ( this == WITH ) {
			hql.append( " with unique keys" );
		}
		else {
			hql.append( " without unique keys" );
		}
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return this == object;
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
