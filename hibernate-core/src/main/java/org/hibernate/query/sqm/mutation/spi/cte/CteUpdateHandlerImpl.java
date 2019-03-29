/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public class CteUpdateHandlerImpl
		extends AbstractCteMutationHandler
		implements UpdateHandler {

	public CteUpdateHandlerImpl(
			SqmUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			CteBasedMutationStrategy strategy,
			HandlerCreationContext creationContext) {
		super( sqmStatement, domainParameterXref, strategy, creationContext );
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception();
	}
}
