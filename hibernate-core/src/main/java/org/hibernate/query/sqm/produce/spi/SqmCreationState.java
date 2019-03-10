/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.annotations.Remove;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.produce.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * Access to the current state for SQM tree creation
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationState {
	/**
	 * Access to the context of the creation
	 */
	SqmCreationContext getCreationContext();

	/**
	 * What options should be applied to the creation
	 */
	SqmCreationOptions getCreationOptions();

	Stack<SqmCreationProcessingState> getProcessingStateStack();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Would prefer to use NavigablePath to serve the role that uid currently
	// serves

	String generateUniqueIdentifier();



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Below are the methods we want to re-think in terms of how SQM creation
	// happens - SqmPath, etc


	/**
	 * @deprecated we want to re-think in terms of how SQM creation happens
	 */
	@Remove
	@Deprecated
	ImplicitAliasGenerator getImplicitAliasGenerator();

	/**
	 * @deprecated we want to re-think in terms of how SQM creation happens
	 */
	@Remove
	@Deprecated
	SqmQuerySpecCreationProcessingState getCurrentQuerySpecProcessingState();

	/**
	 * @deprecated we want to re-think in terms of how SQM creation happens
	 */
	@Remove
	@Deprecated
	void cacheNavigableReference(SqmNavigableReference reference);

	/**
	 * @deprecated we want to re-think in terms of how SQM creation happens
	 */
	@Remove
	@Deprecated
	SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable);

	/**
	 * @deprecated we want to re-think in terms of how SQM creation happens
	 */
	@Remove
	@Deprecated
	void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin);
}
