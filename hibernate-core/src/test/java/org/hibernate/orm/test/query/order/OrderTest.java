package org.hibernate.orm.test.query.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.Order;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hibernate.query.Order.asc;
import static org.hibernate.query.Order.by;
import static org.hibernate.query.Order.desc;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;
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
                    .setOrder(asc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(desc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(asc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(desc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

//            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(asc(title))
//                    .getResultList()
//                    .stream().map(book -> book.title)
//                    .collect(toList());
//            assertEquals("Hibernate in Action", titlesAsc.get(1));
//            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(emptyList())
                    .setOrder(asc(title))
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
                    .setOrder(asc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .setOrder(Order.desc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .setOrder(List.of(asc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book where title like ?1", Book.class)
                    .setParameter(1, "%Hibernate%")
                    .setOrder(List.of(desc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

//            titlesAsc = session.createSelectionQuery("from Book where title like ?1 order by isbn asc", Book.class)
//                    .setParameter(1, "%Hibernate%")
//                    .setOrder(asc(title))
//                    .getResultList()
//                    .stream().map(book -> book.title)
//                    .collect(toList());
//            assertEquals("Hibernate in Action", titlesAsc.get(1));
//            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book where title like ?1 order by isbn asc", Book.class)
                    .setParameter(1, "%Hibernate%")
//                    .setOrder(emptyList())
                    .setOrder(asc(title))
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
                    .setOrder(asc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .setOrder(desc(title))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .setOrder(List.of(asc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book where title like :title", Book.class)
                    .setParameter("title", "%Hibernate%")
                    .setOrder(List.of(desc(isbn), desc(title)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

//            titlesAsc = session.createSelectionQuery("from Book where title like :title order by isbn asc", Book.class)
//                    .setParameter("title", "%Hibernate%")
//                    .setOrder(asc(title))
//                    .getResultList()
//                    .stream().map(book -> book.title)
//                    .collect(toList());
//            assertEquals("Hibernate in Action", titlesAsc.get(1));
//            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book where title like :title order by isbn asc", Book.class)
                    .setParameter("title", "%Hibernate%")
//                    .setOrder(emptyList())
                    .setOrder(asc(title))
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
                    .setOrder(asc(2))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<?> titlesDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(desc(2))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<?> isbnAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(asc(1), desc(2)))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<?> isbnDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(desc(1), desc(2)))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));
        });
    }

    @Test void testAscendingDescendingCaseInsensitive(SessionFactoryScope scope) {
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
                    .setOrder(asc(title).ignoringCase())
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(desc(title).ignoringCase())
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(asc(isbn).ignoringCase(), desc(title).ignoringCase()))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(desc(isbn).ignoringCase(), desc(title).ignoringCase()))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

//            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(asc(title))
//                    .getResultList()
//                    .stream().map(book -> book.title)
//                    .collect(toList());
//            assertEquals("Hibernate in Action", titlesAsc.get(1));
//            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(emptyList())
                    .setOrder(asc(title).ignoringCase())
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
        });
    }

    @Test void testAscendingDescendingCaseInsensitiveLongForm(SessionFactoryScope scope) {
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
                    .setOrder(by(title, ASCENDING, true))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<String> titlesDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(by(title, DESCENDING, true))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<String> isbnAsc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(by(isbn, ASCENDING, true), by(title, DESCENDING, true)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<String> isbnDesc = session.createSelectionQuery("from Book", Book.class)
                    .setOrder(List.of(by(isbn, DESCENDING, true), by(title, DESCENDING, true)))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

//            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(asc(title))
//                    .getResultList()
//                    .stream().map(book -> book.title)
//                    .collect(toList());
//            assertEquals("Hibernate in Action", titlesAsc.get(1));
//            assertEquals("Java Persistence with Hibernate", titlesAsc.get(0));
            titlesAsc = session.createSelectionQuery("from Book order by isbn asc", Book.class)
//                    .setOrder(emptyList())
                    .setOrder(by(title, ASCENDING, true))
                    .getResultList()
                    .stream().map(book -> book.title)
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
        });
    }

    @Test void testAscendingDescendingBySelectElementCaseInsensitive(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> {
            List<?> titlesAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(asc(2).ignoringCase())
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<?> titlesDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(desc(2).ignoringCase())
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<?> isbnAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(asc(1).ignoringCase(), desc(2).ignoringCase()))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<?> isbnDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(desc(1).ignoringCase(), desc(2).ignoringCase()))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnDesc.get(0));
            assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));
        });
    }

    @Test void testAscendingDescendingBySelectElementCaseInsensitiveLongForm(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createMutationQuery("delete Book").executeUpdate() );
        scope.inTransaction( session -> {
            session.persist(new Book("9781932394153", "Hibernate in Action"));
            session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
        });
        scope.inSession(session -> {
            List<?> titlesAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(by(2, ASCENDING, true))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesAsc.get(0));
            assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

            List<?> titlesDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(by(2, DESCENDING, true))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", titlesDesc.get(1));
            assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

            List<?> isbnAsc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(by(1, ASCENDING, true), by(2, DESCENDING, true)))
                    .getResultList()
                    .stream().map(book -> book[1])
                    .collect(toList());
            assertEquals("Hibernate in Action", isbnAsc.get(1));
            assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

            List<?> isbnDesc = session.createSelectionQuery("select isbn, title from Book", Object[].class)
                    .setOrder(List.of(by(1, DESCENDING, true), by(2, DESCENDING, true)))
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
