/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.FromClause;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;

/**
 * todo (6.0) : potentially have QuerySpec/SelectClause be the thing that tracks SqlSelections in terms of uniquing them
 * 		this would most likely require access to the QuerySpec/SelectClause when
 * 		"resolving" the SqlSelection.	Could also manage the "virtual selection"
 * 		todo discussion in SqlSelection
 *
 * @author Steve Ebersole
 */
public class QuerySpec implements SqlAstNode {
	private final boolean isRoot;

	private final FromClause fromClause = new FromClause();
	private final SelectClause selectClause = new SelectClause();

	private Predicate whereClauseRestrictions;
	private List<SortSpecification> sortSpecifications;
	private Expression limitClauseExpression;
	private Expression offsetClauseExpression;

	public QuerySpec(boolean isRoot) {
		this.isRoot = isRoot;
	}

	/**
	 * Does this QuerySpec map to the statement's root query (as
	 * opposed to one of its sub-queries)?
	 */
	public boolean isRoot() {
		return isRoot;
	}

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
			throw new UnsupportedOperationException( "Cannot set where-clause restrictions after already set; try #addRestriction" );
		}
		this.whereClauseRestrictions = whereClauseRestrictions;
	}

	public void addRestriction(Predicate predicate) {
		if ( whereClauseRestrictions == null ) {
			whereClauseRestrictions = predicate;
		}
		else if ( whereClauseRestrictions instanceof Junction
				&& ( (Junction) whereClauseRestrictions ).getNature() == Junction.Nature.CONJUNCTION ) {
			( (Junction) whereClauseRestrictions ).add( predicate );
		}
		else {
			final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
			conjunction.add( whereClauseRestrictions );
			conjunction.add( predicate );
			whereClauseRestrictions = conjunction;
		}
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

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitQuerySpec( this );
	}
}
