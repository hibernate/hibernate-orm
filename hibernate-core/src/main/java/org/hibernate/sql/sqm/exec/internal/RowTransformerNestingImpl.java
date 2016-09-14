/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import org.hibernate.sql.sqm.exec.spi.RowTransformer;

/**
 * Allows nesting RowTransformer impls
 *
 * @author Steve Ebersole
 */
public class RowTransformerNestingImpl implements RowTransformer {
	private final RowTransformer innerTransformer;
	private final RowTransformer outerTransformer;

	public RowTransformerNestingImpl(
			RowTransformer innerTransformer,
			RowTransformer outerTransformer) {
		this.innerTransformer = innerTransformer;
		this.outerTransformer = outerTransformer;
	}

	@Override
	public Object transformRow(Object[] row) {
		final Object innerResult = innerTransformer.transformRow( row );
		final Object[] innerResultRow;
		if ( innerResult.getClass().isArray() ) {
			innerResultRow = (Object[]) innerResult;
		}
		else {
			innerResultRow = new Object[] { innerResult };
		}

		return outerTransformer.transformRow( innerResultRow );
	}
}
