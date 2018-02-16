/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuerySpecProcessingState implements QuerySpecProcessingState {
	private final SqmCreationContext creationContext;
	private final AliasRegistry aliasRegistry;
	private final QuerySpecProcessingState containingQueryState;
	private List<QuerySpecProcessingState> subQueryStateList;

	public AbstractQuerySpecProcessingState(SqmCreationContext creationContext, QuerySpecProcessingState containingQueryState) {
		this.creationContext = creationContext;
		this.containingQueryState = containingQueryState;

		if ( containingQueryState == null ) {
			this.aliasRegistry = new AliasRegistry();
		}
		else {
			this.aliasRegistry = new AliasRegistry( containingQueryState.getAliasRegistry() );
			( (AbstractQuerySpecProcessingState) containingQueryState ).registerSubQueryState( this );
		}
	}


	private void registerSubQueryState(QuerySpecProcessingState subQueryState) {
		if ( subQueryStateList == null ) {
			// this is the first subquery we have encountered for this processing state
			subQueryStateList = new ArrayList<>();
		}

		subQueryStateList.add( subQueryState );
	}

	@Override
	public SqmCreationContext getSqmCreationContext() {
		return creationContext;
	}

	@Override
	public AliasRegistry getAliasRegistry() {
		return aliasRegistry;
	}

	@Override
	public QuerySpecProcessingState getContainingQueryState() {
		return containingQueryState;
	}

	@Override
	public List<QuerySpecProcessingState> getSubQueryStateList() {
		return subQueryStateList == null
				? Collections.emptyList()
				: Collections.unmodifiableList( subQueryStateList );
	}
}
