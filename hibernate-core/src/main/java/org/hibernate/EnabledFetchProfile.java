/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;
import org.hibernate.query.SelectionQuery;

/// A [jakarta.persistence.FindOption] which represents a named
/// [fetch profile][org.hibernate.annotations.FetchProfile].
///
/// An instance of this class may be obtained in a type safe way
/// from the static metamodel for the class annotated with the
/// [@FetchProfile][org.hibernate.annotations.FetchProfile].
///
/// For example, this class defines a fetch profile:
/// ```java
/// @Entity
/// @FetchProfile(name = "WithAuthors")
/// class Book {
/// 	...
///     @ManyToMany
/// 	@FetchProfileOverride(profile = Book_.PROFILE_WITH_AUTHORS)
/// 	Set<Author> authors;
/// }
/// ```
///
/// An `EnabledFetchProfile` may be obtained from the static
/// metamodel for the entity {@code Book} and passed as an option to
/// [Session#find(Class, Object, FindOption...)].
///
/// ```java
/// Book bookWithAuthors =
/// 	session.find(Book.class, isbn, Book_._WithAuthors)
/// ```
///
/// Alternatively, it may be [applied][#enable(Session)]
/// to a `Session` or `Query`.
///
/// ```java
/// Book_._WithAuthors.enable(session);
/// Book bookWithAuthors = session.find(Book.class, isbn);
/// ```
///
/// When the static metamodel is not used, an `EnabledFetchProfile`
/// may be instantiated directly, passing the name of the fetch profile
/// as a string.
///
/// ```java
/// Book bookWithAuthors =
/// 	session.find(Book.class, isbn,
/// 		new EnabledFetchProfile("WithAuthors"))
/// ```
///
/// @param profileName the [profile name][org.hibernate.annotations.FetchProfile#name].
///
/// @since 7.0
///
/// @see org.hibernate.annotations.FetchProfile
/// @see Session#find(Class, Object, FindOption...)
///
/// @author Gavin King
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
