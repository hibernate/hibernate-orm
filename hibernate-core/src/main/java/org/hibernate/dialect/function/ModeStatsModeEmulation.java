/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

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
			SqlAstTranslator<?> translator) {
		final boolean caseWrapper = filter != null && !translator.supportsFilterClause();
		sqlAppender.appendSql( "stats_mode(" );
		if ( withinGroup == null || withinGroup.size() != 1 ) {
			throw new IllegalArgumentException( "MODE function requires a WITHIN GROUP clause with exactly one order by item!" );
		}
		if ( caseWrapper ) {
			sqlAppender.appendSql( "case when " );
			filter.accept( translator );
			sqlAppender.appendSql( " then " );
			withinGroup.get( 0 ).accept( translator );
			sqlAppender.appendSql( " else null end)" );
		}
		else {
			withinGroup.get( 0 ).accept( translator );
			sqlAppender.appendSql( ')' );
			if ( filter != null ) {
				sqlAppender.appendSql( " filter (where " );
				filter.accept( translator );
				sqlAppender.appendSql( ')' );
			}
		}
	}

}
