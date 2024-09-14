/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
