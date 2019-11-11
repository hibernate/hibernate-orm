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
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;

/**
 * @asciidoc
 *
 * {@link SqmMultiTableMutationStrategy} implementation using SQL's CTE (Common Table Expression)
 * approach to perform the update/delete.  E.g. (using delete):
 *
 * ````
 * with cte_id (id) as (
 *     select
 *         id
 *     from (
 *         values
 *             (?),
 *             (?),
 *             (?)
 *             (?)
 *     )
 * )
 * delete
 * from
 *     Person
 * where
 *     ( id ) in (
 *         select id
 *         from cte_id
 *     )
 * ````
 *
 * todo (6.0) : why not:
 *
 * ````
 * with cte_id (id) as (
 *     select id
 *     from Person p
 *     where ...
 * )
 * delete from Contact
 * where (id) in (
 * 		select id
 * 		from cte_id
 * )
 *
 * with cte_id (id) as (
 *     select id
 *     from Person p
 *     where ...
 * )
 * delete from Person
 * where (id) in (
 * 		select id
 * 		from cte_id
 * )
 * ````
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class CteBasedMutationStrategy implements SqmMultiTableMutationStrategy {
	public static final String SHORT_NAME = "cte";
	public static final String TABLE_NAME = "id_cte";

	private final EntityPersister rootDescriptor;
	private final CteTable cteTable;

	public CteBasedMutationStrategy(
			EntityPersister rootDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.rootDescriptor = rootDescriptor;

		final Dialect dialect = runtimeModelCreationContext.getTypeConfiguration()
				.getSessionFactory()
				.getServiceRegistry()
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

		if ( !dialect.supportsRowValueConstructorSyntaxInInList() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support IN clause row-value expressions (for composite identifiers)"
			);
		}

		this.cteTable = new CteTable( rootDescriptor, runtimeModelCreationContext );
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		checkMatch( sqmUpdateStatement, creationContext );

		return new CteUpdateHandler( cteTable, sqmUpdateStatement, domainParameterXref, this, creationContext );
	}

	private void checkMatch(SqmDeleteOrUpdateStatement sqmStatement, HandlerCreationContext creationContext) {
		final String targetEntityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister targetEntityDescriptor = creationContext.getSessionFactory()
				.getDomainModel()
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

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		checkMatch( sqmDeleteStatement, creationContext );

		return new CteDeleteHandler( cteTable, sqmDeleteStatement, domainParameterXref, this, creationContext );
	}
}
