/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * A duration expressed in terms of a given temporal unit.
 * Represents a conversion of a scalar value to a Duration,
 * by providing a duration unit.
 *
 * @see Conversion which does the opposite
 *
 * @author Gavin King
 */
public class Duration implements Expression, DomainResultProducer {
	private final Expression magnitude;
	private final TemporalUnit unit;
	private final BasicValuedMapping type;

	public Duration(
			Expression magnitude,
			TemporalUnit unit,
			BasicValuedMapping type) {
		this.magnitude = magnitude;
		this.unit = unit;
		this.type = type;
	}

	public TemporalUnit getUnit() {
		return unit;
	}

	public Expression getMagnitude() {
		return magnitude;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitDuration(this);
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return new BasicResult<>(
				creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
						this,
						type.getJdbcMapping().getJdbcJavaType(),
						null,
						creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
				).getValuesArrayPosition(),
				resultVariable,
				type.getJdbcMapping()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getJdbcMapping().getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public BasicValuedMapping getExpressionType() {
		return type;
	}
}
