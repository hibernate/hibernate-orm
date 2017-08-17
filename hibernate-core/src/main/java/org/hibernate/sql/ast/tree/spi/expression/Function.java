/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * Generalized contract for any type of function reference in the query
 *
 * @author Steve Ebersole
 */
public interface Function extends Expression, SqlExpressable {
	@Override
	default SqlSelection generateSqlSelection(int jdbcResultSetPosition) {
		return new SqlSelectionImpl(
				// todo (6.0) : Need a better link between scalar/basic types and SqlSelectionReader
				// 		for now we assume, essentially, that a function can return only basic types
				( (BasicValuedExpressableType) getType() ).getBasicType().getSqlSelectionReader(),
				jdbcResultSetPosition
		);
	}

	@Override
	AllowableFunctionReturnType getType();


	@Override
	default Expression createSqlExpression() {
		return this;
	}
}
