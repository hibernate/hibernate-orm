/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class RowTransformerSingularReturnImpl<R> implements RowTransformer<R> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerSingularReturnImpl INSTANCE = new RowTransformerSingularReturnImpl();

	@SuppressWarnings("unchecked")
	public static <R> RowTransformerSingularReturnImpl<R> instance() {
		return INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transformRow(Object[] row) {
		return (R) row[0];
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
