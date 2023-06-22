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
                    .unordered()
                    .ascending(title)
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
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
