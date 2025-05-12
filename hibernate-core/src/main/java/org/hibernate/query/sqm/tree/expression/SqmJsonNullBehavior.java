/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.expression.JsonNullBehavior;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes how a {@code null} should be treated in a JSON document.
 *
 * @since 7.0
 */
public enum SqmJsonNullBehavior implements SqmTypedNode<Object> {
	/**
	 * {@code null} values are removed.
	 */
	ABSENT,
	/**
	 * {@code null} values are retained as JSON {@code null} literals.
	 */
	NULL;

	@Override
	public @Nullable SqmBindableType<Object> getNodeType() {
		return null;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmJsonNullBehavior copy(SqmCopyContext context) {
		return this;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		//noinspection unchecked
		return (X) (this == NULL ? JsonNullBehavior.NULL : JsonNullBehavior.ABSENT);
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		if ( this == NULL ) {
			hql.append( " null on null" );
		}
		else {
			hql.append( " absent on null" );
		}
	}
}
