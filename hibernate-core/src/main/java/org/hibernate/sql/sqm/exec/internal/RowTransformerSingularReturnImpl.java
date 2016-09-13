/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import org.hibernate.sql.sqm.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class RowTransformerSingularReturnImpl<R> implements RowTransformer<R> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerSingularReturnImpl INSTANCE = new RowTransformerSingularReturnImpl();

	@Override
	@SuppressWarnings("unchecked")
	public R transformRow(Object[] row) {
		return (R) row[0];
	}
}
