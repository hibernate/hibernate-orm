/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.inline;

import java.util.List;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.AbstractIdsBulkIdHandler;

/**
 * Base class for all bulk-id strategy handlers that inline the identifiers of the updatable/deletable rows.
 *
 * @author Vlad Mihalcea
 */
public abstract class AbstractInlineIdsBulkIdHandler
		extends AbstractIdsBulkIdHandler {

	public AbstractInlineIdsBulkIdHandler(
			SessionFactoryImplementor sessionFactory,
			HqlSqlWalker walker) {
		super( sessionFactory, walker );
	}

	protected IdsClauseBuilder prepareInlineStatement(
			SharedSessionContractImplementor session,
			QueryParameters queryParameters) {
		return newIdsClauseBuilder( selectIds( session, queryParameters ) );
	}

	protected abstract IdsClauseBuilder newIdsClauseBuilder(List<Object[]> ids);
}
