/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Represents an individual selection within a select clause.
 *
 * @author Steve Ebersole
 */
public class SqmSelection implements SqmAliasedExpression {
	private final SqmExpression selectExpression;
	private final String alias;

	public SqmSelection(SqmExpression selectExpression, String alias) {
		this.selectExpression = selectExpression;
		this.alias = alias;
	}

	public SqmSelection(SqmExpression selectExpression) {
		this( selectExpression, null );
	}

	@Override
	public SqmExpression getExpression() {
		return selectExpression;
	}

	@Override
	public String getAlias() {
		return alias;
	}
}
