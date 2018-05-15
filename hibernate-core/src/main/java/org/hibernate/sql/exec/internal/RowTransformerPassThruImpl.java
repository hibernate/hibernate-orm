/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.Incubating;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * Essentially a no-op transformer - simply passes the result through
 *
 * @author Steve Ebersole
 */
@Incubating
public class RowTransformerPassThruImpl<T> implements RowTransformer<T> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerPassThruImpl INSTANCE = new RowTransformerPassThruImpl();

	@SuppressWarnings("unchecked")
	public static <T> RowTransformerPassThruImpl<T> instance() {
		return INSTANCE;
	}

	private RowTransformerPassThruImpl() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transformRow(Object[] row) {
		return row.length == 1 ? (T) row[0] : (T) row;
	}
}
