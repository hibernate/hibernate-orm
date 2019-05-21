/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * Models the HAVING clause of a SqmQuerySpec
 *
 * @author Steve Ebersole
 */
public class SqmHavingClause {
	private SqmPredicate predicate;

	public SqmHavingClause() {
	}

	public SqmHavingClause(SqmPredicate predicate) {
		this.predicate = predicate;
	}

	public SqmPredicate getPredicate() {
		return predicate;
	}

	public void setPredicate(SqmPredicate predicate) {
		this.predicate = predicate;
	}
}
