/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.produce.spi.AbstractQuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmJoin;

import org.jboss.logging.Logger;

/**
 * Models the state related to parsing a sqm spec.  As a "linked list" to account for
 * subqueries
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class QuerySpecProcessingStateStandardImpl extends AbstractQuerySpecProcessingState {
	private static final Logger log = Logger.getLogger( QuerySpecProcessingStateStandardImpl.class );

	private final SqmFromClause fromClause;

	public QuerySpecProcessingStateStandardImpl(SqmCreationContext creationContext, QuerySpecProcessingState containingQueryState) {
		super( creationContext, containingQueryState );

		this.fromClause = new SqmFromClause();

	}

	public SqmFromClause getFromClause() {
		return fromClause;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceByIdentificationVariable(String identificationVariable) {
		return getSqmCreationContext().getCurrentQuerySpecProcessingState().getAliasRegistry().findFromElementByAlias( identificationVariable );
	}

	@Override
	public SqmNavigableReference findNavigableReferenceExposingNavigable(String navigableName) {
		SqmNavigableReference found = null;
		for ( SqmFromElementSpace space : fromClause.getFromElementSpaces() ) {
			if ( definesAttribute( space.getRoot().getNavigableReference(), navigableName ) ) {
				if ( found != null ) {
					throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + navigableName );
				}
				found = space.getRoot().getNavigableReference();
			}

			for ( SqmJoin join : space.getJoins() ) {
				if ( definesAttribute( join.getNavigableReference(), navigableName ) ) {
					if ( found != null ) {
						throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + navigableName );
					}
					found = join.getNavigableReference();
				}
			}
		}

		if ( found == null ) {
			if ( getContainingQueryState() != null ) {
				log.debugf( "Unable to resolve unqualified attribute [%s] in local SqmFromClause; checking containingQueryState", navigableName );
				found = getContainingQueryState().findNavigableReferenceExposingNavigable( navigableName );
			}
		}

		return found;
	}

	private boolean definesAttribute(SqmNavigableReference sourceBinding, String name) {
		if ( !SqmNavigableContainerReference.class.isInstance( sourceBinding ) ) {
			return false;
		}

		final Navigable navigable = ( (SqmNavigableContainerReference) sourceBinding ).getReferencedNavigable().findNavigable( name );
		return navigable != null;
	}

	private boolean definesAttribute(SqmNavigableContainerReference sourceBinding, String name) {
		final Navigable navigable = sourceBinding.getReferencedNavigable().findNavigable( name );
		return navigable != null;
	}
}
