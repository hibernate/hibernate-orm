/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/**
 * A {@link jakarta.persistence.FindOption} which requests a named
 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}.
 * <p>
 * An instance of this class may be obtained in a type safe way
 * from the static metamodel for the class annotated
 * {@link org.hibernate.annotations.FetchProfile @FetchProfile}
 * and passed as an option to
 * {@link Session#find(Class, Object, FindOption...) find()}.
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
 * The fetch profile may be requested like this:
 * <pre>
 * Book bookWithAuthors =
 *         session.find(Book.class, isbn, Book_._WithAuthors)
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
}
