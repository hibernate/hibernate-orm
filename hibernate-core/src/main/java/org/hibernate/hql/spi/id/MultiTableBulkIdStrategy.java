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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;

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
	void prepare(
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
	void release(JdbcServices jdbcServices, JdbcConnectionAccess connectionAccess);

	/**
	 * Handler for dealing with multi-table HQL bulk update statements.
	 */
	interface UpdateHandler {

		// todo (6.0) : Ideally I think the best option is for this delegate to just handle execution of the "entire SQM delete"
		//		i.e. the single HQL that needs to get split into multiple SQL operations...
		//
		//		something like:

		int execute(SqmDeleteStatement sqmDeleteStatement, QueryParameterBindings parameterBindings);

		// 		What is the proper return type?  for now returning the "number of rows affected" count..

		//		Similarly, any other parameters to pass?  Does it need the Session e.g.?

		// 		the parameter references in the tree still just refer back the the overall bindings
		// 		regardless of which "split tree" they get split into.  Perfect! :)


		// I'd remove thses ones below...

		Queryable getTargetedQueryable();
		String[] getSqlStatements();

		int execute(SharedSessionContractImplementor session, QueryParameters queryParameters);
	}

	/**
	 * Build a handler capable of handling the bulk update indicated by the given walker.
	 *
	 * @param factory The SessionFactory
	 * @param sqmUpdateStatement The SQM AST representing the update query
	 *
	 * @return The handler
	 */
	UpdateHandler buildUpdateHandler(SessionFactoryImplementor factory, SqmUpdateStatement sqmUpdateStatement);

	/**
	 * Handler for dealing with multi-table HQL bulk delete statements.
	 */
	interface DeleteHandler {

		// todo (6.0) : same as discussions above in UpdateHandler

		Queryable getTargetedQueryable();
		String[] getSqlStatements();

		int execute(SharedSessionContractImplementor session, QueryParameters queryParameters);
	}

	/**
	 * Build a handler capable of handling the bulk delete indicated by the given walker.
	 *
	 * @param factory The SessionFactory
	 * @param sqmDeleteStatement The SQM AST representing the delete query
	 *
	 * @return The handler
	 */
	DeleteHandler buildDeleteHandler(SessionFactoryImplementor factory, SqmDeleteStatement sqmDeleteStatement);
}
