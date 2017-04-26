/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.sqm.produce.spi.AliasRegistry;
import org.hibernate.query.sqm.produce.spi.FromElementLocator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.domain.SqmNavigable;
import org.hibernate.query.sqm.produce.spi.AbstractQuerySpecProcessingState;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
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

	private final FromElementBuilder fromElementBuilder;

	public QuerySpecProcessingStateStandardImpl(ParsingContext parsingContext, QuerySpecProcessingState containingQueryState) {
		super( parsingContext, containingQueryState );

		this.fromClause = new SqmFromClause();

		if ( containingQueryState == null ) {
			this.fromElementBuilder = new FromElementBuilder( parsingContext, new AliasRegistry() );
		}
		else {
			this.fromElementBuilder = new FromElementBuilder(
					parsingContext,
					new AliasRegistry( containingQueryState.getFromElementBuilder().getAliasRegistry() )
			);
		}
	}

	public SqmFromClause getFromClause() {
		return fromClause;
	}

	@Override
	public FromElementBuilder getFromElementBuilder() {
		return fromElementBuilder;
	}

	@Override
	public SqmNavigableReference findNavigableBindingByIdentificationVariable(String identificationVariable) {
		return fromElementBuilder.getAliasRegistry().findFromElementByAlias( identificationVariable );
	}

	@Override
	public SqmNavigableReference findNavigableBindingExposingAttribute(String name) {
		SqmNavigableReference found = null;
		for ( SqmFromElementSpace space : fromClause.getFromElementSpaces() ) {
			if ( definesAttribute( space.getRoot().getBinding(), name ) ) {
				if ( found != null ) {
					throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + name );
				}
				found = space.getRoot().getBinding();
			}

			for ( SqmJoin join : space.getJoins() ) {
				if ( definesAttribute( join.getBinding(), name ) ) {
					if ( found != null ) {
						throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + name );
					}
					found = join.getBinding();
				}
			}
		}

		if ( found == null ) {
			if ( getContainingQueryState() != null ) {
				log.debugf( "Unable to resolve unqualified attribute [%s] in local SqmFromClause; checking containingQueryState" );
				found = getContainingQueryState().findNavigableBindingExposingAttribute( name );
			}
		}

		return found;
	}

	private boolean definesAttribute(SqmNavigableReference sourceBinding, String name) {
		if ( !SqmNavigableSourceReference.class.isInstance( sourceBinding ) ) {
			return false;
		}

		final SqmNavigable navigable = ( (SqmNavigableSourceReference) sourceBinding ).getReferencedNavigable().findNavigable( name );
		return navigable != null;
	}

	private boolean definesAttribute(SqmNavigableSourceReference sourceBinding, String name) {
		final SqmNavigable navigable = sourceBinding.getReferencedNavigable().findNavigable( name );
		return navigable != null;
	}

	@Override
	public FromElementLocator getFromElementLocator() {
		return this;
	}
}
