/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

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

	public CteBasedMutationStrategy(
			EntityPersister rootDescriptor,
			BootstrapContext bootstrapContext) {
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
