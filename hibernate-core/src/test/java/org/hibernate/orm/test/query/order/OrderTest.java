package org.hibernate.orm.test.query.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = OrderTest.Book.class)
public class OrderTest {

    @Test void testAscendingDescending(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
        SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
        SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
        scope.inSession(session -> {
            List<String> titlesAsc = session.createSelectionQuery("from Book", Book.class)
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book", Book.class)
                    .descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book", Book.class)
                    .ascending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book", Book.class)
                    .descending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
                    .clearOrder()
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
        });
    }

    @Test void testAscendingDescendingWithPositionalParam(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
        SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
        SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
        scope.inSession(session -> {
            List<String> titlesAsc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .ascending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .descending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

            titlesAsc = session.createSelectionQuery("from Book where title like ?1 order by isbn asc", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book where title like ?1 order by isbn asc", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .clearOrder()
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
        });
    }

    @Test void testAscendingDescendingWithNamedParam(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
        SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
        SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
        scope.inSession(session -> {
            List<String> titlesAsc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .ascending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .descending(isbn).descending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

            titlesAsc = session.createSelectionQuery("from Book where title like :title order by isbn asc", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book where title like :title order by isbn asc", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .clearOrder()
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
        });
    }

    @Test void testAscendingDescendingBySelectElement(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> {
            List<?> titlesAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .ascending(2)
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<?> titlesDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .descending(2)
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<?> isbnAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .ascending(1).descending(2)
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<?> isbnDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .descending(1).descending(2)
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));
        });
    }

    @Entity(name="Book")
    static class Book {
        @Id String isbn;
        String title;

        Book(String isbn, String title) {
            this.isbn = isbn;
            this.title = title;
        }

        Book() {
        }
    }
}
