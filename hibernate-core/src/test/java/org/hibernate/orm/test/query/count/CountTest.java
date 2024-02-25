package org.hibernate.orm.test.query.count;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {CountTest.Book.class, CountTest.Author.class, CountTest.Publisher.class})
public class CountTest {

    @Test void testCount(SessionFactoryScope scope) {
        scope.inTransaction(session -> session.createMutationQuery("delete Book").executeUpdate());
        scope.inTransaction(session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> {
            assertEquals(1L,
                    session.createSelectionQuery("from Book where title like 'Hibernate%'", Book.class)
                            .getResultCount());
            assertEquals(2L,
                    session.createSelectionQuery("from Book where title like '%Hibernate%'", Book.class)
                            .getResultCount());
            assertEquals(1L,
                    session.createSelectionQuery("select isbn, title from Book where title like 'Hibernate%'", String.class)
                            .getResultCount());
            assertEquals(1L,
                    session.createSelectionQuery("from Book where title like :title", Book.class)
                            .setParameter("title", "Hibernate%")
                            .getResultCount());
            assertEquals(0L,
                    session.createSelectionQuery("from Book where title like :title", Book.class)
                            .setParameter("title", "Jibernate%")
                            .getResultCount());
            assertEquals(2L,
                    session.createSelectionQuery("select title from Book where title like '%Hibernate' union select title from Book where title like 'Hibernate%'", String.class)
                            .getResultCount());
            assertEquals(2L,
                    session.createSelectionQuery("from Book left join fetch authors left join fetch publisher", Book.class)
                            .getResultCount());
            assertEquals(0L,
                    session.createSelectionQuery("from Book join fetch publisher", Book.class)
                            .getResultCount());
        });
    }

    @Test void testCountNative(SessionFactoryScope scope) {
        scope.inTransaction(session -> session.createMutationQuery("delete Book").executeUpdate());
        scope.inTransaction(session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> {
            assertEquals(2L,
                    session.createNativeQuery("select title from books", String.class)
                            .setMaxResults(1)
                            .getResultCount());
            assertEquals(1L,
                    session.createNativeQuery("select title from books where title like :title", String.class)
                            .setParameter("title", "Hibernate%")
                            .getResultCount());
            assertEquals(2L,
                    session.createNativeQuery("select title from books", String.class)
                            .setMaxResults(1)
                            .getResultCount());
        });
    }

    @Test void testCountCriteria(SessionFactoryScope scope) {
        scope.inTransaction(session -> session.createMutationQuery("delete Book").executeUpdate());
        scope.inTransaction(session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        CriteriaBuilder builder = scope.getSessionFactory().getCriteriaBuilder();
        scope.inSession(session -> {
            CriteriaQuery<Book> query1 = builder.createQuery(Book.class);
            query1.where( builder.like( query1.from(Book.class).get("title"), "Hibernate%" ) );
            assertEquals(1L,
                    session.createQuery(query1)
                            .getResultCount());
            CriteriaQuery<Book> query2 = builder.createQuery(Book.class);
            query2.from(Book.class);
            assertEquals(2L,
                    session.createQuery(query2)
                            .setMaxResults(1)
                            .getResultCount());
            CriteriaQuery<Book> query3 = builder.createQuery(Book.class);
            ParameterExpression<String> parameter = builder.parameter(String.class);
            query3.where( builder.like( query3.from(Book.class).get("title"), parameter ) );
            assertEquals(1L,
                    session.createQuery(query3)
                            .setParameter(parameter, "Hibernate%")
                            .getResultCount());
            CriteriaQuery<Book> query4 = builder.createQuery(Book.class);
            Root<Book> book = query4.from(Book.class);
            book.fetch("authors", JoinType.INNER);
            assertEquals(0L,
                    session.createQuery(query4)
                            .getResultCount());
        });
    }

    @Entity(name="Book")
    @Table(name = "books")
    static class Book {
        @Id String isbn;
        String title;

        @ManyToMany
        List<Author> authors;

        @ManyToOne
        Publisher publisher;

        Book(String isbn, String title) {
            this.isbn = isbn;
            this.title = title;
        }

        Book() {
        }
    }

    @Entity(name="Author")
    @Table(name = "authors")
    static class Author {
        @Id String ssn;
        String name;
    }

    @Entity(name="Publisher")
    @Table(name = "pubs")
    static class Publisher {
        @Id String name;
    }
}
