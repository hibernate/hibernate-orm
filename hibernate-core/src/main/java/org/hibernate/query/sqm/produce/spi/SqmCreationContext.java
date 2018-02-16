/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

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
public interface SqmCreationContext {
	SessionFactoryImplementor getSessionFactory();

	QuerySpecProcessingState getCurrentQuerySpecProcessingState();

	SqmFromElementSpace getCurrentFromElementSpace();

	SqmFromBuilder getCurrentFromElementBuilder();

	CurrentSqmFromElementSpaceCoordAccess getCurrentSqmFromElementSpaceCoordAccess();

	String generateUniqueIdentifier();

	ImplicitAliasGenerator getImplicitAliasGenerator();

	void cacheNavigableReference(SqmNavigableReference reference);

	SqmNavigableReference getCachedNavigableReference(SqmNavigableContainerReference source, Navigable navigable);
}
