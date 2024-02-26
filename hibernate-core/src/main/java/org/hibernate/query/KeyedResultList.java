/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.List;

/**
 * Support for pagination based on a unique key of the result
 * set instead of the {@link Page#getFirstResult() offset}.
 *
 * @since 6.5
 *
 * @author Gavin King
 */
public class KeyedResultList<R> {
	private final List<R> resultList;
	private final KeyedPage<R> page;
	private final KeyedPage<R> nextPage;

	public KeyedResultList(List<R> resultList, KeyedPage<R> page, KeyedPage<R> nextPage) {
		this.resultList = resultList;
		this.page = page;
		this.nextPage = nextPage;
	}

	public List<R> getResultList() {
		return resultList;
	}

	public KeyedPage<R> getPage() {
		return page;
	}

	public KeyedPage<R> getNextPage() {
		return nextPage;
	}
}
