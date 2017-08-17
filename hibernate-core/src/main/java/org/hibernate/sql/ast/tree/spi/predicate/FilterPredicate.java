/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.predicate;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * Represents a filter applied to an entity/collection.
 * <p/>
 * Note, we do not attempt to parse the filter
 *
 * @author Steve Ebersole
 */
public class FilterPredicate implements Predicate {
	// todo : need to "carry forward" the FilterConfiguration information into the ImprovedEntityPersister so we have access to the alias injections

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitFilterPredicate( this );
	}
}
