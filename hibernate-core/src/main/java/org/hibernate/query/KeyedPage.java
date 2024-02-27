/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.Internal;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Support for pagination based on a unique key of the result
 * set instead of the {@link Page#getFirstResult() offset}.
 * <p>
 * A specification for an initial page may be obtained from
 * an instance of {@link Page}.
 * <pre>
 * KeyedPage&lt;Book&gt; firstPage = Page.first(10).keyedBy(asc(Book_.isbn)));
 * </pre>
 * A {@link KeyedResultList} then may be obtained by calling
 * {@link SelectionQuery#getKeyedResultList(KeyedPage)}.
 * <pre>
 * KeyedResultList results =
 *         session.createQuery("from Book", Book.class)
 *                .getKeyedResultList(firstPage);
 * </pre>
 * The following page may be obtained from {@link KeyedResultList#getNextPage()}.
 * <pre>
 * KeyedPage&lt;Book&gt; nextPage = results.getNextPage();
 * KeyedResultList moreResults =
 *         session.createQuery("from Book", Book.class)
 *                .getKeyedResultList(nextPage);
 * </pre>
 *
 * @since 6.5
 *
 * @see SelectionQuery#getKeyedResultList(KeyedPage)
 * @see KeyedResultList
 *
 * @author Gavin King
 */
@Incubating
public class KeyedPage<R> {
	private final List<Order<? super R>> keyDefinition;
	private final Page page;
	private final List<Comparable<?>> key;

	KeyedPage(List<Order<? super R>> keyDefinition, Page page) {
		this( keyDefinition, page, null );
	}

	KeyedPage(List<Order<? super R>> keyDefinition, Page page, List<Comparable<?>> key) {
		this.page = page;
		this.keyDefinition = unmodifiableList(keyDefinition);
		this.key = key;
	}

	/**
	 * A key definition for key-based pagination. The list of {@link Order}
	 * objects must define a total ordering of the query result set, and
	 * thus forms a unique key on the result set.
	 */
	public List<Order<? super R>> getKeyDefinition() {
		return keyDefinition;
	}

	/**
	 * A specification of this page in terms of page size and an
	 * (approximate) page number.
	 */
	public Page getPage() {
		return page;
	}

	/**
	 * The key of the last result on the previous page, which is used
	 * to locate the start of the current page.
	 * <p>
	 * A null key indicates that an {@linkplain Page#getFirstResult()
	 * offset} should be used instead. This is used to obtain an
	 * initial page of results.
	 *
	 * @return the key of the last result on the previous page, or
	 *         null if an offset should be used to obtain an initial
	 *         page
	 */
	public List<Comparable<?>> getKey() {
		return key;
	}

	/**
	 * Obtain a specification of the next page of results, which is
	 * to be located using the given key, which must be the key of
	 * the last result on this page.
	 *
	 * @param keyOfLastResult the key of the last result on this page
	 * @return a {@link KeyedPage} representing the next page of results
	 */
	@Internal
	public KeyedPage<R> nextPage(List<Comparable<?>> keyOfLastResult) {
		return new KeyedPage<>( keyDefinition, page.next(), keyOfLastResult );
	}
}
