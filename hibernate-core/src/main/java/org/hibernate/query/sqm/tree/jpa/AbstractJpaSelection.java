/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.jpa;

import java.util.List;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base support for {@link JpaSelection} impls.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaSelection<T>
		extends AbstractJpaTupleElement<T>
		implements SqmSelectableNode<T>, JpaSelection<T> {
	protected AbstractJpaSelection(@Nullable SqmBindableType<? super T> sqmExpressible, NodeBuilder criteriaBuilder) {
		super( sqmExpressible, criteriaBuilder );
	}

	@Override
	public JpaSelection<T> alias(String alias) {
		setAlias( alias );
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
