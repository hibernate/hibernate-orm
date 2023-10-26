/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.BOOLEAN;

/**
 * Most databases don't have a function like {@code every()} or {@code any()}.
 * On many platforms we emulate the function using {@code sum()} together with
 * {@code case}.
 *
 * @see MinMaxCaseEveryAnyEmulation
 * @see SQLServerEveryAnyEmulation
 *
 * @author Jan Schatteman
 */
public class EveryAnyEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean every;
	private final QueryLiteral<Boolean> trueLiteral;
	private final QueryLiteral<Boolean> falseLiteral;

	public EveryAnyEmulation(TypeConfiguration typeConfiguration, boolean every, boolean supportsPredicateAsExpression) {
		super(
				every ? "every" : "any",
				FunctionKind.AGGREGATE,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 1 ), BOOLEAN ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, BOOLEAN )
		);
		this.every = every;
		if ( supportsPredicateAsExpression ) {
			this.trueLiteral = null;
			this.falseLiteral = null;
		}
		else {
			final BasicType<Boolean> booleanBasicType = typeConfiguration.getBasicTypeRegistry()
					.resolve( StandardBasicTypes.BOOLEAN );
			this.trueLiteral = new QueryLiteral<>( true, booleanBasicType );
			this.falseLiteral = new QueryLiteral<>( false, booleanBasicType );
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( trueLiteral != null ) {
			sqlAppender.appendSql( "case when " );
		}
		sqlAppender.appendSql( "(sum(case when " );
		if ( filter != null ) {
			walker.getCurrentClauseStack().push( Clause.WHERE );
			filter.accept( walker );
			walker.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then case when " );
			sqlAstArguments.get( 0 ).accept( walker );
			if ( every ) {
				sqlAppender.appendSql( " then 0 else 1 end else null end)=0)" );
			}
			else {
				sqlAppender.appendSql( " then 1 else 0 end else null end)>0)" );
			}
		}
		else {
			sqlAstArguments.get( 0 ).accept( walker );
			if ( every ) {
				sqlAppender.appendSql( " then 0 else 1 end)=0)" );
			}
			else {
				sqlAppender.appendSql( " then 1 else 0 end)>0)" );
			}
		}
		if ( trueLiteral != null ) {
			sqlAppender.appendSql( " then " );
			walker.visitQueryLiteral( trueLiteral );
			sqlAppender.appendSql( " else " );
			walker.visitQueryLiteral( falseLiteral );
			sqlAppender.appendSql( " end" );
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		this.render( sqlAppender, sqlAstArguments, null, returnType, walker );
	}
}
