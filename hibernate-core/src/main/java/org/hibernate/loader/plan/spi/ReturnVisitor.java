/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.spi;

/**
 * Visitor for processing {@link Return} graphs
 *
 * @author Steve Ebersole
 */
public class ReturnVisitor {
	public static void visit(Return[] rootReturns, ReturnVisitationStrategy strategy) {
		new ReturnVisitor( strategy ).visitReturns( rootReturns );
	}

	private final ReturnVisitationStrategy strategy;

	public ReturnVisitor(ReturnVisitationStrategy strategy) {
		this.strategy = strategy;
	}

	private void visitReturns(Return[] rootReturns) {
		strategy.start();

		for ( Return rootReturn : rootReturns ) {
			visitRootReturn( rootReturn );
		}

		strategy.finish();
	}

	private void visitRootReturn(Return rootReturn) {
		strategy.startingRootReturn( rootReturn );

		if ( org.hibernate.loader.plan.spi.ScalarReturn.class.isInstance( rootReturn ) ) {
			strategy.handleScalarReturn( (ScalarReturn) rootReturn );
		}
		else {
			visitNonScalarRootReturn( rootReturn );
		}

		strategy.finishingRootReturn( rootReturn );
	}

	private void visitNonScalarRootReturn(Return rootReturn) {
		if ( EntityReturn.class.isInstance( rootReturn ) ) {
			strategy.handleEntityReturn( (EntityReturn) rootReturn );
			visitFetches( (EntityReturn) rootReturn );
		}
		else if ( CollectionReturn.class.isInstance( rootReturn ) ) {
			strategy.handleCollectionReturn( (CollectionReturn) rootReturn );
			visitFetches( (CollectionReturn) rootReturn );
		}
		else {
			throw new IllegalStateException(
					"Unexpected return type encountered; expecting a non-scalar root return, but found " +
							rootReturn.getClass().getName()
			);
		}
	}

	private void visitFetches(FetchOwner fetchOwner) {
		strategy.startingFetches( fetchOwner );

		for ( Fetch fetch : fetchOwner.getFetches() ) {
			visitFetch( fetch );
		}

		strategy.finishingFetches( fetchOwner );
	}

	private void visitFetch(Fetch fetch) {
		if ( EntityFetch.class.isInstance( fetch ) ) {
			strategy.startingEntityFetch( (EntityFetch) fetch );
			visitFetches( fetch );
			strategy.finishingEntityFetch( (EntityFetch) fetch );
		}
		else if ( CollectionFetch.class.isInstance( fetch ) ) {
			strategy.startingCollectionFetch( (CollectionFetch) fetch );
			visitFetches( fetch );
			strategy.finishingCollectionFetch( (CollectionFetch) fetch );
		}
		else if ( CompositeFetch.class.isInstance( fetch ) ) {
			strategy.startingCompositeFetch( (CompositeFetch) fetch );
			visitFetches( fetch );
			strategy.finishingCompositeFetch( (CompositeFetch) fetch );
		}
		else {
			throw new IllegalStateException(
					"Unexpected return type encountered; expecting a fetch return, but found " +
							fetch.getClass().getName()
			);
		}
	}

}
