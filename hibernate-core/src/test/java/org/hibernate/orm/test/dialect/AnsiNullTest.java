/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.orm.test.length.WithLongStrings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Jan Schatteman
 */
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-16216" )
public class AnsiNullTest {

	private int lob1;
	private int lob2;

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					session.persist( new Book( 1L, "LoremIpsum" ) );
					session.persist( new Book( 2L, null ) );
				}
		);
		lob1 = scope.fromTransaction( session -> {
					WithLongStrings wls = new WithLongStrings();
					wls.longish = "Short String";
					wls.long32 = "Some long String".repeat( 100 );
					session.persist( wls );
					return wls.id;
				}
		);
		lob2 = scope.fromTransaction( session -> {
					WithLongStrings wls = new WithLongStrings();
					wls.longish = "Short String";
					session.persist( wls );
					return wls.id;
				}
		);
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	@DomainModel(annotatedClasses = { AnsiNullTest.Book.class, WithLongStrings.class })
	@SessionFactory
	@ServiceRegistry( settings = {@Setting(name = DriverManagerConnectionProvider.INIT_SQL, value = "set ansinull on")} )
	public void testWithAnsiNullOn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null OR b.id > 1", Book.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null OR b.id >= 1", Book.class ).list().size() );

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null OR b.id < 2", Book.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null OR b.id <= 2", Book.class ).list().size() );

					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title = null", Book.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title = null AND b.id <= 2", Book.class ).list().size());
					b = session.createQuery( "SELECT b FROM Book b WHERE b.title = null OR b.id > 1", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null", Book.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null AND b.id < 2", Book.class ).list().size());
					b = session.createQuery( "SELECT b FROM Book b WHERE b.title != null OR b.id < 2", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title != null or id = 2", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);
					b = session.createQuery( "SELECT b FROM Book b WHERE id = 2 or b.title != null", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE id = 1 and b.title != null", Book.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null and id = 1", Book.class ).list().size() );

					List<Book> books = session.createQuery( "SELECT b FROM Book b WHERE 1 = 1", Book.class ).list();
					Assertions.assertEquals( 2, books.size());
				}
		);
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	@DomainModel(annotatedClasses = { AnsiNullTest.Book.class, WithLongStrings.class })
	@SessionFactory
	@ServiceRegistry( settings = {@Setting(name = DriverManagerConnectionProvider.INIT_SQL, value = "set ansinull on")} )
	public void testLOBWithAnsiNullOn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					WithLongStrings w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob1, w.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null OR w.id > 1", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null OR w.id >= 1", WithLongStrings.class ).list().size() );

					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null OR w.id < 2", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null OR w.id <= 2", WithLongStrings.class ).list().size() );

					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null", WithLongStrings.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null AND w.id <= " + lob2, WithLongStrings.class ).list().size());
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null OR w.id > 1", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);

					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null", WithLongStrings.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null AND w.id < " + lob2, WithLongStrings.class ).list().size());
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null OR w.id > 1", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);

					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null or id = " + lob2, WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( 2L, w.id);
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE id = " + lob2 + " or w.long32 != null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( 2L, w.id);
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE id = " + lob1 + " and w.long32 != null", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null and id = " + lob1, WithLongStrings.class ).list().size() );

					List<WithLongStrings> ws = session.createQuery( "SELECT w FROM WithLongStrings w WHERE 1 = 1", WithLongStrings.class ).list();
					Assertions.assertEquals( 2, ws.size());
				}
		);
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	@DomainModel(annotatedClasses = { AnsiNullTest.Book.class, WithLongStrings.class })
	@SessionFactory
	@ServiceRegistry( settings = {@Setting(name = DriverManagerConnectionProvider.INIT_SQL, value = "set ansinull off")} )
	public void testWithAnsiNullOff(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book b = session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null OR b.id > 1", Book.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS DISTINCT FROM null OR b.id >= 1", Book.class ).list().size() );

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null OR b.id < 2", Book.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT b FROM Book b WHERE b.title IS NOT DISTINCT FROM null OR b.id <= 2", Book.class ).list().size() );

					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title = null", Book.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title = null AND b.id <= 2", Book.class ).list().size());
					b = session.createQuery( "SELECT b FROM Book b WHERE b.title = null OR b.id > 1", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);

					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null", Book.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null AND b.id < 2", Book.class ).list().size());
					b = session.createQuery( "SELECT b FROM Book b WHERE b.title != null OR b.id < 2", Book.class ).getSingleResult();
					Assertions.assertEquals( 1L, b.id);

					b = session.createQuery( "SELECT b FROM Book b WHERE b.title != null or id = 2", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);
					b = session.createQuery( "SELECT b FROM Book b WHERE id = 2 or b.title != null", Book.class ).getSingleResult();
					Assertions.assertEquals( 2L, b.id);
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE id = 1 and b.title != null", Book.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT b FROM Book b WHERE b.title != null and id = 1", Book.class ).list().size() );

					List<Book> books = session.createQuery( "SELECT b FROM Book b WHERE 1 = 1", Book.class ).list();
					Assertions.assertEquals( 2, books.size());
				}
		);
	}

	@Test
	@RequiresDialect(value = SybaseASEDialect.class)
	@DomainModel(annotatedClasses = { AnsiNullTest.Book.class, WithLongStrings.class })
	@SessionFactory
	@ServiceRegistry( settings = {@Setting(name = DriverManagerConnectionProvider.INIT_SQL, value = "set ansinull off")} )
	public void testLOBWithAnsiNullOff(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					WithLongStrings w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob1, w.id);

					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null OR w.id > 1", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS DISTINCT FROM null OR w.id >= 1", WithLongStrings.class ).list().size() );

					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);
					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null OR w.id < 2", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 2, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 IS NOT DISTINCT FROM null OR w.id <= 2", WithLongStrings.class ).list().size() );

					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null", WithLongStrings.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null AND w.id <= " + lob2, WithLongStrings.class ).list().size());
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 = null OR w.id > 1", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);

					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null", WithLongStrings.class ).list().size());
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null AND w.id < " + lob2, WithLongStrings.class ).list().size());
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null OR w.id > 1", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( lob2, w.id);

					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null or id = " + lob2, WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( 2L, w.id);
					w = session.createQuery( "SELECT w FROM WithLongStrings w WHERE id = " + lob2 + " or w.long32 != null", WithLongStrings.class ).getSingleResult();
					Assertions.assertEquals( 2L, w.id);
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE id = " + lob1 + " and w.long32 != null", WithLongStrings.class ).list().size() );
					Assertions.assertEquals( 0, session.createQuery( "SELECT w FROM WithLongStrings w WHERE w.long32 != null and id = " + lob1, WithLongStrings.class ).list().size() );

					List<WithLongStrings> ws = session.createQuery( "SELECT w FROM WithLongStrings w WHERE 1 = 1", WithLongStrings.class ).list();
					Assertions.assertEquals( 2, ws.size());
				}
		);
	}

	@Entity(name = "Book")
	static class Book {
		@Id
		Long id;
		String title;

		public Book() {
		}

		public Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}

}
