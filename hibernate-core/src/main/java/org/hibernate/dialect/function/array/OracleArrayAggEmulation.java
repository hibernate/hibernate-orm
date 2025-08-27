/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;

/**
 * @author Christian Beikov
 */
public class OracleArrayAggEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	public static final String FUNCTION_NAME = "array_agg";

	public OracleArrayAggEmulation() {
		super(
				FUNCTION_NAME,
				FunctionKind.ORDERED_SET_AGGREGATE,
				StandardArgumentsValidators.exactly( 1 ),
				JsonArrayViaElementArgumentReturnTypeResolver.INSTANCE,
				StandardFunctionArgumentTypeResolvers.NULL
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
		if ( !( returnType instanceof BasicPluralType<?, ?> pluralType ) ) {
			throw new SemanticException(
					"Oracle array_agg emulation requires a basic plural return type, but resolved return type was: " + returnType
			);
		}
		final boolean returnJson = pluralType.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.JSON_ARRAY;
		if ( returnJson ) {
			sqlAppender.append( "json_arrayagg(" );
		}
		else {
			final String arrayTypeName = DdlTypeHelper.getTypeName(
					returnType,
					translator.getSessionFactory().getTypeConfiguration()
			);
			sqlAppender.append( arrayTypeName );
			sqlAppender.append( "_from_json(json_arrayagg(" );
		}
		final SqlAstNode firstArg = sqlAstArguments.get( 0 );
		final Expression arg;
		if ( firstArg instanceof Distinct distinct ) {
			sqlAppender.appendSql( "distinct " );
			arg = distinct.getExpression();
		}
		else {
			arg = (Expression) firstArg;
		}
		arg.accept( translator );
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
		sqlAppender.appendSql( " null on null returning " );
		sqlAppender.appendSql(
				translator.getSessionFactory().getTypeConfiguration().getDdlTypeRegistry()
						.getTypeName( SqlTypes.JSON, translator.getSessionFactory().getJdbcServices().getDialect() )
		);
		sqlAppender.appendSql( ')' );
		if ( filter != null ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
			translator.getCurrentClauseStack().pop();
		}
		if ( !returnJson ) {
			sqlAppender.appendSql( ')' );
		}
	}

}
