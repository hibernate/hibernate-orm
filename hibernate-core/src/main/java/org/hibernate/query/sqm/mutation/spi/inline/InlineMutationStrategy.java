/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.inline;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @asciidoc
 *
 * {@link SqmMutationStrategy}
 * implementation first selecting all matching ids back into memory and then
 * using those matching ids to update/delete against each table.
 *
 * ````
 * select id
 * from Person p
 * where ...
 *
 * delete from Contact
 * where ( id ) in ( 1, 2, 3, ...)
 *
 * delete from Person
 * where ( id ) in ( 1, 2, 3, ...)
 * ````
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class InlineMutationStrategy implements SqmMutationStrategy {
	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref, HandlerCreationContext creationContext) {
		return null;
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		return new InlineDeleteHandler( sqmDeleteStatement, domainParameterXref, creationContext );
	}
}
