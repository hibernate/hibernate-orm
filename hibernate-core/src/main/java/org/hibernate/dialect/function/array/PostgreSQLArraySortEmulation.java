/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * PostgreSQL array_sort emulation for versions before 18.
 */
public class PostgreSQLArraySortEmulation extends AbstractArraySortFunction {

	public PostgreSQLArraySortEmulation(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final boolean areArgumentsLiterals =
				( sqlAstArguments.size() <= 1 || sqlAstArguments.get( 1 ) instanceof Literal ) &&
						( sqlAstArguments.size() <= 2 || sqlAstArguments.get( 2 ) instanceof Literal );

		if ( areArgumentsLiterals ) {
			renderWithLiteralArguments( sqlAppender, sqlAstArguments, returnType, walker );
		}
		else {
			renderWithExpressionArguments( sqlAppender, sqlAstArguments, returnType, walker );
		}
	}

	private void renderWithLiteralArguments(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );

		final boolean descending = sqlAstArguments.size() > 1
				&& sqlAstArguments.get( 1 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean
				? (Boolean) literal.getLiteralValue()
				: false;

		final Boolean nullsFirst = sqlAstArguments.size() > 2
				&& sqlAstArguments.get( 2 ) instanceof Literal literal
				&& literal.getLiteralValue() instanceof Boolean
				? (Boolean) literal.getLiteralValue()
				: null;

		final boolean actualNullsFirst = nullsFirst != null ? nullsFirst : descending;

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then " );

		sqlAppender.append( "coalesce((select array_agg(t.val order by t.val" );

		sqlAppender.append( descending
									? ( actualNullsFirst ? " desc nulls first" : " desc nulls last" )
									: ( actualNullsFirst ? " asc nulls first" : " asc nulls last" ) );

		sqlAppender.append( ") from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") t(val))" );

		appendEmptyArrayWithCasting( sqlAppender, returnType, walker );
		sqlAppender.append( " end" );
	}

	private void renderWithExpressionArguments(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		assert sqlAstArguments.size() >= 2;

		final SqlAstNode arrayExpression = sqlAstArguments.get( 0 );
		final SqlAstNode descendingNode = sqlAstArguments.get( 1 );
		final SqlAstNode nullsFirstNode = sqlAstArguments.size() > 2
				? sqlAstArguments.get( 2 )
				: descendingNode;

		sqlAppender.append( "case when " );
		arrayExpression.accept( walker );
		sqlAppender.append( " is not null then " );

		sqlAppender.append( "coalesce((select array_agg(t.val order by " );

		sqlAppender.append( '(' );
		nullsFirstNode.accept( walker );
		sqlAppender.append( "=(t.val is null)) desc," );

		sqlAppender.append( "case when " );
		descendingNode.accept( walker );
		sqlAppender.append( " then t.val end desc," );

		sqlAppender.append( "case when not " );
		descendingNode.accept( walker );
		sqlAppender.append( " then t.val end" );

		sqlAppender.append( ") from unnest(" );
		arrayExpression.accept( walker );
		sqlAppender.append( ") t(val))" );

		appendEmptyArrayWithCasting( sqlAppender, returnType, walker );
		sqlAppender.append( " end" );
	}

	private void appendEmptyArrayWithCasting(
			SqlAppender sqlAppender,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		String arrayTypeName = null;
		if ( returnType instanceof BasicPluralType<?, ?> pluralType ) {
			if ( needsArrayCasting( pluralType.getElementType() ) ) {
				arrayTypeName = DdlTypeHelper.getCastTypeName(
						returnType,
						walker.getSessionFactory().getTypeConfiguration()
				);
			}
		}

		if ( arrayTypeName != null ) {
			sqlAppender.append( ",cast(array[] as " );
			sqlAppender.appendSql( arrayTypeName );
			sqlAppender.appendSql( "))" );
		}
		else {
			sqlAppender.append( ",array[])" );
		}
	}

	private static boolean needsArrayCasting(BasicType<?> elementType) {
		return elementType.getJdbcType().isString();
	}
}
