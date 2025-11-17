/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.query.sqm.internal.SqmUtil.determineAffectedTableName;

/**
 * @author Andrea Boriero
 */
public class NonAggregatedCompositeValuedPathInterpretation<T>
		extends AbstractSqmPathInterpretation<T>
		implements SqlTupleContainer {

	public static <T> NonAggregatedCompositeValuedPathInterpretation<T> from(
			NonAggregatedCompositeSimplePath<T> sqmPath,
			SqmToSqlAstConverter converter,
			SqmToSqlAstConverter sqlAstCreationState) {
		final TableGroup tableGroup =
				sqlAstCreationState.getFromClauseAccess()
						.findTableGroup( sqmPath.getLhs().getNavigablePath() );
		final NonAggregatedIdentifierMapping mapping =
				(NonAggregatedIdentifierMapping)
						tableGroup.getModelPart()
								.findSubPart( sqmPath.getReferencedPathSource().getPathName(), null );

		return new NonAggregatedCompositeValuedPathInterpretation<>(
				mapping.toSqlExpression(
						tableGroup,
						converter.getCurrentClauseStack().getCurrent(),
						converter,
						converter
				),
				sqmPath.getNavigablePath(),
				mapping,
				tableGroup,
				determineAffectedTableName( tableGroup, mapping )
		);
	}

	private final SqlTuple sqlExpression;
	private final @Nullable String affectedTableName;

	private NonAggregatedCompositeValuedPathInterpretation(
			SqlTuple sqlExpression,
			NavigablePath navigablePath,
			NonAggregatedIdentifierMapping mapping,
			TableGroup tableGroup,
			@Nullable String affectedTableName) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
		this.affectedTableName = affectedTableName;
	}

	@Override
	public SqlTuple getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public @Nullable String getAffectedTableName() {
		return affectedTableName;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

	@Override
	public SqlTuple getSqlTuple() {
		return sqlExpression;
	}
}
