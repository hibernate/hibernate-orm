/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.sqm.produce.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * Models the state related to parsing a sqm spec.  As a "linked list" to account for
 * subqueries
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class SqmQuerySpecCreationProcessingStateStandardImpl
		extends SqmCreationProcessingStateImpl
		implements SqmQuerySpecCreationProcessingState {

	private final SqmCreationProcessingState parentState;

	public SqmQuerySpecCreationProcessingStateStandardImpl(
			SqmCreationProcessingState parentState,
			SqmSelectQuery<?> processingQuery,
			SqmCreationState creationState) {
		super( processingQuery, creationState );
		this.parentState = parentState;
	}

	@Override
	public SqmCreationProcessingState getParentProcessingState() {
		return parentState;
	}

	@Override
	public SqmSelectQuery<?> getProcessingQuery() {
		return (SqmSelectQuery<?>) super.getProcessingQuery();
	}

	@Override
	public void registerSelection(SqmSelection selection) {
		getProcessingIndex().register( selection );
	}

	@Override
	public SqmSelection findSelectionByAlias(String alias) {
		return getProcessingIndex().findSelectionByAlias( alias );
	}

	@Override
	public SqmSelection findSelectionByPosition(int position) {
		return getProcessingIndex().findSelectionByPosition( position );
	}
}
