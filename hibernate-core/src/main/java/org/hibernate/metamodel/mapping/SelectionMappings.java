/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.ArrayList;
import java.util.List;

/**
 * A container for multiple column mappings.
 *
 * @author Christian Beikov
 */
public interface SelectionMappings {
	SelectionMapping getSelectionMapping(int columnIndex);

	int getJdbcTypeCount();

	int forEachSelection(int offset, SelectionConsumer consumer);

	default int forEachSelection(SelectionConsumer consumer) {
		return forEachSelection( 0, consumer );
	}

	default List<JdbcMapping> getJdbcMappings() {
		final List<JdbcMapping> results = new ArrayList<>();
		forEachSelection( (index, selection) -> results.add( selection.getJdbcMapping() ) );
		return results;
	}
}
