/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

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

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			throw new IndexOutOfBoundsException( "No selectables in empty instance" );
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			return 0;
		}
	};
}
