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

/**
 * HSQLDB array_set function.
 */
public class HSQLArraySetFunction extends ArraySetUnnestFunction {

	public HSQLArraySetFunction() {
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression indexExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression elementExpression = (Expression) sqlAstArguments.get( 2 );
		sqlAppender.append( "(select array_agg(case when i.idx=");
		indexExpression.accept( walker );
		sqlAppender.append(" then " );
		elementExpression.accept( walker );
		sqlAppender.append(" else t.val end) from unnest(sequence_array(1,greatest(cardinality(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ")," );
		indexExpression.accept( walker );
		sqlAppender.append( "),1)) i(idx) left join unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") with ordinality t(val, idx) on i.idx=t.idx)" );
	}
}
