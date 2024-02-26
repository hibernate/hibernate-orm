/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * Support for pagination based on a unique key of the result
 * set instead of the {@link Page#getFirstResult() offset}.
 *
 * @since 6.5
 *
 * @author Gavin King
 */
public class KeyedPage<R> {
	private final List<Order<? super R>> keyDefinition;
	private final Page page;
	private final List<Comparable<?>> key;

	KeyedPage(List<Order<? super R>> keyDefinition, Page page) {
		this( keyDefinition, page, null );
	}

	public KeyedPage(List<Order<? super R>> keyDefinition, Page page, List<Comparable<?>> key) {
		this.page = page;
		this.keyDefinition = unmodifiableList(keyDefinition);
		this.key = key;
	}

	public List<Order<? super R>> getKeyDefinition() {
		return keyDefinition;
	}

	public Page getPage() {
		return page;
	}

	/**
	 * Null key indicates that the {@linkplain Page#getNumber() page number}
	 * should be used. This is useful to obtain an initial page of results.
	 */
	public List<Comparable<?>> getKey() {
		return key;
	}
}
