/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.hql.spi;

import java.util.Map;

import org.hibernate.cfg.Mappings;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
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
	 *
	 * @param jdbcServices The JdbcService object
	 * @param connectionAccess Access to the JDBC Connection
	 * @param mappings The Hibernate Mappings object, for access to O/RM mapping information
	 * @param mapping The Hibernate Mapping contract, mainly for use in DDL generation
	 * @param settings Configuration settings
	 */
	public void prepare(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess, Mappings mappings, Mapping mapping, Map settings);

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
