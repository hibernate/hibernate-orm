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
 * This bulk-id strategy inlines identifiers of the rows that need to be updated or deleted in a subselect using a VALUES list:
 *
 * <pre>
 * delete
 * from
 *     Person
 * where
 *     ( id ) in (
 *         select
 *             id
 *         from (
 *             values
 *                 ( 1 ),
 *                 ( 2 ),
 *                 ( 3 ),
 *                 ( 4 )
 *             ) as HT (id)
 *     )
 * </pre>
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsSubSelectValueListBulkIdStrategy
		implements MultiTableBulkIdStrategy {

	public static final InlineIdsSubSelectValueListBulkIdStrategy INSTANCE =
			new InlineIdsSubSelectValueListBulkIdStrategy();

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
		return new InlineIdsSubSelectValuesListUpdateHandlerImpl(
				factory,
				walker
		);
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		return new InlineIdsSubSelectValuesListDeleteHandlerImpl(
				factory,
				walker
		);
	}

}
