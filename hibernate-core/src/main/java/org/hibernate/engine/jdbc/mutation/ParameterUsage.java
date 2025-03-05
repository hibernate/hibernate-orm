/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation;

/**
 * Describes how a parameter is used in a mutation statement
 *
 * @author Steve Ebersole
 */
public enum ParameterUsage {
	/**
	 * The parameter is used in the update set clause or insert values clause
	 */
	SET,

	/**
	 * The parameter is used in the where clause
	 */
	RESTRICT
}
