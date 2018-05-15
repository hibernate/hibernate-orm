/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.paging;

import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Christian Beikov
 */
public class SqmLimitOffsetClause {
	private final SqmExpression limitExpression;
	private final SqmExpression offsetExpression;

	public SqmLimitOffsetClause(SqmExpression limitExpression, SqmExpression offsetExpression) {
		this.limitExpression = limitExpression;
		this.offsetExpression = offsetExpression;
	}

	public SqmExpression getLimitExpression() {
		return limitExpression;
	}

	public SqmExpression getOffsetExpression() {
		return offsetExpression;
	}
}
