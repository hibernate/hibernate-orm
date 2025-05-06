/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.CastType;
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
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * @author Christian Beikov
 */
public class ListaggStringAggEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	public static final String FUNCTION_NAME = "listagg";

	private final String functionName;
	private final String stringType;
	private final boolean withinGroupClause;

	public ListaggStringAggEmulation(
			String functionName,
			String stringType,
			boolean withinGroupClause,
			TypeConfiguration typeConfiguration) {
		super(
				FUNCTION_NAME,
				FunctionKind.ORDERED_SET_AGGREGATE,
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), STRING, STRING ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, STRING )
		);
		this.functionName = functionName;
		this.stringType = stringType;
		this.withinGroupClause = withinGroupClause;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, Collections.emptyList(), returnType, walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.getSessionFactory().getJdbcServices().getDialect().supportsFilterClause();
		sqlAppender.appendSql( functionName );
		sqlAppender.appendSql( '(' );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			renderAsString( sqlAppender, translator, arg );
			sqlAppender.appendSql( " else null end" );
			translator.getCurrentClauseStack().pop();
		}
		else {
			renderAsString( sqlAppender, translator, arg );
		}
		if ( sqlAstArguments.size() != 1 ) {
			SqlAstNode separator = sqlAstArguments.get( 1 );
			// string_agg doesn't support the overflow clause, so we just omit it
			if ( separator instanceof Overflow overflow ) {
				separator = overflow.getSeparatorExpression();
			}
			sqlAppender.appendSql( ',' );
			separator.accept( translator );
			if ( !withinGroupClause && withinGroup != null && !withinGroup.isEmpty() ) {
				translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
				sqlAppender.appendSql( " order by " );
				withinGroup.get( 0 ).accept( translator );
				for ( int i = 1; i < withinGroup.size(); i++ ) {
					sqlAppender.appendSql( ',' );
					withinGroup.get( i ).accept( translator );
				}
				translator.getCurrentClauseStack().pop();
			}
		}
		sqlAppender.appendSql( ')' );
		if ( withinGroupClause && withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " within group (order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
		if ( !caseWrapper && filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}

	private void renderAsString(SqlAppender sqlAppender, SqlAstTranslator<?> translator, Expression expression) {
		final JdbcMapping sourceMapping = expression.getExpressionType().getSingleJdbcMapping();
		// No need to cast if we already have a string
		if ( sourceMapping.getCastType() == CastType.STRING ) {
			expression.accept( translator );
		}
		else {
			sqlAppender.appendSql( "cast(" );
			expression.accept( translator );
			sqlAppender.appendSql( " as " );
			sqlAppender.appendSql( stringType );
			sqlAppender.appendSql( ')' );
		}
	}

}
