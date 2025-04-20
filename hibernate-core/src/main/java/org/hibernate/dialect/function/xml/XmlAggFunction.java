/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.function.json.ExpressionTypeHelper;
import org.hibernate.metamodel.model.domain.ReturnableType;
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
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.XML;

/**
 * Standard xmlagg function.
 */
public class XmlAggFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public XmlAggFunction(TypeConfiguration typeConfiguration) {
		super(
				"xmlagg",
				FunctionKind.ORDERED_SET_AGGREGATE,
				StandardArgumentsValidators.composite(
						new ArgumentTypesValidator( null, XML )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML )
				),
				StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, XML )
		);
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
		sqlAppender.appendSql( "xmlagg(" );
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		final boolean needsCast = !ExpressionTypeHelper.isXml( arg );
		if ( needsCast ) {
			sqlAppender.appendSql( "cast(" );
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			arg.accept( translator );
			sqlAppender.appendSql( " else null end" );
			translator.getCurrentClauseStack().pop();
		}
		else {
			arg.accept( translator );
		}
		if ( needsCast ) {
			sqlAppender.appendSql( " as xml)" );
		}
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			sqlAppender.appendSql( " order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			translator.getCurrentClauseStack().pop();
		}
		sqlAppender.appendSql( ')' );
		if ( !caseWrapper && filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
	}
}
