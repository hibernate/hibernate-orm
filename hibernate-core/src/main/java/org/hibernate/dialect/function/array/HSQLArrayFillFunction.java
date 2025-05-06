/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Implement the array fill function by using {@code sequence_array}.
 */
public class HSQLArrayFillFunction extends AbstractArrayFillFunction {

	public HSQLArrayFillFunction(boolean list) {
		super( list );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.append( "coalesce(case when " );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( "<>0 then (select array_agg(" );
		walker.render( sqlAstArguments.get( 0 ), SqlAstNodeRenderingMode.NO_UNTYPED );
		sqlAppender.append( ") from unnest(sequence_array(1," );
		sqlAstArguments.get( 1 ).accept( walker );
		sqlAppender.append( ",1))) end,array[])" );
	}
}
