package org.hibernate.orm.test.query.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = OrderParamTest.Book.class)
public class OrderParamTest {

    @Test
    void test(SessionFactoryScope scope) {
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> session.createSelectionQuery("select isbn, title from Book order by ?1")
                .setParameter(1, 1).getResultList());
        scope.inSession(session -> session.createSelectionQuery("select isbn, title from Book order by ?1")
                .setParameter(1, 2).getResultList());
    }

    @Entity(name="Book")
    static class Book {
        @Id
        String isbn;
        String title;

        Book(String isbn, String title) {
            this.isbn = isbn;
            this.title = title;
        }

        Book() {
        }
    }
}
