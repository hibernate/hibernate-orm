/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.criteria.Selection;

/**
 * API extension to the JPA {@link Selection} contract
 *
 * @author Steve Ebersole
 */
public interface JpaSelection<T> extends JpaTupleElement<T>, Selection<T> {
	List<? extends JpaSelection<?>> getSelectionItems();

	@Override
	default List<Selection<?>> getCompoundSelectionItems() {
		return Collections.unmodifiableList( getSelectionItems() );
	}

	@Override
	JpaSelection<T> alias(String name);
}
