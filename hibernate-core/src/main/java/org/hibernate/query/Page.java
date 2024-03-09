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
 * Identifies a page of query results by {@linkplain #size page size}
 * and {@linkplain #number page number}.
 * <p>
 * This is a convenience class which allows a reference to a page of
 * results to be passed around the system before being applied to
 * a {@link Query} by calling {@link Query#setPage(Page)}.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Page}.
 * <p>
 * For key-based pagination, call {@link #keyedBy(Order)} to obtain a
 * {@link KeyedPage}.
 *
 * @see SelectionQuery#setPage(Page)
 *
 * @since 6.3
 *
 * @author Gavin King
 */
@Incubating
public class Page {
	private final int size;
	private final int number;

	/**
	 * The size of the page, that is, the maximum number
	 * of results on the page.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * The page number, where {@code 0} is the first page.
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @return {@code true} is this is the first page.
	 */
	public boolean isFirst() {
		return number == 0;
	}

	/**
	 * @return the page {@linkplain #size}, that is, the
	 *         {@linkplain SelectionQuery#getMaxResults()
	 *         maximum number of results} on the page.
	 */
	public int getMaxResults() {
		return size;
	}

	/**
	 * @return the {@linkplain SelectionQuery#getFirstResult()
	 *         offset} of this page.
	 */
	public int getFirstResult() {
		return size*number;
	}

	Page(int size, int number) {
		if ( size <= 0 ) {
			throw new IllegalArgumentException("page size must be strictly positive");
		}
		if ( number < 0 ) {
			throw new IllegalArgumentException("page number must be non-negative");
		}
		this.size = size;
		this.number = number;
	}

	/**
	 * Obtain a page, numbered from {@code 0}, with the given size.
	 * @param size the number of results on the initial page.
	 * @param number the number of the page, where {@code 0} is the first page.
	 */
	public static Page page(int size, int number) {
		return new Page( size, number );
	}

	/**
	 * Obtain an initial page with the given size.
	 * @param size the number of results on the initial page.
	 */
	public static Page first(int size) {
		return new Page( size, 0 );
	}

	/**
	 * Obtain the next page with the same size as this page.
	 */
	public Page next() {
		return new Page( size, number+1 );
	}

	/**
	 * Obtain the previous page with the same size as this page.
	 * @throws IllegalStateException if this is the first page
	 */
	public Page previous() {
		if ( isFirst() ) {
			throw new IllegalStateException("already at first page");
		}
		return new Page( size, number-1 );
	}

	public Page first() {
		return first( size );
	}

	/**
	 * Obtain a {@linkplain KeyedPage key-based specification} of this page.
	 *
	 * @param keyDefinition an {@link Order} object defining a total order
	 *                      on the result set
	 * @return a {@link KeyedPage} representing this page
	 *
	 * @since 6.5
	 */
	public <R> KeyedPage<R> keyedBy(Order<? super R> keyDefinition) {
		if ( keyDefinition == null )  {
			throw new IllegalArgumentException("Key definition must not null");
		}
		return new KeyedPage<>( List.of(keyDefinition), this );
	}

	/**
	 * Obtain a {@linkplain KeyedPage key-based specification} of this page.
	 *
	 * @param keyDefinition a list of {@link Order} objects defining a total
	 *                      order on the result set
	 * @return a {@link KeyedPage} representing this page
	 *
	 * @since 6.5
	 */
	public <R> KeyedPage<R> keyedBy(List<Order<? super R>> keyDefinition) {
		if ( keyDefinition == null || keyDefinition.isEmpty() )  {
			throw new IllegalArgumentException("Key definition must not be empty or null");
		}
		return new KeyedPage<>( keyDefinition, this );
	}
}
