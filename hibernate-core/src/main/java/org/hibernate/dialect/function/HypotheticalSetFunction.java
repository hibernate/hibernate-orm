/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.Collections;
import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class HypotheticalSetFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public HypotheticalSetFunction(String name, BasicTypeReference<?> returnType, TypeConfiguration typeConfiguration) {
		super(
				name,
				FunctionKind.ORDERED_SET_AGGREGATE,
				null,
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( returnType )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, Collections.emptyList(), walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, filter, Collections.emptyList(), walker );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		sqlAppender.appendSql( getName() );
		sqlAppender.appendSql( '(' );
		if ( !sqlAstArguments.isEmpty() ) {
			if ( caseWrapper ) {
				sqlAppender.appendSql( "case when " );
				filter.accept( translator );
				sqlAppender.appendSql( " then " );
				sqlAstArguments.get( 0 ).accept( translator );
				sqlAppender.appendSql( " else null end" );
			}
			else {
				sqlAstArguments.get( 0 ).accept( translator );
			}
			for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				sqlAstArguments.get( i ).accept( translator );
			}
		}
		else if ( caseWrapper ) {
			throw new IllegalArgumentException( "Can't emulate filter clause for function [" + getName() + "] without arguments!" );
		}
		sqlAppender.appendSql( ')' );
		if ( withinGroup != null && !withinGroup.isEmpty() ) {
			sqlAppender.appendSql( " within group (order by " );
			withinGroup.get( 0 ).accept( translator );
			for ( int i = 1; i < withinGroup.size(); i++ ) {
				sqlAppender.appendSql( ',' );
				withinGroup.get( i ).accept( translator );
			}
			sqlAppender.appendSql( ')' );
		}
		if ( filter != null ) {
			sqlAppender.appendSql( " filter (where " );
			filter.accept( translator );
			sqlAppender.appendSql( ')' );
		}
	}

}
