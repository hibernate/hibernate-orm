/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.SortDirection;
import org.hibernate.query.sqm.function.SelfRenderingWindowFunctionSqlAstExpression;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicType;

import static java.util.Collections.emptyList;

/**
 * @author Christian Beikov
 */
public final class SqmInsertStrategyHelper {

	private SqmInsertStrategyHelper() {
	}

	/**
	 * Creates a row numbering expression, that can be added to the select clause of the query spec.
	 */
	public static Expression createRowNumberingExpression(
			QuerySpec querySpec,
			SessionFactoryImplementor sessionFactory) {
		final BasicType<Integer> resultType = sessionFactory.getTypeConfiguration()
				.getBasicTypeForJavaType( Integer.class );
		final Expression functionExpression;
		final List<SortSpecification> orderList;
		if ( querySpec.getSelectClause().isDistinct() ) {
			assert sessionFactory.getJdbcServices().getDialect().supportsWindowFunctions();
			functionExpression = new SelfRenderingWindowFunctionSqlAstExpression<>(
					"dense_rank",
					(appender, args, returnType, walker) -> appender.appendSql( "dense_rank()" ),
					emptyList(),
					null,
					null,
					null,
					resultType,
					resultType
			);
			final List<SortSpecification> sortSpecifications = querySpec.getSortSpecifications();
			final List<SqlSelection> sqlSelections = querySpec.getSelectClause().getSqlSelections();
			if ( sortSpecifications == null ) {
				orderList = new ArrayList<>( sqlSelections.size() );
			}
			else {
				orderList = new ArrayList<>( sortSpecifications.size() + sqlSelections.size() );
				orderList.addAll( sortSpecifications );
			}
			for ( SqlSelection sqlSelection : sqlSelections ) {
				if ( containsSelectionExpression( orderList, sqlSelection ) ) {
					continue;
				}
				orderList.add( new SortSpecification( sqlSelection.getExpression(), SortDirection.ASCENDING ) );
			}
		}
		else {
			functionExpression = new SelfRenderingWindowFunctionSqlAstExpression<>(
					"row_number",
					(appender, args, returnType, walker) -> appender.appendSql( "row_number()" ),
					emptyList(),
					null,
					null,
					null,
					resultType,
					resultType
			);
			orderList = emptyList();
		}
		return new Over<>( functionExpression, emptyList(), orderList );
	}

	private static boolean containsSelectionExpression(List<SortSpecification> orderList, SqlSelection sqlSelection) {
		final Expression expression = sqlSelection.getExpression();
		for ( SortSpecification sortSpecification : orderList ) {
			final Expression sortExpression = sortSpecification.getSortExpression();
			if ( sortExpression == expression
				|| sortExpression instanceof SqlSelectionExpression sqlSelectionExpression
						&& sqlSelectionExpression.getSelection() == sqlSelection ) {
				return true;
			}
		}
		return false;
	}
}
