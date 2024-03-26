/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Identifies a method of an abstract class or interface as defining
 * the signature of a <em>finder method</em>, with an implementation
 * generated automatically by the Hibernate Metamodel Generator.
 * <p>
 * For example, suppose the entity {@code Book} is defined as follows:
 * <pre>
 * &#64;Entity
 * class Book {
 *     &#64;Id String isbn;
 *     String title;
 *     ...
 * }
 * </pre>
 * <p>
 * Then we might define:
 * <pre>
 * &#064;Find
 * Book getBookForIsbn(String isbn);
 *
 * &#064;Find
 * List&lt;Book&gt; getBooksWithTitle(String title);
 * </pre>
 * <p>
 * Notice that:
 * <ul>
 * <li>the types and names of the method parameters exactly match the
 *     types and names of the corresponding fields of the entity.
 * <li>there's no special naming convention for the {@code @Find}
 *     methods&mdash;they may be named arbitrarily, and their names
 *     encode no semantics.
 * </ul>
 * <p>
 * It's even possible to query by a field of an embedded object:
 * <pre>
 * &#064;Find
 * List&lt;Book&gt; publishedBooks(String publisher$name);
 * </pre>
 * Here, {@code publisher$name} refers to the field {@code name} of
 * the {@code Book}'s {@code Publisher}.
 * <p>
 * The Metamodel Generator automatically creates an "implementation"
 * of every finder method in the static metamodel class {@code Books_}.
 * The generated method may be called according to the following
 * protocol:
 * <pre>
 * Book book = Books_.findBookByIsbn(session, isbn);
 * List&lt;Book&gt; books = Books_.getBooksWithTitle(session, String title);
 * </pre>
 * <p>
 * Notice the extra parameter of type {@code EntityManager} at the
 * start of the parameter list.
 * <p>
 * Alternatively, the type to which the annotated method belongs may
 * also declare an abstract method with no parameters which returns
 * one of the types {@link jakarta.persistence.EntityManager},
 * {@link org.hibernate.StatelessSession},
 * or {@link org.hibernate.Session}, for example:
 * <pre>
 * EntityManager entityManager();
 * </pre>
 * In this case:
 * <ul>
 * <li>the generated method is no longer {@code static},
 * <li>the generated method will use this method to obtain the
 *     session object, instead of having a parameter of type
 *     {@code EntityManager}, and
 * <li>the generated static metamodel class will actually implement
 *     the type which declares the method annotated {@code @Find}.
 * </ul>
 * <p>
 * Thus, the generated method may be called according to the following
 * protocol:
 * <pre>
 * Books books = new Books_(session);
 * Book book = books.getBookForIsbn(isbn);
 * List&lt;Book&gt; books = books.getBooksWithTitle(String title);
 * </pre>
 * <p>
 * This is reminiscent of traditional DAO-style repositories.
 * <p>
 * The return type of an annotated method must be an entity type {@code E},
 * or one of the following types:
 * <ul>
 * <li>{@link java.util.List java.util.List&lt;E&gt;},
 * <li>{@link java.util.stream.Stream java.util.stream.Stream&lt;E&gt;},
 * <li>{@link java.util.Optional java.util.Optional&lt;E&gt;},
 * <li>{@code io.smallrye.mutiny.Uni<E>}, when used with Hibernate Reactive,
 * <li>{@link org.hibernate.query.Query org.hibernate.query.Query&lt;E&gt;},
 * <li>{@link org.hibernate.query.SelectionQuery org.hibernate.query.SelectionQuery&lt;E&gt;},
 * <li>{@link jakarta.persistence.Query jakarta.persistence.Query&lt;E&gt;}, or
 * <li>{@link jakarta.persistence.TypedQuery jakarta.persistence.TypedQuery&lt;E&gt;}.
 * </ul>
 * <p>
 * The names and types of the parameters of a finder method must match
 * exactly with the names and types of persistent fields of the entity
 * type returned by the finder method.
 * <ul>
 * <li>If there is one parameter, and it matches the {@code @Id} or
 *     {@code @EmbeddedId} field of the entity, the finder method uses
 *     {@link jakarta.persistence.EntityManager#find(Class, Object)}
 *     to retrieve the entity.
 * <li>Similarly, if there is one parameter, and its type matches the
 *     type of the {@link jakarta.persistence.IdClass IdClass} of the
 *     entity, the finder method uses
 *     {@link jakarta.persistence.EntityManager#find(Class, Object)}
 *     to retrieve the entity. In this case the parameter name is not
 *     significant.
 * <li>If the parameters match exactly with the {@code @NaturalId}
 *     field or fields of the entity, the finder method uses
 *     {@link org.hibernate.Session#byNaturalId(Class)} to retrieve the
 *     entity.
 * <li>Otherwise, the finder method builds and executes a
 *     {@linkplain jakarta.persistence.criteria.CriteriaBuilder criteria
 *     query}.
 * </ul>
 * <p>
 * As an exception, the method may have at most one parameter of
 * type {@code EntityManager}, {@code Session},
 * {@code StatelessSession}, or {@code Mutiny.Session}. Furthermore,
 * if the finder method returns multiple results, that is, if its
 * return type is {@code List}, then it may also have:
 * <ul>
 * <li>a parameter with type {@code Page}, and/or
 * <li>a parameter with type {@code Order<? super E>},
 *     {@code List<Order<? super E>>}, or {@code Order<? super E>...}
 *     (varargs) where {@code E} is the entity type returned by the
 *     query.
 * </ul>
 * <p>
 * For example:
 * <pre>
 * &#064;Find
 * List&lt;Book&gt; getBooksWithTitle(String title, List&lt;Order&lt;Book&gt;&gt; order);
 * </pre>
 * <p>
 * As a further exception, a method might support key-based pagination.
 * Then it must have:
 * <ul>
 * <li>return type {@link org.hibernate.query.KeyedResultList}, and
 * <li>a parameter of type {@link org.hibernate.query.KeyedPage}.
 * </ul>
 *
 * @see HQL
 * @see SQL
 *
 * @author Gavin King
 * @since 6.3
 */
@Target(METHOD)
@Retention(CLASS)
@Incubating
public @interface Find {
	String[] enabledFetchProfiles() default {};
}
