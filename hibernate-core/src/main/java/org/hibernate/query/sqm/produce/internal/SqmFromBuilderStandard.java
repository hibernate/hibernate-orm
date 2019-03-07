/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.sqm.produce.spi.AbstractSqmFromBuilder;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

import org.jboss.logging.Logger;

/**
 * The normal SqmFromBuilder used throught most processing
 *
 * @author Steve Ebersole
 */
public class SqmFromBuilderStandard extends AbstractSqmFromBuilder {
	private static final Logger log = Logger.getLogger( SqmFromBuilderStandard.class );
	private static final boolean log_trace_enabled = log.isTraceEnabled();

	public SqmFromBuilderStandard(SqmCreationState creationState) {
		super( creationState );
	}

	@Override
	public SqmNavigableJoin buildNavigableJoin(SqmNavigableReference navigableReference) {
		assert navigableReference != null;
		assert navigableReference.getSourceReference() != null;
		assert navigableReference.getSourceReference().getExportedFromElement() != null;

		final SqmFromElementSpace fromElementSpace = navigableReference.getSourceReference()
				.getExportedFromElement()
				.getContainingSpace();
		assert fromElementSpace != null;

		getCreationState().getCurrentSqmFromElementSpaceCoordAccess().setCurrentSqmFromElementSpace( fromElementSpace );

		if ( log_trace_enabled ) {
			log.tracef( "#buildNavigableJoin( %s )", navigableReference );
		}

		final String uid = getCreationState().generateUniqueIdentifier();
		final String alias = getCreationState().getImplicitAliasGenerator().generateUniqueImplicitAlias();

		final SqmNavigableReference cachedNavigableReference = getCreationState().getCachedNavigableReference(
				navigableReference.getSourceReference(),
				navigableReference.getReferencedNavigable()
		);

		if ( cachedNavigableReference != null ) {
			final SqmNavigableJoin cachedJoin = (SqmNavigableJoin) cachedNavigableReference.getExportedFromElement();
			log.debugf( "Found re-usable join [%s] : %s", cachedNavigableReference, cachedJoin );
			return cachedJoin;
		}


		final SqmNavigableJoin navigableJoin = new SqmNavigableJoin(
				navigableReference.getSourceReference().getExportedFromElement(),
				navigableReference,
				uid,
				alias,
				SqmJoinType.INNER,
				false
		);

		fromElementSpace.addJoin( navigableJoin );
		registerAlias( navigableJoin );

		getCreationState().cacheNavigableReference( navigableReference );

		return navigableJoin;
	}
}
