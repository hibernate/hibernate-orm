/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Andrea Boriero
 */
public class RowTransformerDatabaseSnapshotImpl<T> implements RowTransformer<T> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerDatabaseSnapshotImpl INSTANCE = new RowTransformerDatabaseSnapshotImpl();

	@SuppressWarnings("unchecked")
	public static <T> RowTransformerDatabaseSnapshotImpl<T> instance() {
		return INSTANCE;
	}

	private RowTransformerDatabaseSnapshotImpl() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transformRow(Object[] row) {
		return (T) row;
	}
}
