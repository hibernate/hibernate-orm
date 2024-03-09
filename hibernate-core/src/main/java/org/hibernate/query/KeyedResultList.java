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
 * <p>
 * An idiom for iterating pages of keyed results is:
 * <pre>
 * var query =
 *         session.createQuery("where title like :title", Book.class)
 *                 .setParameter("title", "%Hibernate%");
 * var resultList =
 *         query.getKeyedResultList(first(10).keyedBy(asc(Book_.isbn)));
 * var results = resultList.getResultList();
 * ...
 * while ( !resultList.isLastPage() ) {
 *     resultList = query.getKeyedResultList(resultList.getNextPage());
 *     results = resultList.getResultList();
 *     ...
 * }
 * </pre>
 * <p>
 * When {@code KeyedResultList} is the declared return type of a
 * {@linkplain org.hibernate.annotations.processing.Find finder
 * method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method}, the idiom may be written:
 * <pre>
 * var resultList =
 *         books.byTitle("%Hibernate%", first(10).keyedBy(asc(Book_.isbn)));
 * var results = resultList.getResultList();
 * ...
 * while ( !resultList.isLastPage() ) {
 *     resultList = books.byTitle("%Hibernate%", resultList.getNextPage());
 *     results = resultList.getResultList();
 *     ...
 * }
 * </pre>
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
	private final List<List<?>> keyList;
	private final KeyedPage<R> page;
	private final KeyedPage<R> nextPage;
	private final KeyedPage<R> previousPage;

	public KeyedResultList(
			List<R> resultList, List<List<?>> keyList,
			KeyedPage<R> page, KeyedPage<R> nextPage, KeyedPage<R> previousPage) {
		this.resultList = resultList;
		this.keyList = keyList;
		this.page = page;
		this.nextPage = nextPage;
		this.previousPage = previousPage;
	}

	/**
	 * The results on the current page.
	 */
	public List<R> getResultList() {
		return resultList;
	}

	/**
	 * The keys of the results, in order.
	 */
	public List<List<?>> getKeyList() {
		return keyList;
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
	 * The specification of the previous page of results,
	 * or {@code null} if it is known that this is the
	 * first page.
	 */
	public KeyedPage<R> getPreviousPage() {
		return previousPage;
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
