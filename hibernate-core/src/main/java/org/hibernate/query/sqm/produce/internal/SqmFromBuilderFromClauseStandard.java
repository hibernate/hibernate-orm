/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmFromBuilderFromClauseStandard extends AbstractSqmFromBuilderFromClause {

	public SqmFromBuilderFromClauseStandard(
			String alias,
			SqmCreationContext sqmCreationContext) {
		super( alias, sqmCreationContext );
	}

	@Override
	public SqmRoot buildRoot(EntityValuedNavigable navigable) {
		final SqmRoot root = new SqmRoot(
				getSqmCreationContext().getCurrentFromElementSpace(),
				getSqmCreationContext().generateUniqueIdentifier(),
				getAlias(),
				navigable,
				getSqmCreationContext()
		);

		getSqmCreationContext().getCurrentFromElementSpace().setRoot( root );
		commonHandling( root );

		return root;
	}

	@Override
	public SqmCrossJoin buildCrossJoin(EntityValuedNavigable navigable) {
		final SqmCrossJoin join = new SqmCrossJoin(
				getSqmCreationContext().getCurrentFromElementSpace(),
				getSqmCreationContext().generateUniqueIdentifier(),
				getAlias(),
				navigable.getEntityDescriptor(),
				getSqmCreationContext()
		);

		getSqmCreationContext().getCurrentFromElementSpace().addJoin( join );
		commonHandling( join );

		return join;
	}
}
