/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlParameterInfo;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Information obtained from the interpretation of an SqmStatement
 *
 * @author Steve Ebersole
 */
public interface SqmTranslation<T extends Statement> {
	T getSqlAst();
	SqlExpressionResolver getSqlExpressionResolver();
	FromClauseAccess getFromClauseAccess();
	Map<SqmParameter<?>, List<List<JdbcParameter>>> getJdbcParamsBySqmParam();
	Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelTypeResolutions();

	/**
	 * The parameter information for the SQL AST.
	 *
	 * @since 7.1
	 */
	SqlParameterInfo getParameterInfo();
}
