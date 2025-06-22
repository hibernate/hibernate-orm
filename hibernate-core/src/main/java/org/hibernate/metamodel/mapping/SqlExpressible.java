/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

/**
 * Unifying contract for things that are capable of being an expression in
 * the SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqlExpressible extends JdbcMappingContainer {
	/**
	 * Anything that is expressible at the SQL AST level
	 * would be of basic type.
	 */
	JdbcMapping getJdbcMapping();

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		assert index == 0;
		return getJdbcMapping();
	}

}
