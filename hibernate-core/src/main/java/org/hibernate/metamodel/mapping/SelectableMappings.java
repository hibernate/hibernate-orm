/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

/**
 * A container for multiple selectable (column, formula) mappings.
 *
 * @author Christian Beikov
 */
public interface SelectableMappings {
	/**
	 * The number of selectables
	 */
	int getJdbcTypeCount();

	/**
	 * Get the selectable at the given position
	 */
	@Nonnull
	SelectableMapping getSelectable(int columnIndex);

	/**
	 * Visit each contained selectable mapping.
	 *
	 * As the selectables are iterated, we call `SelectionConsumer`
	 * passing along `offset` + our current iteration index.
	 *
	 * The return is the number of selectables we directly contain
	 *
	 * @see SelectableConsumer#accept(int, SelectableMapping)
	 */
	int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer);

	/**
	 * Same as {@link #forEachSelectable(int, SelectableConsumer)}, with
	 * an implicit offset of `0`
	 */
	default int forEachSelectable(@Nonnull SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

}
