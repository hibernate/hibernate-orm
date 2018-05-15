/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;

/**
 * @author Steve Ebersole
 */
public class AggregateSqlSelectionGroupNode implements SqlSelectionGroupNode {
	private final List<? extends SqlSelectionGroupNode> subNodes;

	public AggregateSqlSelectionGroupNode(List<? extends SqlSelectionGroupNode> subNodes) {
		this.subNodes = subNodes;
	}

	@Override
	public Object hydrateStateArray(RowProcessingState currentRowState) {
		if ( subNodes.size() == 0 ) {
			return null;
		}
		if ( subNodes.size() == 1 ) {
			return subNodes.get( 0 ).hydrateStateArray( currentRowState );
		}
		final Object[] values = new Object[ subNodes.size() ];
		for ( int i = 0; i < subNodes.size(); i++ ) {
			values[i] = subNodes.get( i ).hydrateStateArray( currentRowState );
		}
		return values;
	}

	@Override
	public void visitSqlSelections(Consumer<SqlSelection> action) {
		subNodes.forEach( node -> node.visitSqlSelections( action ) );
	}
}
