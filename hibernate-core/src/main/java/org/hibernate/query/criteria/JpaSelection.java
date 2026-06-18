/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Selection;

import static java.util.Collections.unmodifiableList;

/**
 * API extension to the JPA {@link Selection} contract
 *
 * @author Steve Ebersole
 */
public interface JpaSelection<T> extends JpaTupleElement<T>, Selection<T> {
	/**
	 * Return the items of this selection.
	 */
	List<? extends JpaSelection<?>> getSelectionItems();

	/**
	 * Return the compound selection items.
	 */
	@Nonnull
	@Override
	default List<Selection<?>> getCompoundSelectionItems() {
		return unmodifiableList( getSelectionItems() );
	}

	/**
	 * Assign an alias to this selection.
	 */
	@Nonnull
	@Override
	JpaSelection<T> alias(@Nonnull String name);
}
