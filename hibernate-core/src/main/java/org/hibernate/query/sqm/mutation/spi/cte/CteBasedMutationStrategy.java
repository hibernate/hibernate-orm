/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @asciidoc
 *
 * {@link SqmMutationStrategy}
 * implementation using CTE (SQL's Common Table Expression) approach to perform
 * the update/delete.  E.g. (using delete):
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
public class CteBasedMutationStrategy implements SqmMutationStrategy {
	public static final String ID_CTE = "id_cte";

	private final EntityTypeDescriptor<?> entityDescriptor;

	private final CteTable cteTable;

	public CteBasedMutationStrategy(EntityTypeDescriptor<?> entityDescriptor) {
		this.entityDescriptor = entityDescriptor;

		final Dialect dialect = entityDescriptor.getTypeConfiguration()
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

		this.cteTable = new CteTable( entityDescriptor );
	}

	public EntityTypeDescriptor<?> getEntityDescriptor() {
		return entityDescriptor;
	}

	public CteTable getCteTable() {
		return cteTable;
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		checkMatch( sqmStatement );

		return new CteUpdateHandlerImpl( sqmStatement, domainParameterXref, this, creationContext );
	}

	private void checkMatch(SqmDeleteOrUpdateStatement sqmStatement) {
		final EntityTypeDescriptor targetEntity = sqmStatement.getTarget().getReferencedPathSource();
		if ( targetEntity != entityDescriptor && ! entityDescriptor.isSubclassTypeName( targetEntity.getEntityName() ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Target of query [%s] did not match configured entity [%s]",
							targetEntity,
							entityDescriptor
					)
			);
		}

	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		checkMatch( sqmStatement );

		return new CteDeleteHandlerImpl( sqmStatement, domainParameterXref, this, creationContext );
	}
}
