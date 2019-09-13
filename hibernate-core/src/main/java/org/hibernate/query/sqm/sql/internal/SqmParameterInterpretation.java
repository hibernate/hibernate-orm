/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.internal.DomainModelHelper;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmParameterInterpretation implements SqmExpressionInterpretation {
	private final SqmParameter sqmParameter;
	private final List<JdbcParameter> jdbcParameters;
	private final MappingModelExpressable valueMapping;

	public SqmParameterInterpretation(
			SqmParameter sqmParameter,
			List<JdbcParameter> jdbcParameters,
			MappingModelExpressable valueMapping) {
		this.valueMapping = valueMapping;
		assert jdbcParameters != null;
		assert jdbcParameters.size() > 0;

		this.sqmParameter = sqmParameter;
		this.jdbcParameters = jdbcParameters;
	}

	@Override
	public SqmExpressable getExpressableType() {
		return sqmParameter.getExpressableType();
	}

	@Override
	public Expression toSqlExpression(
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		if ( jdbcParameters.size() == 1 ) {
			return jdbcParameters.get( 0 );
		}

		return new SqlTuple( jdbcParameters, valueMapping );
	}

	@Override
	public DomainResultProducer getDomainResultProducer(
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		throw new SemanticException( "SqmParameter parameter cannot be a DomainResult" );
	}
}
