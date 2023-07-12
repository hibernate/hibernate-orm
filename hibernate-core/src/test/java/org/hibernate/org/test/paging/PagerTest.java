package org.hibernate.org.test.paging;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.query.Page;
import org.hibernate.query.Pager;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = PagerTest.Book.class)
public class PagerTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(s -> {
			for ( int i = 1; i<100; i ++ ) {
				Book b = new Book();
				b.title = "Some Title";
				s.persist(b);
			}
		});
		scope.inSession(s -> {
			AtomicInteger integer = new AtomicInteger(0);
			Pager<Book> pager = s.createSelectionQuery("from Book", Book.class)
					.getResultPager(Page.first(15));
			pager.forEachRemainingPage(books -> {
				assertEquals( integer.incrementAndGet()-1, pager.getCurrentPage().getNumber() );
				assertEquals( pager.getCurrentPage().getNumber() < 6 ? 15 : 9, books.size() );
			});
			assertEquals( 7, integer.get() );
		});
		scope.inSession(s -> {
			AtomicInteger integer = new AtomicInteger(0);
			Pager<Book> pager = s.createSelectionQuery("from Book", Book.class)
					.getResultPager(Page.first(15));
			pager.stream().forEach(books -> {
				assertEquals( integer.incrementAndGet()-1, pager.getCurrentPage().getNumber() );
				assertEquals( pager.getCurrentPage().getNumber() < 6 ? 15 : 9, books.size() );
			});
			assertEquals( 7, integer.get() );
		});
		scope.inSession(s -> {
			Pager<Book> pager = s.createSelectionQuery("from Book", Book.class)
					.getResultPager(Page.first(15));
			int count = 0;
			while ( pager.hasResults() ) {
				assertEquals( count++, pager.getCurrentPage().getNumber() );
				List<Book> books = pager.getResultList();
				assertEquals( pager.getCurrentPage().getNumber() < 6 ? 15 : 9, books.size() );
				pager.next();
			}
			assertEquals( 7, count );
		});
		scope.inSession(s -> {
			Page page = Page.first(10);
			boolean hasMore;
			do {
				Pager<Book> pager = s.createSelectionQuery("from Book", Book.class)
						.getResultPager(page);
				List<Book> books = pager.getResultList();
				hasMore = pager.hasResultsOnNextPage();
				page = page.next();

			} while ( hasMore );
		});
	}

	@Entity(name = "Book")
	static class Book {
		@Id @GeneratedValue(strategy = GenerationType.UUID)
		String isbn;
		String title;
	}
}
