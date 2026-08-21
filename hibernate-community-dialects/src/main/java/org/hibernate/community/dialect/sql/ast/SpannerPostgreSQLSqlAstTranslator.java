/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sql.ast;

import org.hibernate.dialect.sql.ast.PostgreSQLSqlAstTranslator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteMaterialization;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.exec.spi.JdbcOperation;

public class SpannerPostgreSQLSqlAstTranslator<T extends JdbcOperation> extends PostgreSQLSqlAstTranslator<T> {

	public SpannerPostgreSQLSqlAstTranslator(
			SessionFactoryImplementor sessionFactory, Statement statement) {
		super(sessionFactory, statement);
	}

	@Override
	protected void renderMaterializationHint(CteMaterialization materialization) {
		// NO-OP
	}

	@Override
	protected void renderLikePredicate(LikePredicate likePredicate) {
		// We need a custom implementation here because Spanner
		// uses the backslash character as default escape character
		if (likePredicate.getEscapeCharacter() == null) {
			renderBackslashEscapedLikePattern( likePredicate.getPattern(), likePredicate.getEscapeCharacter(), true );
		}
		else {
			likePredicate.getPattern().accept( this );
			appendSql( " escape " );
			likePredicate.getEscapeCharacter().accept( this );
		}
	}
}
