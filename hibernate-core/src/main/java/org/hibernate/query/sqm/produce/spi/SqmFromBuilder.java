/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public interface SqmFromBuilder {

	// todo (6.0) : assume the builder has access to SqmCreationContext?  Or pass it along (as param)?

	// todo (6.0) : same for `fromElementSpace` - pass it along or assume the builder has access to the current space

	// todo (6.0) : have `#buildRoot` trigger creation of the `SqmFromElementSpace` and making that available through `QuerySpecProcessingState` from `SqmCreationContext`
	//		this allows subsequent calls to build joins can look that up to know the space to use
	//
	//		the other option is for SemanticQueryBuilder (as SqmCreationContext) to create the spaces
	//		as we walk the parse tree, and handle that part itself.  this second option is probably the best overall.

	SqmRoot buildRoot(EntityValuedNavigable navigable);

	SqmCrossJoin buildCrossJoin(EntityValuedNavigable navigable);

	SqmEntityJoin buildEntityJoin(EntityValuedNavigable navigable);

	SqmNavigableJoin buildNavigableJoin(SqmNavigableReference navigableReference);
}
