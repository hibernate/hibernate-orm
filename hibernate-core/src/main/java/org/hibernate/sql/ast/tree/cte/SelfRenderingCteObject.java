/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * A self rendering object that is part of a WITH clause, like a function.
 *
 * @author Christian Beikov
 */
public interface SelfRenderingCteObject extends CteObject {

	void render(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory);

}
