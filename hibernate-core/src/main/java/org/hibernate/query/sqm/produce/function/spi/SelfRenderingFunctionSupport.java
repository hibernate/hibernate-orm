/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * Support for SqmFunctionTemplates that ultimately want to
 * perform SQL rendering themselves
 *
 * @author Steve Ebersole
 */
public interface SelfRenderingFunctionSupport {
	void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory);
}
