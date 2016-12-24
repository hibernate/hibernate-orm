/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.from.FromClause;
import org.hibernate.sql.ast.predicate.Predicate;
import org.hibernate.sql.ast.select.SelectClause;

/**
 * @author Steve Ebersole
 */
public class QuerySpec {
	private final FromClause fromClause = new FromClause();
	private final SelectClause selectClause = new SelectClause();

	private Predicate whereClauseRestrictions;

	// where clause, etc

	public FromClause getFromClause() {
		return fromClause;
	}

	public SelectClause getSelectClause() {
		return selectClause;
	}

	public Predicate getWhereClauseRestrictions() {
		return whereClauseRestrictions;
	}

	public void setWhereClauseRestrictions(Predicate whereClauseRestrictions) {
		if ( this.whereClauseRestrictions != null ) {
			throw new UnsupportedOperationException( "Cannot set where-clause restrictions after already set" );
		}
		this.whereClauseRestrictions = whereClauseRestrictions;
	}
}
