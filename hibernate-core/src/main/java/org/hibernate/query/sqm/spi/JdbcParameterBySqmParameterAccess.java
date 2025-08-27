/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Access to the mapping between an SqmParameter and all of its JDBC parameters
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface JdbcParameterBySqmParameterAccess {
	/**
	 * The mapping between an SqmParameter and all of its JDBC parameters
	 */
	Map<SqmParameter<?>, List<List<JdbcParameter>>> getJdbcParamsBySqmParam();
}
