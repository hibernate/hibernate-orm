/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.persister.entity.Queryable;

/**
 * Generalized strategy contract for handling multi-table bulk HQL operations.
 *
 * @author Steve Ebersole
 */
public interface MultiTableBulkIdStrategy {
	/**
	 * Prepare the strategy.  Called as the SessionFactory is being built.  Intended patterns here include:<ul>
	 *     <li>Adding tables to the passed Mappings, to be picked by by "schema management tools"</li>
	 *     <li>Manually creating the tables immediately through the passed JDBC Connection access</li>
	 * </ul>
	 *  @param jdbcServices The JdbcService object
	 * @param connectionAccess Access to the JDBC Connection
	 * @param metadata Access to the O/RM mapping information
	 * @param sessionFactoryOptions
	 */
	public void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess,
			MetadataImplementor metadata,
			SessionFactoryOptions sessionFactoryOptions);

	/**
	 * Release the strategy.   Called as the SessionFactory is being shut down.
	 *
	 * @param jdbcServices The JdbcService object
	 * @param connectionAccess Access to the JDBC Connection
	 */
	public void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess);

	/**
	 * Handler for dealing with multi-table HQL bulk update statements.
	 */
	public static interface UpdateHandler {
		public Queryable getTargetedQueryable();
		public String[] getSqlStatements();

		public int execute(SessionImplementor session, QueryParameters queryParameters);
	}

	/**
	 * Build a handler capable of handling the bulk update indicated by the given walker.
	 *
	 * @param factory The SessionFactory
	 * @param walker The AST walker, representing the update query
	 *
	 * @return The handler
	 */
	public UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, HqlSqlWalker walker);

	/**
	 * Handler for dealing with multi-table HQL bulk delete statements.
	 */
	public static interface DeleteHandler {
		public Queryable getTargetedQueryable();
		public String[] getSqlStatements();

		public int execute(SessionImplementor session, QueryParameters queryParameters);
	}

	/**
	 * Build a handler capable of handling the bulk delete indicated by the given walker.
	 *
	 * @param factory The SessionFactory
	 * @param walker The AST walker, representing the delete query
	 *
	 * @return The handler
	 */
	public DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, HqlSqlWalker walker);
}
