/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;

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
public class CteMutationStrategy implements SqmMultiTableMutationStrategy {
	public static final String SHORT_NAME = "cte";
	public static final String ID_TABLE_NAME = "id_cte";

	private final EntityPersister rootDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	private final CteTable idCteTable;

	public CteMutationStrategy(
			EntityMappingType rootEntityType,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this( rootEntityType.getEntityPersister(), runtimeModelCreationContext );
	}

	public CteMutationStrategy(
			EntityPersister rootDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.rootDescriptor = rootDescriptor;
		this.sessionFactory = runtimeModelCreationContext.getSessionFactory();

		final Dialect dialect = runtimeModelCreationContext.getDialect();

		if ( !dialect.supportsNonQueryWithCTE() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support CTE that can take UPDATE or DELETE statements as well"
			);
		}

		this.idCteTable = CteTable.createIdTable( ID_TABLE_NAME, rootDescriptor );
	}

	@Override
	public int executeDelete(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		checkMatch( sqmDelete );

		final CteDeleteHandler deleteHandler;
		if ( rootDescriptor.getSoftDeleteMapping() != null ) {
			deleteHandler = new CteSoftDeleteHandler(
					idCteTable,
					sqmDelete,
					domainParameterXref,
					this,
					sessionFactory
			);
		}
		else {
			deleteHandler = new CteDeleteHandler(
					idCteTable,
					sqmDelete,
					domainParameterXref,
					this,
					sessionFactory
			);
		}
		return deleteHandler.execute( context );
	}

	@Override
	public int executeUpdate(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		checkMatch( sqmUpdate );
		return new CteUpdateHandler( idCteTable, sqmUpdate, domainParameterXref, this, sessionFactory ).execute( context );
	}

	protected void checkMatch(SqmDeleteOrUpdateStatement<?> sqmStatement) {
		final String targetEntityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister targetEntityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( targetEntityName );

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

	protected EntityPersister getRootDescriptor() {
		return rootDescriptor;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected CteTable getIdCteTable() {
		return idCteTable;
	}
}
