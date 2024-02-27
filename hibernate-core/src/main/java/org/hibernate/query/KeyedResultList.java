/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;

import java.util.List;

/**
 * Support for pagination based on a unique key of the result
 * set instead of the {@link Page#getFirstResult() offset}.
 * An instance of this class represent a page of results returned
 * by {@link SelectionQuery#getKeyedResultList(KeyedPage)}. The
 * actual query results are held in {@link #getResultList()}.
 *
 * @since 6.5
 *
 * @see KeyedPage
 * @see SelectionQuery#getKeyedResultList(KeyedPage)
 *
 * @author Gavin King
 */
@Incubating
public class KeyedResultList<R> {
	private final List<R> resultList;
	private final KeyedPage<R> page;
	private final KeyedPage<R> nextPage;

	public KeyedResultList(List<R> resultList, KeyedPage<R> page, KeyedPage<R> nextPage) {
		this.resultList = resultList;
		this.page = page;
		this.nextPage = nextPage;
	}

	/**
	 * The results on the current page.
	 */
	public List<R> getResultList() {
		return resultList;
	}

	/**
	 * The {@linkplain Page#getSize() size} and
	 * approximate {@linkplain Page#getNumber()
	 * page number} of the current page.
	 */
	public KeyedPage<R> getPage() {
		return page;
	}

	/**
	 * The specification of the next page of results,
	 * if there are more results, or {@code null} if
	 * it is known that there are no more results
	 * after this page.
	 */
	public KeyedPage<R> getNextPage() {
		return nextPage;
	}

	/**
	 * @return {@code true} if this is known to be the
	 *         last page of results.
	 */
	public boolean isLastPage() {
		return nextPage == null;
	}

	/**
	 * @return {@code true} if this is the first page
	 *         of results.
	 */
	public boolean isFirstPage() {
		return page.getPage().isFirst();
	}
}
