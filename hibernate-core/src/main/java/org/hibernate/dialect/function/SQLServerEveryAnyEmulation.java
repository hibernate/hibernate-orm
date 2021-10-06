/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Jan Schatteman
 */
public class SQLServerEveryAnyEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean every;

	public SQLServerEveryAnyEmulation(TypeConfiguration typeConfiguration, boolean every) {
		super(
				every ? "every" : "any",
				FunctionKind.AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN )
				)
		);
		this.every = every;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		if ( every ) {
			sqlAppender.appendSql( "min(iif(" );
		}
		else {
			sqlAppender.appendSql( "max(iif(" );
		}
		if ( filter != null ) {
			filter.accept( walker );
			sqlAppender.appendSql( ",iif(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",1,0),null))" );
		}
		else {
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",1,0))" );
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender, List<SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		this.render( sqlAppender, sqlAstArguments, null, walker );
	}
}
