/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;

/**
 * This bulk-id strategy inlines identifiers of the rows that need to be updated or deleted using multiple identifier OR clauses.
 *
 * <pre>
 * delete
 * from
 *     Engineer
 * where
 *     (
 *         id = 0
 *         and companyName = 'Red Hat Europe'
 *     )
 *     or (
 *         id = 1
 *       and companyName = 'Red Hat USA'
 *   )
 * </pre>
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsOrClauseBulkIdStrategy
		implements MultiTableBulkIdStrategy {

	public static final InlineIdsOrClauseBulkIdStrategy INSTANCE =
			new InlineIdsOrClauseBulkIdStrategy();

	@Override
	public void prepare(
			JdbcServices jdbcServices,
			JdbcConnectionAccess jdbcConnectionAccess,
			MetadataImplementor metadataImplementor,
			SessionFactoryOptions sessionFactoryOptions) {
		// nothing to do
	}

	@Override
	public void release(
			JdbcServices jdbcServices,
			JdbcConnectionAccess connectionAccess) {
		// nothing to do
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new InlineIdsOrClauseUpdateHandlerImpl( factory, walker );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new InlineIdsIdsOrClauseDeleteHandlerImpl( factory, walker );
	}

}
