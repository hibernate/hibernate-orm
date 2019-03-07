/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractQuerySpecProcessingState implements QuerySpecProcessingState {
	private final QuerySpecProcessingState containingQueryState;
	private final SqmCreationState creationState;

	private final AliasRegistry aliasRegistry;

	public AbstractQuerySpecProcessingState(
			QuerySpecProcessingState containingQueryState,
			SqmCreationState creationState) {
		this.containingQueryState = containingQueryState;
		this.creationState = creationState;

		if ( containingQueryState == null ) {
			this.aliasRegistry = new AliasRegistry();
		}
		else {
			this.aliasRegistry = new AliasRegistry( containingQueryState.getAliasRegistry() );
		}
	}

	@Override
	public SqmCreationState getCreationState() {
		return creationState;
	}

	@Override
	public AliasRegistry getAliasRegistry() {
		return aliasRegistry;
	}

	@Override
	public QuerySpecProcessingState getContainingQueryState() {
		return containingQueryState;
	}
}
