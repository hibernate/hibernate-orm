/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributorContainer;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;

/**
 * @author Steve Ebersole
 */
@Remove
public abstract class AbstractSqlSelectionGroup implements SqlSelectionGroup {
	private final Map<StateArrayContributor<?>, SqlSelectionGroupNode> selectionNodesByContributor;

	public AbstractSqlSelectionGroup(Map<StateArrayContributor<?>, SqlSelectionGroupNode> selectionNodesByContributor) {
		this.selectionNodesByContributor = selectionNodesByContributor;
	}

	protected abstract StateArrayContributorContainer getContributorContainer();

	@Override
	public Object hydrateStateArray(RowProcessingState currentRowState) {
		final List<StateArrayContributor<?>> stateArrayContributors = getContributorContainer().getStateArrayContributors();
		final Object[] state = new Object[ stateArrayContributors.size() ];

		stateArrayContributors.forEach(
				contributor -> {
					final SqlSelectionGroupNode selections = selectionNodesByContributor.get( contributor );
					final Object value;
					if ( selections == null ) {
						value = UNFETCHED_PROPERTY;
					}
					else {
						value = selections.hydrateStateArray( currentRowState );
					}
					state[ contributor.getStateArrayPosition() ] = value;
				}
		);
		return state;
	}

	@Override
	public void visitSqlSelections(Consumer<SqlSelection> action) {
		selectionNodesByContributor.forEach(
				(contributor, sqlSelectionGroupNode) -> sqlSelectionGroupNode.visitSqlSelections( action )
		);
	}
}
