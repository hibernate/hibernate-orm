/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Oracle array_sort function.
 */
public class OracleArraySortFunction extends AbstractArraySortFunction {

	public OracleArraySortFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final String arrayTypeName = DdlTypeHelper.getTypeName(
				arrayExpression.getExpressionType(),
				walker.getSessionFactory().getTypeConfiguration()
		);

		sqlAppender.append( arrayTypeName );
		sqlAppender.append( "_sort(" );
		arrayExpression.accept( walker );

		if ( sqlAstArguments.size() > 1 ) {
			sqlAppender.append( ',' );
			final Expression descNode = (Expression) sqlAstArguments.get( 1 );
			sqlAppender.append( "case when " );
			descNode.accept( walker );
			sqlAppender.append( '=' );
			var sessionFactory = walker.getSessionFactory();
			castNonNull( descNode.getExpressionType() ).getSingleJdbcMapping().getJdbcLiteralFormatter()
					.appendJdbcLiteral(
							sqlAppender,
							true,
							sessionFactory.getJdbcServices().getDialect(),
							sessionFactory.getWrapperOptions()
					);
			sqlAppender.append( " then 1 else 0 end" );
			if ( sqlAstArguments.size() > 2 ) {
				sqlAppender.append( ",case when " );
				final Expression nullsNode = (Expression) sqlAstArguments.get( 2 );
				nullsNode.accept( walker );
				sqlAppender.append( '=' );
				castNonNull( nullsNode.getExpressionType() ).getSingleJdbcMapping().getJdbcLiteralFormatter()
						.appendJdbcLiteral(
								sqlAppender,
								true,
								sessionFactory.getJdbcServices().getDialect(),
								sessionFactory.getWrapperOptions()
						);
				sqlAppender.append( " then 1 else 0 end" );
			}
		}
		sqlAppender.append( ')' );
	}
}
