/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;

/**
 * Inline bulk-id delete handler that uses a subselect with a VALUES clause.
 *
 * @author Vlad Mihalcea
 */
public class InlineIdsSubSelectValuesListDeleteHandlerImpl
		extends AbstractInlineIdsDeleteHandlerImpl
		implements MultiTableBulkIdStrategy.DeleteHandler {

	public InlineIdsSubSelectValuesListDeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker) {
		super( factory, walker );

		Dialect dialect = factory().getServiceRegistry().getService( JdbcServices.class ).getDialect();
		if ( !dialect.supportsRowValueConstructorSyntaxInInList() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
							" can only be used with Dialects that support IN clause row-value expressions (for composite identifiers)!"
			);
		}
		if ( !dialect.supportsValuesList() ) {
			throw new UnsupportedOperationException(
					"The " + getClass().getSimpleName() +
							" can only be used with Dialects that support VALUES lists!"
			);
		}
	}

	@Override
	protected IdsClauseBuilder newIdsClauseBuilder(List<Object[]> ids) {
		return new InlineIdsSubSelectValuesListBuilder(
				dialect(),
				getTargetedQueryable().getIdentifierType(),
				factory().getTypeResolver(),
				getTargetedQueryable().getIdentifierColumnNames(),
				ids
		);
	}
}
