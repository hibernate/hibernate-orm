/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

public class STRelateFunction extends OracleSpatialFunction {

	public STRelateFunction(BasicTypeRegistry typeRegistry) {
		super(
				"ST_RELATE",
				false,
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve( StandardBasicTypes.STRING ) )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression geom1 = (Expression) arguments.get( 0 );
		final Expression geom2 = (Expression) arguments.get( 1 );
		sqlAppender.appendSql( "ST_GEOMETRY(" );
		walker.render( geom1, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ").ST_RELATE( ST_GEOMETRY(" );
		walker.render( geom2, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ") , 'DETERMINE' ) " );
	}
}
