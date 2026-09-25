package org.hibernate.action.queue.internal.graph;

import jakarta.annotation.Nonnull;


import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;

/// Utility code
///
/// @author Steve Ebersole
public class Util {
	/// Empty SelectableMappings for non-breakable DELETE edges
	public static final SelectableMappings EMPTY_SELECTABLES = new SelectableMappings() {
		@Override
		public int getJdbcTypeCount() {
			return 0;
		}

		@Nonnull
		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			throw new IndexOutOfBoundsException( "No selectables in empty instance" );
		}

		@Override
		public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
			return 0;
		}
	};
}
