/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.Incubating;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * The standard RowTransformer - <ol>
 *     <li>if the row array has just a single dimension, the value from that dimension (index zero) is returned</li>
 *     <li>otherwise, the array itself is returned</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@Incubating
public class RowTransformerStandardImpl<T> implements RowTransformer<T> {
	/**
	 * Singleton access
	 */
	@SuppressWarnings("rawtypes")
	public static final RowTransformerStandardImpl INSTANCE = new RowTransformerStandardImpl();

	@SuppressWarnings("unchecked")
	public static <T> RowTransformerStandardImpl<T> instance() {
		return INSTANCE;
	}

	private RowTransformerStandardImpl() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transformRow(Object[] row) {
		return row.length == 1 ? (T) row[0] : (T) row;
	}
}
