/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.SelfRenderingOrderedSetAggregateFunctionSqlAstExpression;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * HSQLDB has a special syntax.
 */
public class HSQLArrayToStringFunction extends ArrayToStringFunction {

	public HSQLArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression arrayExpression = (Expression) sqlAstArguments.get( 0 );
		final Expression separatorExpression = (Expression) sqlAstArguments.get( 1 );
		final Expression defaultExpression = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) arrayExpression.getExpressionType().getSingleJdbcMapping();
		final int ddlTypeCode = pluralType.getElementType().getJdbcType().getDdlTypeCode();
		final boolean needsCast = !SqlTypes.isStringType( ddlTypeCode );
		if ( arrayExpression instanceof SelfRenderingOrderedSetAggregateFunctionSqlAstExpression
				&& ArrayAggFunction.FUNCTION_NAME.equals( ( (FunctionExpression) arrayExpression ).getFunctionName() ) ) {
			final SelfRenderingOrderedSetAggregateFunctionSqlAstExpression functionExpression
					= (SelfRenderingOrderedSetAggregateFunctionSqlAstExpression) arrayExpression;
			// When the array argument is an aggregate expression, we access its contents directly
			final Expression arrayElementExpression = (Expression) functionExpression.getArguments().get( 0 );
			final List<SortSpecification> withinGroup = functionExpression.getWithinGroup();
			final Predicate filter = functionExpression.getFilter();

			sqlAppender.append( "group_concat(" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			if ( needsCast ) {
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					// By default, HSQLDB uses upper case, so lower it for a consistent experience
					sqlAppender.append( "lower(" );
				}
				sqlAppender.append( "cast(" );
			}
			arrayElementExpression.accept( walker );
			if ( needsCast ) {
				sqlAppender.append( " as longvarchar)" );
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					sqlAppender.append( ')' );
				}
			}
			if ( defaultExpression != null ) {
				sqlAppender.append( "," );
				defaultExpression.accept( walker );
				sqlAppender.append( ")" );
			}

			if ( withinGroup != null && !withinGroup.isEmpty() ) {
				walker.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
				sqlAppender.appendSql( " order by " );
				withinGroup.get( 0 ).accept( walker );
				for ( int i = 1; i < withinGroup.size(); i++ ) {
					sqlAppender.appendSql( ',' );
					withinGroup.get( i ).accept( walker );
				}
				walker.getCurrentClauseStack().pop();
			}
			sqlAppender.append( " separator " );
			// HSQLDB doesn't like non-literals as separator
			walker.render( separatorExpression, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
			sqlAppender.appendSql( ')' );
			if ( filter != null ) {
				walker.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( walker );
				sqlAppender.appendSql( ')' );
				walker.getCurrentClauseStack().pop();
			}
		}
		else {
			sqlAppender.append( "case when " );
			arrayExpression.accept( walker );
			sqlAppender.append( " is not null then coalesce((select group_concat(" );
			if ( defaultExpression != null ) {
				sqlAppender.append( "coalesce(" );
			}
			if ( needsCast ) {
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					// By default, HSQLDB uses upper case, so lower it for a consistent experience
					sqlAppender.append( "lower(" );
				}
				sqlAppender.append( "cast(" );
			}
			sqlAppender.append( "t.val" );
			if ( needsCast ) {
				sqlAppender.append( " as longvarchar)" );
				if ( ddlTypeCode == SqlTypes.BOOLEAN ) {
					sqlAppender.append( ')' );
				}
			}
			if ( defaultExpression != null ) {
				sqlAppender.append( "," );
				defaultExpression.accept( walker );
				sqlAppender.append( ")" );
			}
			sqlAppender.append( " order by t.idx separator " );
			// HSQLDB doesn't like non-literals as separator
			walker.render( separatorExpression, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
			sqlAppender.append( ") from unnest(" );
			arrayExpression.accept( walker );
			sqlAppender.append( ") with ordinality t(val,idx)),'') end" );
		}
	}
}
