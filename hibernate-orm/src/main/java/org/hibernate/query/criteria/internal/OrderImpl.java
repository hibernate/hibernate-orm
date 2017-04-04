/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;
import java.io.Serializable;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;

/**
 * Represents an <tt>ORDER BY</tt> fragment.
 *
 * @author Steve Ebersole
 */
public class OrderImpl implements Order, Serializable {
	private final Expression<?> expression;
	private boolean ascending;

	public OrderImpl(Expression<?> expression) {
		this( expression, true );
	}

	public OrderImpl(Expression<?> expression, boolean ascending) {
		this.expression = expression;
		this.ascending = ascending;
	}

	public Order reverse() {
		ascending = !ascending;
		return this;
	}

	public boolean isAscending() {
		return ascending;
	}

	public Expression<?> getExpression() {
		return expression;
	}
}
