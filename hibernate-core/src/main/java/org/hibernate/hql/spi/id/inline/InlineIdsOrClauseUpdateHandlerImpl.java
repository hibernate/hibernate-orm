/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import java.util.List;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.sql.Update;

/**
 * Inline bulk-id update handler that uses multiple identifier OR clauses.
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsOrClauseUpdateHandlerImpl
		extends AbstractInlineIdsUpdateHandlerImpl
		implements MultiTableBulkIdStrategy.UpdateHandler {

	public InlineIdsOrClauseUpdateHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		super( factory, walker );
	}

	@Override
	protected IdsClauseBuilder newIdsClauseBuilder(List<Object[]> ids) {
		return new InlineIdsOrClauseBuilder(
				dialect(),
				getTargetedQueryable().getIdentifierType(),
				factory().getTypeResolver(),
				getTargetedQueryable().getIdentifierColumnNames(),
				ids
		);
	}

	@Override
	protected Update generateUpdate(
			String tableName,
			String[] columnNames,
			String idSubselect,
			String comment) {
		final Update update = new Update( factory().getServiceRegistry().getService( JdbcServices.class ).getDialect() )
				.setTableName( tableName )
				.setWhere( idSubselect );
		if ( factory().getSessionFactoryOptions().isCommentsEnabled() ) {
			update.setComment( comment );
		}
		return update;
	}
}
