/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.Internal;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_FIRST_ON_NEXT_PAGE;
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_LAST_ON_PREVIOUS_PAGE;
import static org.hibernate.query.KeyedPage.KeyInterpretation.NO_KEY;

/**
 * Support for pagination based on a unique key of the result
 * set instead of the {@link Page#getFirstResult() offset}.
 * <p>
 * In this context, a <em>key</em> is a unique key of the
 * query result set which imposes a total order on the results.
 * It is represented as a {@code List<Order<? super R>>} where
 * {@code R} is the result type of the query. For example, a
 * unique key for paginating a query result set containing
 * {@code Book}s might be:
 * <pre>
 * var key = List.of(asc(Book_.title), asc(Book_.publicationDate), asc(Book_.publisher));
 * </pre>
 * <p>
 * When key-based pagination is used, Hibernate modifies the
 * original HQL or criteria query to incorporate the key in
 * the {@code order by}, {@code where}, and {@code select}
 * clauses.
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
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.Find
 * finder method} or {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Page}. Then the
 * return type of the method should be {@link KeyedResultList}.
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
	private final KeyInterpretation keyInterpretation;

	KeyedPage(List<Order<? super R>> keyDefinition, Page page) {
		this( keyDefinition, page, null, NO_KEY );
	}

	KeyedPage(List<Order<? super R>> keyDefinition, Page page, List<Comparable<?>> key, KeyInterpretation interpretation) {
		this.keyDefinition = unmodifiableList(keyDefinition);
		this.page = page;
		this.key = key;
		this.keyInterpretation = interpretation;
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
	 * The key of the last result on the previous page, or of the
	 * first result on the next page, which may be used to locate
	 * the start or end, respectively, of the current page.
	 * <p>
	 * A null key indicates that an {@linkplain Page#getFirstResult()
	 * offset} should be used instead. This is used to obtain an
	 * initial page of results.
	 *
	 * @return the key, or null if an offset should be used
	 */
	public List<Comparable<?>> getKey() {
		return key;
	}

	/**
	 * Determines whether the {@link #getKey() key} should be
	 * interpreted as the last result on the previous page, or
	 * as the first result on the next page.
	 */
	public KeyInterpretation getKeyInterpretation() {
		return keyInterpretation;
	}

	/**
	 * Obtain a specification of the next page of results, which is
	 * to be located using the given key, which must be the key of
	 * the last result on this page.
	 *
	 * @param keyOfLastResultOnThisPage the key of the last result on this page
	 * @return a {@link KeyedPage} representing the next page of results
	 */
	@Internal
	public KeyedPage<R> nextPage(List<Comparable<?>> keyOfLastResultOnThisPage) {
		return new KeyedPage<>( keyDefinition, page.next(), keyOfLastResultOnThisPage, KEY_OF_LAST_ON_PREVIOUS_PAGE );
	}

	/**
	 * Obtain a specification of the previous page of results, which
	 * is to be located using the given key, which must be the key of
	 * the first result on this page.
	 *
	 * @param keyOfFirstResultOnThisPage the key of the first result on this page
	 * @return a {@link KeyedPage} representing the next page of results
	 */
	@Internal
	public KeyedPage<R> previousPage(List<Comparable<?>> keyOfFirstResultOnThisPage) {
		if ( page.isFirst() ) {
			return null;
		}
		else {
			return new KeyedPage<>( keyDefinition, page.previous(), keyOfFirstResultOnThisPage, KEY_OF_FIRST_ON_NEXT_PAGE );
		}
	}

	/**
	 * Attach the given key to the specification of this page,
	 * with the given interpretation.
	 *
	 * @return a {@link KeyedPage} representing the same page
	 *         of results, but which may be located using the
	 *         given key
	 */
	@Internal
	public KeyedPage<R> withKey(List<Comparable<?>> key, KeyInterpretation interpretation) {
		return new KeyedPage<>( keyDefinition, page, key, interpretation );
	}

	public enum KeyInterpretation {
		KEY_OF_LAST_ON_PREVIOUS_PAGE,
		KEY_OF_FIRST_ON_NEXT_PAGE,
		NO_KEY
	}
}
