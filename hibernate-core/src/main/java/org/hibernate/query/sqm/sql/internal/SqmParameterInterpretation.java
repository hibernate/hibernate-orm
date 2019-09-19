/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmParameterInterpretation implements Expression {
	private final SqmParameter sqmParameter;
	private final MappingModelExpressable valueMapping;

	private final Expression resolvedExpression;

	public SqmParameterInterpretation(
			SqmParameter sqmParameter,
			List<JdbcParameter> jdbcParameters,
			MappingModelExpressable valueMapping) {
		this.sqmParameter = sqmParameter;
		this.valueMapping = valueMapping;

		assert jdbcParameters != null;
		assert jdbcParameters.size() > 0;

		this.resolvedExpression = jdbcParameters.size() == 1
				? jdbcParameters.get( 0 )
				: new SqlTuple( jdbcParameters, valueMapping );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		resolvedExpression.accept( sqlTreeWalker );
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}
}
