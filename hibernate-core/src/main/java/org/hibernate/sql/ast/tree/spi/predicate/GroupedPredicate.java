/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.predicate;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class GroupedPredicate implements Predicate {
	private final Predicate subPredicate;

	public GroupedPredicate(Predicate subPredicate) {
		this.subPredicate = subPredicate;
	}

	public Predicate getSubPredicate() {
		return subPredicate;
	}

	@Override
	public boolean isEmpty() {
		return subPredicate.isEmpty();
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitGroupedPredicate( this );
	}
}
