/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.function.Consumer;
import jakarta.persistence.criteria.Selection;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;

/**
 * Defines a SQM AST node that can be used as a selection in the query,
 * or as an argument to a dynamic-instantiation.
 *
 * @author Steve Ebersole
 */
public interface SqmSelectableNode<T> extends JpaSelection<T>, SqmTypedNode<T> {
	/**
	 * Visit each of this selectable's direct sub-selectables - used to
	 * support JPA's {@link Selection} model (which is really a "selectable",
	 * just poorly named and poorly defined
	 *
	 * @see JpaSelection#getSelectionItems()
	 * @see Selection#getCompoundSelectionItems()
	 */
	void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer);

	@Override
	SqmSelectableNode<T> copy(SqmCopyContext context);

	default @Nullable Integer getTupleLength() {
		final SqmBindableType<T> nodeType = getExpressible();
		final SqmDomainType<T> sqmType = nodeType == null ? null : nodeType.getSqmType();
		return sqmType == null ? 1 : sqmType.getTupleLength();
	}
}
