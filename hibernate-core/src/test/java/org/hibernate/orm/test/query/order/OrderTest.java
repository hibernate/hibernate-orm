/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.Order;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.query.Order.asc;
import static org.hibernate.query.Order.by;
import static org.hibernate.query.Order.desc;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel(annotatedClasses = OrderTest.Book.class)
public class OrderTest {

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist(new Book("9781932394153", "Hibernate in Action"));
			session.persist(new Book("9781617290459", "Java Persistence with Hibernate"));
		});
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test void testAscendingDescending(SessionFactoryScope scope) {
		EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
		SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
		scope.inSession(session -> {
			List<String> titlesAsc = SelectionSpecification.create( Book.class, "from Book" )
					.sort(asc(title))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<String> titlesDesc = SelectionSpecification.create( Book.class, "from Book" )
					.sort(desc(title))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<String> isbnAsc = SelectionSpecification.create( Book.class, "from Book" )
					.sort(asc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<String> isbnDesc = SelectionSpecification.create( Book.class, "from Book" )
					.sort(desc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

			titlesAsc = SelectionSpecification.create( Book.class, "from Book order by isbn asc" )
					.resort(asc(title))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
		});
	}

	@Test void testAscendingDescendingWithPositionalParam(SessionFactoryScope scope) {
		EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
		SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
		scope.inSession(session -> {
			List<String> titlesAsc = SelectionSpecification.create( Book.class, "from Book where title like ?1" )
					.sort(asc(title))
					.createQuery( session )
					.setParameter(1, "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<String> titlesDesc = SelectionSpecification.create(Book.class, "from Book where title like ?1")
					.sort(Order.desc(title))
					.createQuery( session )
					.setParameter(1, "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<String> isbnAsc = SelectionSpecification.create(Book.class, "from Book where title like ?1")
					.sort(asc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.setParameter(1, "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<String> isbnDesc = SelectionSpecification.create(Book.class, "from Book where title like ?1")
					.sort(desc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.setParameter(1, "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

			titlesAsc = SelectionSpecification.create(Book.class, "from Book where title like ?1 order by isbn asc")
					.resort(asc(title))
					.createQuery( session )
					.setParameter(1, "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
		});
	}

	@Test void testAscendingDescendingWithNamedParam(SessionFactoryScope scope) {
		EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
		SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
		scope.inSession(session -> {
			List<String> titlesAsc = SelectionSpecification.create(Book.class, "from Book where title like :title")
					.sort(asc(title))
					.createQuery( session )
					.setParameter("title", "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<String> titlesDesc = SelectionSpecification.create(Book.class,"from Book where title like :title")
					.sort(desc(title))
					.createQuery( session )
					.setParameter("title", "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<String> isbnAsc = SelectionSpecification.create(Book.class, "from Book where title like :title")
					.sort(asc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.setParameter("title", "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<String> isbnDesc = SelectionSpecification.create(Book.class, "from Book where title like :title")
					.sort(desc(isbn))
					.sort(desc(title))
					.createQuery( session )
					.setParameter("title", "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

			titlesAsc = SelectionSpecification.create(Book.class, "from Book where title like :title order by isbn asc")
					.resort(asc(title))
					.createQuery( session )
					.setParameter("title", "%Hibernate%")
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
		});
	}

	@NotImplementedYet(reason = "Support for explicit select lists not implemented yet for SelectionSpecification")
	@Test void testAscendingDescendingBySelectElement(SessionFactoryScope scope) {
		scope.inSession(session -> {
			List<?> titlesAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(asc(2))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<?> titlesDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(desc(2))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<?> isbnAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(asc(1))
					.sort(desc(2))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<?> isbnDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(desc(1))
					.sort(desc(2))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));
		});
	}

	@Test void testAscendingDescendingCaseInsensitive(SessionFactoryScope scope) {
		EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
		SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
		scope.inSession(session -> {
			List<String> titlesAsc = SelectionSpecification.create(Book.class, "from Book")
					.sort(asc(title).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<String> titlesDesc = SelectionSpecification.create(Book.class, "from Book")
					.sort(desc(title).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<String> isbnAsc = SelectionSpecification.create(Book.class, "from Book")
					.sort(asc(isbn).ignoringCase())
					.sort(desc(title).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<String> isbnDesc = SelectionSpecification.create(Book.class, "from Book")
					.sort(desc(isbn).ignoringCase())
					.sort(desc(title).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

			titlesAsc = SelectionSpecification.create(Book.class, "from Book order by isbn asc")
					.resort(asc(title).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
		});
	}

	@Test void testAscendingDescendingCaseInsensitiveLongForm(SessionFactoryScope scope) {
		EntityDomainType<Book> bookType = scope.getSessionFactory().getJpaMetamodel().findEntityType(Book.class);
		SingularAttribute<? super Book, ?> title = bookType.findSingularAttribute("title");
		SingularAttribute<? super Book, ?> isbn = bookType.findSingularAttribute("isbn");
		scope.inSession(session -> {
			List<String> titlesAsc = SelectionSpecification.create(Book.class, "from Book")
					.sort(by(title, ASCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<String> titlesDesc = SelectionSpecification.create(Book.class, "from Book")
					.sort(by(title, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<String> isbnAsc = SelectionSpecification.create(Book.class, "from Book")
					.sort(by(isbn, ASCENDING, true))
					.sort(by(title, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<String> isbnDesc = SelectionSpecification.create(Book.class, "from Book")
					.sort(by(isbn, DESCENDING, true))
					.sort(by(title, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));

			titlesAsc = SelectionSpecification.create(Book.class, "from Book order by isbn asc")
					.resort(by(title, ASCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book.title)
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));
		});
	}

	@NotImplementedYet(reason = "Support for explicit select lists not implemented yet for SelectionSpecification")
	@Test void testAscendingDescendingBySelectElementCaseInsensitive(SessionFactoryScope scope) {
		scope.inSession(session -> {
			List<?> titlesAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(asc(2).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<?> titlesDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(desc(2).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<?> isbnAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(asc(1).ignoringCase())
					.sort(desc(2).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<?> isbnDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(desc(1).ignoringCase())
					.sort(desc(2).ignoringCase())
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", isbnDesc.get(0));
			assertEquals("Java Persistence with Hibernate", isbnDesc.get(1));
		});
	}

	@NotImplementedYet(reason = "Support for explicit select lists not implemented yet for SelectionSpecification")
	@Test void testAscendingDescendingBySelectElementCaseInsensitiveLongForm(SessionFactoryScope scope) {
		scope.inSession(session -> {
			List<?> titlesAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(by(2, ASCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesAsc.get(0));
			assertEquals("Java Persistence with Hibernate", titlesAsc.get(1));

			List<?> titlesDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(by(2, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", titlesDesc.get(1));
			assertEquals("Java Persistence with Hibernate", titlesDesc.get(0));

			List<?> isbnAsc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(by(1, ASCENDING, true))
					.sort(by(2, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
			assertEquals("Hibernate in Action", isbnAsc.get(1));
			assertEquals("Java Persistence with Hibernate", isbnAsc.get(0));

			List<?> isbnDesc = SelectionSpecification.create(Object[].class, "select isbn, title from Book")
					.sort(by(1, DESCENDING, true))
					.sort(by(2, DESCENDING, true))
					.createQuery( session )
					.getResultList()
					.stream().map(book -> book[1])
					.toList();
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
