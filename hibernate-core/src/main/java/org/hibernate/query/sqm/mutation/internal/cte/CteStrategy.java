/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @asciidoc
 *
 * {@link SqmMultiTableMutationStrategy} implementation using SQL's modifiable CTE (Common Table Expression)
 * approach to perform the update/delete.  E.g. (using delete):
 *
 * ````
 * with cte_id (id) as (
 *     select
 *         id
 *     from Person
 *     where condition
 * ), delete_1 as (
 *   delete
 *   from
 *   	Person
 *   where
 *   	(id) in (
 *   		select id
 *   		from cte_id
 *   	)
 *   returning id
 * )
 * select count(*) from cte_id
 * ````
 *
 * @author Christian Beikov
 */
public class CteStrategy implements SqmMultiTableMutationStrategy {
	public static final String SHORT_NAME = "cte";
	public static final String TABLE_NAME = "id_cte";

	private final EntityPersister rootDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	private final SqmCteTable cteTable;

	public CteStrategy(
			EntityMappingType rootEntityType,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this( rootEntityType.getEntityPersister(), runtimeModelCreationContext );
	}

	public CteStrategy(
			EntityPersister rootDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.rootDescriptor = rootDescriptor;
		this.sessionFactory = runtimeModelCreationContext.getSessionFactory();

		final Dialect dialect = sessionFactory.getServiceRegistry()
				.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect();

		if ( !dialect.supportsNonQueryWithCTE() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support CTE that can take UPDATE or DELETE statements as well"
			);
		}

		if ( !dialect.supportsValuesList() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support VALUES lists"
			);
		}

		this.cteTable = new SqmCteTable( TABLE_NAME, rootDescriptor );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		checkMatch( sqmDelete );
		return new CteDeleteHandler( cteTable, sqmDelete, domainParameterXref, this, sessionFactory ).execute( context );
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		checkMatch( sqmUpdate );
		return new CteUpdateHandler( cteTable, sqmUpdate, domainParameterXref, this, sessionFactory ).execute( context );
	}

	private void checkMatch(SqmDeleteOrUpdateStatement sqmStatement) {
		final String targetEntityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister targetEntityDescriptor = sessionFactory.getDomainModel().getEntityDescriptor( targetEntityName );

		if ( targetEntityDescriptor != rootDescriptor && ! rootDescriptor.isSubclassEntityName( targetEntityDescriptor.getEntityName() ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Target of query [%s] did not match configured entity [%s]",
							targetEntityName,
							rootDescriptor.getEntityName()
					)
			);
		}

	}
}
