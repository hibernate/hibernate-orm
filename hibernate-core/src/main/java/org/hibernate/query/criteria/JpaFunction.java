/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
}
