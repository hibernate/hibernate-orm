/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.query.TemporalUnit;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A duration expressed in terms of a given temporal unit.
 * Represents a conversion of a scalar value to a Duration,
 * by providing a duration unit.
 *
 * @see Conversion which does the opposite
 *
 * @author Gavin King
 */
public class Duration
		implements Expression, SqlExpressable, DomainResultProducer {
	private Expression magnitude;
	private final TemporalUnit unit;
	private final SqlExpressableType type;

	public Duration(
			Expression magnitude,
			TemporalUnit unit,
			SqlExpressableType type) {
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
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getType()
		);
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return new BasicResultImpl(
				resultVariable,
				creationState.getSqlExpressionResolver().resolveSqlSelection(
						this,
						getType().getJavaTypeDescriptor(),
						creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
				),
				getType()
		);
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}
}
