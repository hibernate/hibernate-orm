/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.jpa;

import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for {@link JpaTupleElement} impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaTupleElement<T>
		extends AbstractSqmNode
		implements SqmVisitableNode, JpaTupleElement<T> {

	private @Nullable SqmExpressible<T> expressibleType;
	private @Nullable String alias;

	protected AbstractJpaTupleElement(@Nullable SqmExpressible<? super T> expressibleType, NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		setExpressibleType( expressibleType );
	}

	protected void copyTo(AbstractJpaTupleElement<T> target, SqmCopyContext context) {
		target.alias = alias;
	}

	@Override
	public @Nullable String getAlias() {
		return alias;
	}

	/**
	 * Protected access to set the alias.
	 */
	protected void setAlias(@Nullable String alias) {
		this.alias = alias;
	}

	public @Nullable SqmExpressible<T> getNodeType() {
		return expressibleType;
	}

	protected final void setExpressibleType(@Nullable SqmExpressible<?> expressibleType) {
		//noinspection unchecked
		this.expressibleType = (SqmExpressible<T>) expressibleType;
	}

}
