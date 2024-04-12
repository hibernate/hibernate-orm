/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

import java.util.Map;
import java.util.Optional;

/**
 * Loads an entity by its natural identifier, which may be a
 * composite value comprising more than one attribute of the
 * entity. If the entity has exactly one attribute annotated
 * {@link org.hibernate.annotations.NaturalId @NaturalId},
 * then {@link SimpleNaturalIdLoadAccess} may be used instead.
 * <p>
 * <pre>
 * Book book =
 *         session.byNaturalId(Book.class)
 *             .using(Book_.isbn, isbn)
 *             .using(Book_.printing, printing)
 *             .load();
 * </pre>
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @see Session#byNaturalId(Class)
 * @see org.hibernate.annotations.NaturalId
 * @see SimpleNaturalIdLoadAccess
 */
public interface NaturalIdLoadAccess<T> {
	/**
	 * Specify the {@linkplain LockOptions lock options} to use when
	 * querying the database.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Override the associations fetched by default by specifying
	 * the complete list of associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default NaturalIdLoadAccess<T> withFetchGraph(RootGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default NaturalIdLoadAccess<T> withLoadGraph(RootGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 *
	 * @since 6.3
	 */
	NaturalIdLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be enabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 6.3
	 */
	NaturalIdLoadAccess<T> enableFetchProfile(String profileName);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be disabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 6.3
	 */
	NaturalIdLoadAccess<T> disableFetchProfile(String profileName);

	/**
	 * Add a {@link org.hibernate.annotations.NaturalId @NaturalId}
	 * attribute value in a typesafe way.
	 *
	 * @param attribute A typesafe reference to an attribute of the
	 *                  entity that is annotated {@code @NaturalId}
	 * @param value The value of the attribute
	 *
	 * @return {@code this}, for method chaining
	 */
	<X> NaturalIdLoadAccess<T> using(SingularAttribute<? super T, X> attribute, X value);

	/**
	 * Add a {@link org.hibernate.annotations.NaturalId @NaturalId}
	 * attribute value.
	 * 
	 * @param attributeName The name of an attribute of the entity
	 *                      that is annotated {@code @NaturalId}
	 * @param value The value of the attribute
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdLoadAccess<T> using(String attributeName, Object value);

	/**
	 * Set multiple {@link org.hibernate.annotations.NaturalId @NaturalId}
	 * attribute values at once. An even number of arguments is expected,
	 * with each attribute name followed by its value, for example:
	 * <pre>
	 * Book book =
	 *         session.byNaturalId(Book.class)
	 *             .using(Map.of(Book_.ISBN, isbn, Book_.PRINTING, printing))
	 *             .load();
	 * </pre>
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdLoadAccess<T> using(Map<String,?> mappings);

	/**
	 * Set multiple {@link org.hibernate.annotations.NaturalId @NaturalId}
	 * attribute values at once. An even number of arguments is expected,
	 * with each attribute name followed by its value, for example:
	 * <pre>
	 * Book book =
	 *         session.byNaturalId(Book.class)
	 *             .using(Book_.ISBN, isbn, Book_.PRINTING, printing)
	 *             .load();
	 * </pre>
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #using(Map)} with {@link Map#of}, which is
	 *             slightly more typesafe
	 */
	@Deprecated(since = "6.3")
	NaturalIdLoadAccess<T> using(Object... mappings);

	/**
	 * Determines if cached natural id cross-references are synchronized
	 * before query execution with unflushed modifications made in memory
	 * to {@linkplain org.hibernate.annotations.NaturalId#mutable mutable}
	 * natural ids.
	 * <p>
	 * By default, every cached cross-reference is updated to reflect any
	 * modification made in memory.
	 * <p>
	 * Here "synchronization" means updating the natural id to
	 * primary key cross-reference maintained by the session. When
	 * enabled, before performing the lookup, Hibernate will check
	 * all entities associated with the session of the given type to
	 * determine if any natural id values have changed and, if so,
	 * update the cross-references.
	 * <p>
	 * There's some cost associated with this synchronization, so if
	 * it's completely certain the no natural ids have been modified,
	 * synchronization may be safely disabled to avoid that cost.
	 * Disabling this setting when natural id values <em>have</em>
	 * been modified may lead to incorrect results.
	 *
	 * @param enabled Should synchronization be performed?
	 *                {@code true} indicates synchronization will be performed;
	 *                {@code false} indicates it will be circumvented.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdLoadAccess<T> setSynchronizationEnabled(boolean enabled);

	/**
	 * Return the persistent instance with the full natural id specified
	 * by previous calls to {@link #using}. This method might return a
	 * proxied instance that is initialized on-demand, when a non-identifier
	 * method is accessed.
	 * <p>
	 * You should not use this method to determine if an instance exists;
	 * to check for existence, use {@link #load} instead. Use this method
	 * only to retrieve an instance that you assume exists, where
	 * non-existence would be an actual error.
	 *
	 * @return the persistent instance or proxy
	 */
	T getReference();

	/**
	 * Return the persistent instance with the full natural id specified
	 * by previous calls to {@link #using}, or {@code null} if there is no
	 * such persistent instance. If the instance is already associated with
	 * the session, return that instance, initializing it if needed. This
	 * method never returns an uninitialized instance.
	 *
	 * @return The persistent instance or {@code null} 
	 */
	T load();

	/**
	 * Just like {@link #load}, except that here an {@link Optional} is
	 * returned.
	 *
	 * @return The persistent instance, if one, as an {@link Optional}
	 */
	Optional<T> loadOptional();

}
