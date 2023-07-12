/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Allows bidirectional iteration over pages of query results, useful for processing
 * query results in batches.
 * <pre>
 * Pager&lt;Book&gt; bookPager =
 *		 session.createSelectionQuery("from Book where title like :title", Book.class)
 *				.setParameter("title", titlePattern)
 *				.getResultPager(Page.first(pageSize));
 * while (bookPager.hasResults()) {
 *	 List&lt;Book&gt; books = bookPager.getResultList();
 *	 ...
 *	 session.clear();
 *	 bookPager.next();
 * }
 * </pre>
 * Or simply:
 * <pre>
 * session.createSelectionQuery("from Book where title like :title", Book.class)
 *		.setParameter("title", titlePattern)
 *		.getResultPager(Page.first(pageSize))
 *		.forEachRemainingPage(books -> {
 *			...
 *			session.clear();
 *		});
 * </pre>
 *
 * @param <R> the query result type
 *
 * @see Page
 * @see SelectionQuery#getResultPager(Page)
 *
 * @author Gavin King
 * @since 6.3
 */
@Incubating
public class Pager<R> {
	private final SelectionQuery<R> query;
	private List<R> results;
	private Page page;
	private boolean hasMoreResults;
	private int lastPage = -1;

	Pager(SelectionQuery<R> query, Page initialPage) {
		this.query = query;
		this.page = initialPage;
		this.hasMoreResults = true;
		execute();
	}

	/**
	 * @return the {@link SelectionQuery} backing this pager.
	 */
	public SelectionQuery<R> getQuery() {
		return query;
	}

	/**
	 * @return the current {@linkplain Page page}.
	 */
	public Page getCurrentPage() {
		return page;
	}

	/**
	 * @return the current page of results, as a list.
	 */
	public List<R> getResultList() {
		return results;
	}

	/**
	 * Advance to the next page of results.
	 *
	 * @see Page#next()
	 */
	public void next() {
		page = page.next();
		execute();
	}

	/**
	 * Go back to the previous page of results.
	 *
	 * @see Page#previous()
	 */
	public void previous() {
		page = page.previous();
		if ( lastPage >= 0 ) {
			hasMoreResults = page.getNumber() <= lastPage;
		}
		execute();
	}

	/**
	 * @return {@code true} if the {@linkplain #next() next} page has
	 *		 a nonempty {@linkplain #getResultList() result list}.
	 *
	 * @apiNote A return value of {@code true} guarantees that the
	 *          next page has at least one result. A return value of
	 *          {@code false} indicates that every subsequent page is
	 *          empty.
	 *
	 * @see #next()
	 */
	public boolean hasResultsOnNextPage() {
		return hasMoreResults;
	}

	/**
	 * @return {@code true} unless this is the first page of results.
	 *
	 * @apiNote A return value of {@code true} does not guarantee
	 *          that the {@linkplain #getResultList() result list}
	 *          of the previous page is nonempty. But a return value
	 *          of {@code false} does indicate that every earlier
	 *          page is empty.
	 *
	 * @see #previous()
	 */
	public boolean hasResultsOnPreviousPage() {
		return !page.isFirst();
	}

	/**
	 * @return {@code true} if the current page has a nonempty
	 *		 {@linkplain #getResultList() result list}.
	 */
	public boolean hasResults() {
		return !results.isEmpty();
	}

	/**
	 * Perform the given action on each remaining page of results,
	 * starting from the current page.
	 */
	public void forEachRemainingPage(Consumer<? super List<R>> action) {
		new PageIterator().forEachRemaining( action );
	}

	/**
	 * Perform the given action on each result, starting from the
	 * first result of the current page.
	 */
	public void forEachRemaining(Consumer<? super R> action) {
		forEachRemainingPage( page -> page.forEach( action ) );
	}

	/**
	 * @return a {@link Stream} over the remaining pages of results,
	 *		 starting from the current page.
	 */
	public Stream<List<R>> stream() {
		return StreamSupport.stream(
				spliteratorUnknownSize( new PageIterator(), NONNULL | IMMUTABLE | ORDERED ),
				false
		);
	}

	private void execute() {
		if ( hasMoreResults ) {
			// either the previous page was not the last,
			// or we went backwards using previous(), or
			// this is the very first time we're executing
			// the query
			final int pageSize = page.getMaxResults();
			// ask for one more row than we actually need,
			// to detect if there are more results on the
			// next page
			query.setMaxResults( pageSize + 1 );
			query.setFirstResult( page.getFirstResult() );
			final List<R> resultList = query.getResultList();
			hasMoreResults = resultList.size() > pageSize;
			if ( hasMoreResults ) {
				// there are more results on the next page
				// (the last result in the list belongs to
				// the next page, not to this page, remove)
				resultList.remove( pageSize );
			}
			else {
				// there are no more results on the next
				// page, so record that this is the last
				int currentPage = page.getNumber();
				if ( lastPage < 0 || currentPage < lastPage ) {
					lastPage = currentPage;
				}
			}
			results = resultList;
			//TODO: unset the page on Query
		}
		else {
			// we know that this page has no results
			results = emptyList();
		}
	}

	private class PageIterator implements Iterator<List<R>> {
		private boolean first = true;

		@Override
		public boolean hasNext() {
			return first || hasMoreResults;
		}

		@Override
		public List<R> next() {
			if ( first ) {
				first = false;
			}
			else {
				if ( !hasMoreResults ) {
					throw new NoSuchElementException();
				}
				page = page.next();
				execute();
			}
			return results;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Pager does not implement Iterator.remove()");
		}
	}
}
