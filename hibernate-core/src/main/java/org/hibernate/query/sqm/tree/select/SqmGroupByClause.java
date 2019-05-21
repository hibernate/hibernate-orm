/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Models the GROUP-BY clause of a SqmQuerySpec
 *
 * @author Steve Ebersole
 */
public class SqmGroupByClause {
	private List<SqmGrouping> groupings;

	public List<SqmGrouping> getGroupings() {
		return groupings;
	}

	public void setGroupings(List<SqmGrouping> groupings) {
		this.groupings = groupings;
	}

	public void addGrouping(SqmGrouping grouping) {
		if ( groupings == null ) {
			groupings = new ArrayList<>();
		}

		groupings.add( grouping );
	}

	public void addGrouping(SqmExpression groupExpression) {
		addGrouping( new SqmGrouping( groupExpression, null ) );
	}

	public void addGrouping(SqmExpression groupExpression, String collation) {
		addGrouping( new SqmGrouping( groupExpression, collation ) );
	}

	public void visitGroupings(Consumer<SqmGrouping> consumer) {
		if ( groupings != null ) {
			groupings.forEach( consumer );
		}
	}

	public void clearGroupings() {
		if ( groupings != null ) {
			groupings.clear();
		}
	}

	public static class SqmGrouping {
		private SqmExpression expression;
		// todo (6.0) : special type besides String?
		private String collation;

		public SqmGrouping() {
		}

		public SqmGrouping(SqmExpression expression, String collation) {
			this.expression = expression;
			this.collation = collation;
		}

		public SqmExpression getExpression() {
			return expression;
		}

		public void setExpression(SqmExpression expression) {
			this.expression = expression;
		}

		public String getCollation() {
			return collation;
		}

		public void setCollation(String collation) {
			this.collation = collation;
		}
	}
}
