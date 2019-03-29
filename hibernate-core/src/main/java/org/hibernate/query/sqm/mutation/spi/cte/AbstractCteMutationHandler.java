/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;

/**
 * Defines how identifier values are selected from the updatable/deletable tables.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
public abstract class AbstractCteMutationHandler extends AbstractMutationHandler {
	private final DomainParameterXref domainParameterXref;
	private final CteBasedMutationStrategy strategy;

	public AbstractCteMutationHandler(
			SqmDeleteOrUpdateStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			CteBasedMutationStrategy strategy,
			HandlerCreationContext creationContext) {
		super( sqmStatement, creationContext );
		this.domainParameterXref = domainParameterXref;

		this.strategy = strategy;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	public CteBasedMutationStrategy getStrategy() {
		return strategy;
	}
}
