/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.jpa;

import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.AbstractSqmNode;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmVisitableNode;

import jakarta.annotation.Nullable;

/**
 * Base support for {@link JpaTupleElement} impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaTupleElement<T>
		extends AbstractSqmNode
		implements SqmVisitableNode, JpaTupleElement<T> {

	private @Nullable SqmBindableType<T> expressibleType;
	private @Nullable String alias;

	protected AbstractJpaTupleElement(@Nullable SqmBindableType<? super T> expressibleType, NodeBuilder criteriaBuilder) {
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

	public @Nullable SqmBindableType<T> getNodeType() {
		return expressibleType;
	}

	protected final void setExpressibleType(
			// This is fine, since this method is final
			AbstractJpaTupleElement<T> this,
			@Nullable SqmBindableType<?> expressibleType) {
		//noinspection unchecked
		this.expressibleType = (SqmBindableType<T>) expressibleType;
	}

}
