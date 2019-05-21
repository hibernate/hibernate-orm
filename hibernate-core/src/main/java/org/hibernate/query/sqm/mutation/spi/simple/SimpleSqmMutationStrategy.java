/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.simple;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public class SimpleSqmMutationStrategy implements SqmMutationStrategy {
	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		return new SimpleUpdateHandler( sqmUpdateStatement, domainParameterXref, creationContext );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		return new SimpleDeleteHandler( sqmDeleteStatement, domainParameterXref, creationContext );
	}
}
