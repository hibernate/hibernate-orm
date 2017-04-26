/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.select;

import java.util.List;
import javax.persistence.criteria.Selection;

/**
 * @author Steve Ebersole
 */
public interface JpaSimpleSelection<X> extends JpaSelection<X> {
	@Override
	default boolean isCompoundSelection() {
		return false;
	}

	default List<Selection<?>> getCompoundSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}
}
