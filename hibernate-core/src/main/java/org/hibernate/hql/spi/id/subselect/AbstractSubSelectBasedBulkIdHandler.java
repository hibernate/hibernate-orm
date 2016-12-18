/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.subselect;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.AbstractTableBasedBulkIdHandler;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public abstract class AbstractSubSelectBasedBulkIdHandler extends
		AbstractTableBasedBulkIdHandler {
	
	public AbstractSubSelectBasedBulkIdHandler(
			SessionFactoryImplementor sessionFactory, HqlSqlWalker walker) {
		super(sessionFactory, walker);
	}

	/**
	 * This is required for MySQL, otherwise a
	 * "You can't specify target table X for update in FROM clause” Exception is thrown
	 */
	protected String generateIdSubSelect(String tableAlias, ProcessedWhereClause whereClause) {
		return String.format( "select * from ( %s ) HT_%s", generateIdSelect( tableAlias, whereClause ).toStatementString(), tableAlias );
	}
}
