/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.from.FromClause;
import org.hibernate.sql.ast.predicate.Predicate;
import org.hibernate.sql.ast.select.SelectClause;
import org.hibernate.sql.ast.sort.SortSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class QuerySpec {
	private final FromClause fromClause = new FromClause();
	private final SelectClause selectClause = new SelectClause();

	private Predicate whereClauseRestrictions;

	private List<SortSpecification> sortSpecifications;
	private Expression limitClauseExpression;
	private Expression offsetClauseExpression;

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

	public List<SortSpecification> getSortSpecifications() {
		if ( sortSpecifications == null ) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableList( sortSpecifications );
		}
	}

	public void addSortSpecification(SortSpecification sortSpecification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<SortSpecification>();
		}
		sortSpecifications.add( sortSpecification );
	}

	public Expression getLimitClauseExpression() {
		return limitClauseExpression;
	}

	public void setLimitClauseExpression(Expression limitExpression) {
		if ( this.limitClauseExpression != null ) {
			throw new UnsupportedOperationException( "Cannot set limit-clause expression after already set" );
		}
		this.limitClauseExpression = limitExpression;
	}

	public Expression getOffsetClauseExpression() {
		return offsetClauseExpression;
	}

	public void setOffsetClauseExpression(Expression offsetExpression) {
		if ( this.offsetClauseExpression != null ) {
			throw new UnsupportedOperationException( "Cannot set offset-clause expression after already set" );
		}
		this.offsetClauseExpression = offsetExpression;
	}
}
