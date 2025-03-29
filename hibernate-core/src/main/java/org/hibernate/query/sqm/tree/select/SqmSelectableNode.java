/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.function.Consumer;
import jakarta.persistence.criteria.Selection;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

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

	default Integer getTupleLength() {
		final DomainType<T> sqmType = getNodeType() == null ? null : getNodeType().getSqmType();
		return sqmType == null ? 1 : sqmType.getTupleLength();
	}
}
