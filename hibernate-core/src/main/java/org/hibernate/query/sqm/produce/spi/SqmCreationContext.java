/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.annotations.Remove;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;

/**
 * The "context" object for creation of SQM objects
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext extends SqmCreationState {
	SessionFactoryImplementor getSessionFactory();

	String generateUniqueIdentifier();

	ImplicitAliasGenerator getImplicitAliasGenerator();

	/**
	 * todo (6.0) : Remove this and the other state methods, pass the state into `Navigable#createSqmExpression` directly in addition to `SqmCreationContext`
	 */
	@Remove
	default SqmCreationState getCreationState() {
		return this;
	}

	@Remove
	QuerySpecProcessingState getCurrentQuerySpecProcessingState();

	@Remove
	SqmFromElementSpace getCurrentFromElementSpace();

	@Remove
	SqmFromBuilder getCurrentFromElementBuilder();

	@Remove
	CurrentSqmFromElementSpaceCoordAccess getCurrentSqmFromElementSpaceCoordAccess();

	@Remove
	void cacheNavigableReference(SqmNavigableReference reference);

	@Remove
	SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable);
}
