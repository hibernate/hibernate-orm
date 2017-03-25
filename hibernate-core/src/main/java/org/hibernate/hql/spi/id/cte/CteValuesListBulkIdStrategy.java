/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;

/**
 * This bulk-id strategy uses a CTE with a VALUE list to hold the identifiers,
 * which are later used by the update or delete statement:
 *
 * <pre>
 * with HT_Person (id ) as (
 *     select
 *         id
 *     from (
 *         values
 *             (?),
 *             (?),
 *             (?)
 *             (?)
 *     ) as HT (id)
 * )
 * delete
 * from
 *     Person
 * where
 *     ( id ) in (
 *         select
 *             id
 *         from
 *             HT_Person
 *     )
 * </pre>
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CteValuesListBulkIdStrategy
		implements MultiTableBulkIdStrategy {

	public static final CteValuesListBulkIdStrategy INSTANCE = new CteValuesListBulkIdStrategy();

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
		return new CteValuesListUpdateHandlerImpl( factory, walker );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new CteValuesListDeleteHandlerImpl( factory, walker );
	}

}
