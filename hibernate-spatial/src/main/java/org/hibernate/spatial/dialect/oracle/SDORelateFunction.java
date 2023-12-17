/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

public class SDORelateFunction extends OracleSpatialFunction {

	final private List<String> masks;

	public SDORelateFunction(List<String> masks, BasicTypeRegistry typeRegistry) {
		super(
				"SDO_GEOM.RELATE",
				false,
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve(
						StandardBasicTypes.BOOLEAN ) )
		);
		this.masks = masks;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		Expression geom1 = (Expression) sqlAstArguments.get( 0 );
		Expression geom2 = (Expression) sqlAstArguments.get( 1 );
		String maskExpression = String.join( "+", masks );
		sqlAppender.appendSql( "CASE " );
		sqlAppender.appendSql( getName() );
		sqlAppender.appendSql( "(" );
		walker.render( geom1, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ", '" );
		sqlAppender.appendSql( maskExpression );
		sqlAppender.appendSql( "', " );
		walker.render( geom2, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ")" );
		sqlAppender.appendSql( " WHEN 'FALSE' THEN 0 ");
		sqlAppender.appendSql( " ELSE 1 " );
		sqlAppender.appendSql( " END" );


	}
}
