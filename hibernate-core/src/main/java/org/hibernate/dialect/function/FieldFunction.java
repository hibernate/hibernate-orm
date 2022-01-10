/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

public class FieldFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public FieldFunction(TypeConfiguration typeConfiguration) {
		super(
				"field",
				StandardArgumentsValidators.min( 2 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "field(" );
		sqlAstArguments.get( 0 ).accept( walker );
		for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
			sqlAppender.appendSql( ',' );

			final SqlAstNode argument = sqlAstArguments.get( i );
			final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple( argument );
			if ( sqlTuple != null ) {
				final List<? extends Expression> expressions = sqlTuple.getExpressions();
				for ( int j = 0; j < expressions.size(); j++ ) {
					if ( j != 0 ) {
						sqlAppender.appendSql( ',' );
					}
					expressions.get( j ).accept( walker );
				}
			}
			else {
				argument.accept( walker );
			}
		}
		sqlAppender.appendSql( ")" );
	}
}
