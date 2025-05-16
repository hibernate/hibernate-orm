/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;
import org.hibernate.query.SelectionQuery;

/**
 * A {@link jakarta.persistence.FindOption} which requests a named
 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}.
 * <p>
 * An instance of this class may be obtained in a type safe way
 * from the static metamodel for the class annotated
 * {@link org.hibernate.annotations.FetchProfile @FetchProfile}.
 * <p>
 * For example, this class defines a fetch profile:
 * <pre>
 * &#064;Entity
 * &#064;FetchProfile(name = "WithAuthors")
 * class Book {
 *     ...
 *
 *     &#064;ManyToMany
 *     &#064;FetchProfileOverride(profile = Book_.PROFILE_WITH_AUTHORS)
 *     Set&lt;Author&gt; authors;
 * }
 * </pre>
 * <p>
 * An {@code EnabledFetchProfile} may be obtained from the static
 * metamodel for the entity {@code Book} and passed as an option to
 * {@link Session#find(Class, Object, FindOption...) find()}.
 * <pre>
 * Book bookWithAuthors =
 *         session.find(Book.class, isbn, Book_._WithAuthors)
 * </pre>
 * Alternatively, it may be {@linkplain #enable(Session) applied}
 * to a {@code Session} or {@code Query}.
 * <pre>
 * Book_._WithAuthors.enable(session);
 * Book bookWithAuthors = session.find(Book.class, isbn);
 * </pre>
 * <p>
 * When the static metamodel is not used, an {@code EnabledFetchProfile}
 * may be instantiated directly, passing the name of the fetch profile
 * as a string.
 * <pre>
 * Book bookWithAuthors =
 *         session.find(Book.class, isbn,
 *                      new EnabledFetchProfile("WithAuthors"))
 * </pre>
 *
 * @param profileName the {@linkplain org.hibernate.annotations.FetchProfile#name profile name}
 *
 * @since 7.0
 *
 * @see org.hibernate.annotations.FetchProfile
 * @see Session#find(Class, Object, FindOption...)
 *
 * @author Gavin King
 */
public record EnabledFetchProfile(String profileName)
		implements FindOption {

	/**
	 * Enable the fetch profile represented by this
	 * object in the given session.
	 */
	public void enable(Session session) {
		session.enableFetchProfile(profileName);
	}

	/**
	 * Enable the fetch profile represented by this
	 * object during execution of the given query.
	 */
	public void enable(SelectionQuery<?> query) {
		query.enableFetchProfile(profileName);
	}
}
