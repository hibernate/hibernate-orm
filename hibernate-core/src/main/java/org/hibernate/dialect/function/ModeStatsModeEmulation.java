/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class ModeStatsModeEmulation extends InverseDistributionFunction {

	public static final String FUNCTION_NAME = "mode";

	public ModeStatsModeEmulation(TypeConfiguration typeConfiguration) {
		super(
				FUNCTION_NAME,
				null,
				typeConfiguration
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !filterClauseSupported( translator );
		sqlAppender.appendSql( "stats_mode(" );
		if ( withinGroup == null || withinGroup.size() != 1 ) {
			throw new IllegalArgumentException( "MODE function requires a WITHIN GROUP clause with exactly one order by item" );
		}
		if ( caseWrapper ) {
			translator.getCurrentClauseStack().push( Clause.WHERE );
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( " then " );
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			withinGroup.get( 0 ).accept( translator );
			sqlAppender.appendSql( " else null end)" );
			translator.getCurrentClauseStack().pop();
		}
		else {
			translator.getCurrentClauseStack().push( Clause.WITHIN_GROUP );
			withinGroup.get( 0 ).accept( translator );
			translator.getCurrentClauseStack().pop();
			sqlAppender.appendSql( ')' );
			if ( filter != null ) {
				translator.getCurrentClauseStack().push( Clause.WHERE );
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				sqlAppender.appendSql( ')' );
				translator.getCurrentClauseStack().pop();
			}
		}
	}

}
