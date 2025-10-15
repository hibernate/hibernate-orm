/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import java.util.List;
import java.util.Map;

/**
 * @since 7.1
 */
public record CacheableSqmInterpretation<S extends Statement, J extends JdbcOperation>(
		S statement,
		J jdbcOperation,
		Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref,
		Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes) {

}
