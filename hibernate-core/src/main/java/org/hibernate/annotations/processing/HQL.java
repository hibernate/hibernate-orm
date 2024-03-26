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
 * the signature of a method which is used to execute the given
 * {@linkplain #value HQL query}, with an implementation generated
 * automatically by the Hibernate Metamodel Generator.
 * <p>
 * For example:
 * <pre>
 * public interface Books {
 *     &#64;HQL("where isbn = :isbn")
 *     Book findBookByIsbn(String isbn);
 *
 *     &#64;HQL("where title like ?1 order by title offset ?3 fetch first ?2 rows only")
 *     List&lt;Book&gt; findBooksByTitleWithPagination(String title, int max, int start);
 *
 *     &#64;HQL("select isbn, title, author.name from Book order by isbn")
 *     List&lt;BookSummary&gt; summarizeBooksWithPagination(Page page);
 *
 *     &#64;HQL("where title like ?1")
 *     TypedQuery&lt;Book&gt; createBooksByTitleQuery(String title);
 * }
 * </pre>
 * <p>
 * The Metamodel Generator automatically creates an "implementation"
 * of these methods in the static metamodel class {@code Books_}.
 * The generated methods may be called according to the following
 * protocol:
 * <pre>
 * Book book = Books_.findBookByIsbn(session, isbn);
 * List&lt;Book&gt; books = Books_.findBooksByTitleWithPagination(session, pattern, 10, 0);
 * </pre>
 * <p>
 * Notice the extra parameter of type {@code EntityManager} at the
 * start of the parameter list.
 * <p>
 * Alternatively, the type to which the annotated method belongs may
 * also declare an abstract method with no parameters which returns
 * one of the types {@link jakarta.persistence.EntityManager},
 * {@link org.hibernate.Session},
 * {@link org.hibernate.StatelessSession}, or {@code Mutiny.Session},
 * for example:
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
 *     the type which declares the method annotated {@code @SQL}.
 * </ul>
 * <p>
 * Thus, the generated methods may be called according to the following
 * protocol:
 * <pre>
 * Books books = new Books_(session);
 * Book book = books.findBookByIsbn(isbn);
 * List&lt;Book&gt; books = books.findBooksByTitleWithPagination(pattern, 10, 0);
 * </pre>
 * <p>
 * This is reminiscent of traditional DAO-style repositories.
 * <p>
 * For a {@code select} query, the return type of an annotated method
 * must be:
 * <ul>
 * <li>an entity type, or {@link java.util.Optional Optional&lt;E&gt;}
 *     where {@code E} is an entity type,
 * <li>{@link java.util.List List&lt;E&gt;} or
 *     {@link java.util.stream.Stream Stream&lt;E&gt;}
 *     where {@code E} is an entity type,
 * <li>{@link java.util.List List&lt;Object[]&gt;} or
 *     {@link java.util.stream.Stream Stream&lt;Object[]&gt;}
 * <li>a record type or JavaBean class with a constructor signature
 *     matching the types in the query {@code select} list, or
 *     {@link java.util.List List&lt;R&gt;} where {@code R} is such
 *     a type,
 * <li>{@code io.smallrye.mutiny.Uni}, when used with Hibernate Reactive,
 * <li>{@link org.hibernate.query.Query},
 * <li>{@link org.hibernate.query.SelectionQuery},
 * <li>{@link jakarta.persistence.Query}, or
 * <li>{@link jakarta.persistence.TypedQuery}.
 * </ul>
 * <p>
 * For an {@code insert}, {@code update}, or {@code delete} query,
 * the return type of the annotated method must be {@code int},
 * {@code boolean}, or {@code void}.
 * <p>
 * The method parameters must match the parameters of the HQL query,
 * either by name or by position:
 * <ul>
 * <li>an ordinal query parameter of form {@code ?n} is matched to
 *     the <em>n</em>th parameter of the method, and
 * <li>a named query parameter of form {@code :name} is matched to
 *     the method parameter {@code name}.
 * </ul>
 * <p>
 * As an exception, the method may have:
 * <ul>
 * <li>a parameter of type {@code EntityManager}, {@code Session},
 *     {@code StatelessSession}, or {@code Mutiny.Session},
 * <li>a parameter with type {@code Page}, and/or
 * <li>a parameter with type {@code Order<? super E>},
 *     {@code List<Order<? super E>>}, or {@code Order<? super E>...}
 *     (varargs) where {@code E} is the entity type returned by the
 *     query.
 * </ul>
 * <p>
 * For example:
 * <pre>
 * &#64;HQL("where title like :title)
 * List&lt;Book&gt; findBooksByTitleWithPagination(String title, Page page, Order&lt;Book&gt; order);
 * </pre>
 * <p>
 * As a further exception, a method might support key-based pagination.
 * Then it must have:
 * <ul>
 * <li>return type {@link org.hibernate.query.KeyedResultList}, and
 * <li>a parameter of type {@link org.hibernate.query.KeyedPage}.
 * </ul>
 * <p>
 * Queries specified using this annotation are always validated by
 * the Metamodel Generator, and so it isn't necessary to specify the
 * {@link CheckHQL} annotation.
 *
 * @apiNote Instantiations with {@code select new} are not currently
 *          type-checked at build time, and so use of this syntax is
 *          not recommended. Nor, however, is this syntax necessary.
 *          Hibernate is able to automatically match the elements of
 *          the {@code select} list with a constructor of the method
 *          return type, which is much less verbose and just as type
 *          safe.
 *
 * @see SQL
 * @see Find
 *
 * @author Gavin King
 * @author Yanming Zhou
 * @since 6.3
 */
@Target(METHOD)
@Retention(CLASS)
@Incubating
public @interface HQL {
	String value();
}
