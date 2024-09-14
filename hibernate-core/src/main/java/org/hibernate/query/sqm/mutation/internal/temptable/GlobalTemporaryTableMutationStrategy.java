/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * Strategy based on ANSI SQL's definition of a "global temporary table".
 *
 * @author Steve Ebersole
 */
public class GlobalTemporaryTableMutationStrategy extends GlobalTemporaryTableStrategy implements SqmMultiTableMutationStrategy {

	public GlobalTemporaryTableMutationStrategy(
			TemporaryTable idTable,
			SessionFactoryImplementor sessionFactory) {
		super( idTable, sessionFactory );
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedUpdateHandler(
				sqmUpdate,
				domainParameterXref,
				getTemporaryTable(),
				getSessionFactory().getJdbcServices().getDialect().getTemporaryTableAfterUseAction(),
				// generally a global temp table should already track a Connection-specific uid,
				// but just in case a particular env needs it...
				session -> session.getSessionIdentifier().toString(),
				getSessionFactory()
		).execute( context );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new TableBasedDeleteHandler(
				sqmDelete,
				domainParameterXref,
				getTemporaryTable(),
				getSessionFactory().getJdbcServices().getDialect().getTemporaryTableAfterUseAction(),
				// generally a global temp table should already track a Connection-specific uid,
				// but just in case a particular env needs it...
				session -> session.getSessionIdentifier().toString(),
				getSessionFactory()
		).execute( context );
	}
}
