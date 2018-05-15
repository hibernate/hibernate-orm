/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * Access to the current state for SQM tree creation
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationState {

	QuerySpecProcessingState getCurrentQuerySpecProcessingState();

	SqmFromElementSpace getCurrentFromElementSpace();

	SqmFromBuilder getCurrentFromElementBuilder();

	CurrentSqmFromElementSpaceCoordAccess getCurrentSqmFromElementSpaceCoordAccess();

	SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable);

	void cacheNavigableReference(SqmNavigableReference reference);

	void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin);
}
