/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

/**
 * Contract for expressions which model a SQL function call.
 *
 * @param <T> The type of the function result.
 *
 * @author Steve Ebersole
 */
public interface JpaFunction<T> extends JpaExpression<T> {
	/**
	 * Retrieve the name of the function.
	 *
	 * @return The function name.
	 */
	String getFunctionName();

	/**
	 * Is this function a value aggregator (like a <tt>COUNT</tt> or <tt>MAX</tt> function e.g.)?
	 *
	 * @return True if this functions does aggregation.
	 */
	boolean isAggregator();
}
