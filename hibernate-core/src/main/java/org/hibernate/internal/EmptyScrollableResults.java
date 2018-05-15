/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * @author Andrea Boriero
 */
public class EmptyScrollableResults implements ScrollableResultsImplementor {

	public static final ScrollableResultsImplementor INSTANCE = new EmptyScrollableResults();

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public void close() {

	}

	@Override
	public boolean next() {
		return false;
	}

	@Override
	public boolean previous() {
		return false;
	}

	@Override
	public boolean scroll(int positions) {
		return false;
	}

	@Override
	public boolean last() {
		return true;
	}

	@Override
	public boolean first() {
		return false;
	}

	@Override
	public void beforeFirst() {

	}

	@Override
	public void afterLast() {

	}

	@Override
	public boolean isFirst() {
		return false;
	}

	@Override
	public boolean isLast() {
		return false;
	}

	@Override
	public int getRowNumber() {
		return 0;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		return false;
	}

	@Override
	public Object[] get() {
		return new Object[0];
	}
}
