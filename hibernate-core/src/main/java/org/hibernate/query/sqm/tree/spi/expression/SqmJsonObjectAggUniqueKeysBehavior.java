/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.sql.ast.tree.expression.JsonObjectAggUniqueKeysBehavior;

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
	public NodeBuilder nodeBuilder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmJsonObjectAggUniqueKeysBehavior copy(SqmCopyContext context) {
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		//noinspection unchecked
		return (X) (this == WITH ? JsonObjectAggUniqueKeysBehavior.WITH : JsonObjectAggUniqueKeysBehavior.WITHOUT);
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( this == WITH ) {
			hql.append( " with unique keys" );
		}
		else {
			hql.append( " without unique keys" );
		}
	}

	@Override
	public boolean isCompatible(Object object) {
		return this == object;
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
