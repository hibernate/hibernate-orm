/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Annotations used to drive annotation processors:
 * <ul>
 * <li>{@link org.hibernate.annotations.processing.Find @Find}
 *     is used to generate finder methods using the Metamodel
 *     Generator,
 * <li>{@link org.hibernate.annotations.processing.HQL @HQL}
 *     and {@link org.hibernate.annotations.processing.SQL @SQL}
 *     are used to generate query methods using the Metamodel
 *     Generator, and
 * <li>{@link org.hibernate.annotations.processing.CheckHQL}
 *     instructs the Query Validator to check all HQL queries
 *     in the annotated package or type.
 * </ul>
 * <p>
 * Annotations in this package control Hibernate's compile-time
 * tooling, and depend on the use of the annotation processors
 * in the {@code hibernate-jpamodelgen} or {@code query-validator}
 * modules. If the appropriate annotation processor is not enabled
 * at build time, these annotations have no effect.
 * <p>
 * Finder methods and query methods are usually declared by an
 * interface, say {@code Queries}, and their implementations
 * are generated into a class whose name follows the convention
 * of the JPA static metamodel, that is, {@code Queries_}.
 * <ul>
 * <li>If the interface declares a method which returns an
 *     {@code EntityManager}, Hibernate session, stateless
 *     session, or reactive session, the generated class
 *     implements the interface, {@code class Queries_ implements
 *     Queries}, the generated methods are instance methods, and
 *     the session must be provided to the class constructor,
 *     possibly via dependency injection.
 * <li>Otherwise, if there is no such method, the generated class
 *     is non-instantiable, {@code abstract class Queries_}, the
 *     generated methods are declared {@code static}, and the
 *     session must be provided as an argument by the caller of
 *     the generated methods.
 * </ul>
 * <p>
 * For example, this code defines a DAO-style repository object:
 * <pre>
 * package org.example;
 *
 * import org.hibernate.StatelessSession;
 * import org.hibernate.annotations.processing.Find;
 * import org.hibernate.annotations.processing.HQL;
 * import org.hibernate.query.Order;
 *
 * import java.util.List;
 *
 * interface BookRepository {
 *
 *   StatelessSession session();
 *
 *   &#064;Find
 *   Book bookByIsbn(String isbn);
 *
 *   &#064;Find
 *   List&lt;Book&gt; booksByPublisher(long publisher$id);
 *
 *   &#064;HQL("where title like :title")
 *   List&lt;Book&gt; booksByTitle(String title, Order&lt;Book&gt; order);
 *
 * }
 * </pre>
 * <p>
 * The Metamodel Generator produces this implementation when 
 * the interface is compiled:
 * <pre>
 * package org.example;
 *
 * import jakarta.annotation.Generated;
 * import jakarta.annotation.Nonnull;
 * import jakarta.enterprise.context.Dependent;
 * import jakarta.inject.Inject;
 * import jakarta.persistence.metamodel.StaticMetamodel;
 * import java.util.List;
 * import org.hibernate.StatelessSession;
 * import org.hibernate.query.Order;
 *
 * &#064;Dependent 
 * &#064;StaticMetamodel(BookRepository.class) 
 * &#064;Generated("org.hibernate.processor.HibernateProcessor")
 * public class BookRepository_ implements BookRepository {
 *
 *
 *    &#064;Override 
 *    public List&lt;Book&gt; booksByTitle(String title, Order&lt;Book&gt; order) {
 *         return session.createQuery(BOOKS_BY_TITLE_String, Book.class)
 *                 .setParameter("title", title)
 *                 .setOrder(order)
 *                 .getResultList();
 *    }
 *
 *    &#064;Override 
 *    public Book bookByIsbn(@Nonnull String isbn) {
 *         return session.get(Book.class, isbn);
 *    }
 *
 *     private final @Nonnull StatelessSession session;
 *
 *    &#064;Inject 
 *    public BookRepository_(@Nonnull StatelessSession session) {
 *         this.session = session;
 *    }
 *
 *    public @Nonnull StatelessSession session() {
 *         return session;
 *    }
 *
 *    &#064;Override 
 *    public List&lt;Book&gt; booksByPublisher(long publisher$id) {
 *         var builder = session.getFactory().getCriteriaBuilder();
 *         var query = builder.createQuery(Book.class);
 *         var entity = query.from(Book.class);
 *         query.where(
 *                 builder.equal(entity.get(Book_.publisher).get(Publisher_.id), publisher$id)
 *         );
 *         return session.createQuery(query).getResultList();
 *    }
 *
 *     static final String BOOKS_BY_TITLE_String = "where title like :title";
 *
 * }
 * </pre>
 * <p>
 * The exact annotations included in the generated code depend on
 * the libraries available during compilation. The code above was
 * produced with CDI and Jakarta annotations on the build path,
 * and so the repository is an injectable CDI bean and uses
 * {@code @Nonnull} to indicate required parameters.
 */
@Incubating
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;
