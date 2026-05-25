/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.scrollable;

import org.hibernate.ScrollableResults;

/**
 * @author Andrea Boriero
 */
public class EmptyScrollableResults<R> implements ScrollableResults<R> {

	@SuppressWarnings("rawtypes")
	private static final ScrollableResults INSTANCE = new EmptyScrollableResults();

	@SuppressWarnings("unchecked")
	public static <R> EmptyScrollableResults<R> instance() {
		return (EmptyScrollableResults<R>) INSTANCE;
	}

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
	public boolean position(int position) {
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
		return -1;
	}

	@Override
	public int getPosition() {
		return 0;
	}

	@Override
	public boolean setRowNumber(int rowNumber) {
		return false;
	}

	@Override
	public void setFetchSize(int fetchSize) {}

	@Override
	public R get() {
		throw new UnsupportedOperationException( "Empty result set" );
	}
}
