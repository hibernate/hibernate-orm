/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;

/**
 * The final phase of query translation. Here we take the SQL AST an
 * "interpretation". For a select query, that means an instance of
 * {@link JdbcOperationQuerySelect}.
 *
 * @author Christian Beikov
 */
public class StandardSqlAstTranslator<T extends JdbcOperation> extends AbstractSqlAstTranslator<T> {

	public StandardSqlAstTranslator(SessionFactoryImplementor sessionFactory, Statement statement) {
		super( sessionFactory, statement );
	}
}
