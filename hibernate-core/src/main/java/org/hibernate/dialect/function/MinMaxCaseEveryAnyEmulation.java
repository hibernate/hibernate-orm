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
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Most databases don't have a function like {@code every()} or {@code any()}.
 * On some platforms we emulate the function using {@code min()} or {@code max()}
 * together with {@code case}.
 *
 * @see EveryAnyEmulation
 *
 * @author Jan Schatteman
 */
public class MinMaxCaseEveryAnyEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean every;

	public MinMaxCaseEveryAnyEmulation(TypeConfiguration typeConfiguration, boolean every) {
		super(
				every ? StandardFunctions.EVERY : StandardFunctions.ANY,
				FunctionKind.AGGREGATE,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), FunctionParameterType.BOOLEAN ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, FunctionParameterType.BOOLEAN )
		);
		this.every = every;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		if ( every ) {
			sqlAppender.appendSql( "min(case when " );
		}
		else {
			sqlAppender.appendSql( "max(case when " );
		}
		if ( filter != null ) {
			walker.getCurrentClauseStack().push( Clause.WHERE );
			filter.accept( walker );
			walker.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then case when " );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " then 1 else 0 end else null end)" );
		}
		else {
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( " then 1 else 0 end)" );
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		this.render( sqlAppender, sqlAstArguments, null, walker );
	}
}
