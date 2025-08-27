/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.annotations.NaturalId;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Jan-Willem Gmelig Meyling
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				NaturalIdDereferenceTest.Book.class,
				NaturalIdDereferenceTest.BookRef.class,
				NaturalIdDereferenceTest.BookRefRef.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class NaturalIdDereferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book book = new Book();
					book.isbn = "abcd";
					session.persist( book );

					BookRef bookRef = new BookRef();
					bookRef.naturalBook = bookRef.normalBook = book;
					session.persist( bookRef );

					session.flush();
					session.clear();
				}
		);
	}

	@AfterEach
	public void deleteData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void naturalIdDereferenceTest(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r.normalBook.isbn FROM BookRef r" );
					List resultList = query.getResultList();
					assertFalse( resultList.isEmpty() );
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void normalIdDereferenceFromAlias(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r.normalBook.id FROM BookRef r" );
					List resultList = query.getResultList();
					assertFalse( resultList.isEmpty() );
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 0, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void naturalIdDereferenceFromAlias(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r.naturalBook.isbn FROM BookRef r" );
					List resultList = query.getResultList();
					assertFalse( resultList.isEmpty() );
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 0, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void normalIdDereferenceFromImplicitJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.normalBookRef.normalBook.id FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void naturalIdDereferenceFromImplicitJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.normalBookRef.naturalBook.isbn FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * Due to the construction of the mapping for {@link BookRefRef#naturalBookRef}, the {@code isbn} column maps
	 * to both the referenced {@link BookRef} and {@link Book}. As such, {@code r2.naturalBookRef.naturalBook.isbn}
	 * can be dereferenced without a single join.
	 */
	@Test
	public void nestedNaturalIdDereferenceFromImplicitJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.naturalBookRef.naturalBook.isbn FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 0, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * Adjustment of {@link #nestedNaturalIdDereferenceFromImplicitJoin(SessionFactoryScope)}, that instead selects the {@code id} property,
	 * which requires a single join to {@code Book}.
	 */
	@Test
	public void nestedNaturalIdDereferenceFromImplicitJoin2(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.naturalBookRef.naturalBook.id FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void doNotDereferenceNaturalIdIfIsReferenceToPrimaryKey(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.normalBookRef.normalBook.isbn FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 2, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	public void selectedEntityIsNotDereferencedForPrimaryKey(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.normalBookRef.normalBook FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 2, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * BookRefRef can be joined directly with Book due to the construction of the isbn key.
	 * <p>
	 * I.e.
	 * <p>
	 * BookRefRef{isbn=abcd} enforces BookRef{isbn=abc} (FK) enforces BookRef{isbn=abc} (FK),
	 * so bookRefRef.naturalBookRef.naturalBook = Book{isbn=abc}.
	 * <p>
	 * BookRefRef{isbn=null}, i.e. no BookRef for this BookRefRef, and as such no book,
	 * so bookRefRef.naturalBookRef.naturalBook yields null which is expected.
	 */
	@Test
	public void selectedEntityIsNotDereferencedForNaturalId(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT r2.naturalBookRef.naturalBook FROM BookRefRef r2" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * {@code r2.normalBookRef.normalBook.id} requires 1 join as seen in {@link #normalIdDereferenceFromImplicitJoin}.
	 * {@code r3.naturalBookRef.naturalBook.isbn} requires 1 join as seen in {@link #selectedEntityIsNotDereferencedForNaturalId(SessionFactoryScope)} .
	 * An additional join is added to join BookRef once more on {@code r2.normalBookRef.normalBook.isbn = r3.naturalBookRef.naturalBook.isbn}.
	 * This results in three joins in total.
	 */
	@Test
	public void dereferenceNaturalIdInJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT r2.normalBookRef.normalBook.id, r3.naturalBookRef.naturalBook.isbn " +
									"FROM BookRefRef r2 JOIN BookRefRef r3 ON r2.normalBookRef.normalBook.isbn = r3.naturalBookRef.naturalBook.isbn" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					// r2.normalBookRef.normalBook.id requires
					assertEquals( 3, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * {@code BookRefRef} is joined with {@code BookRef} on {@code b.naturalBook.isbn = a.naturalBookRef.naturalBook.isbn}.
	 * {@code b.naturalBook.isbn} can be dereferenced without any join ({@link #naturalIdDereferenceFromAlias(SessionFactoryScope)} .
	 * {@code a.naturalBookRef.naturalBook.isbn} can be dereferenced without any join ({@link #nestedNaturalIdDereferenceFromImplicitJoin(SessionFactoryScope)} .
	 * We finally select all properties of {@code b.normalBook}, which requires {@code Book} to be joined.
	 * This results in two joins in total.
	 */
	@Test
	public void dereferenceNaturalIdInJoin2(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT b.normalBook FROM BookRefRef a " +
															"JOIN BookRef b ON b.naturalBook.isbn = a.naturalBookRef.naturalBook.isbn" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 2, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * The {@link BookRef#normalBook} is joined with {@code BookRef} on {@code join.isbn = from.normalBook.isbn}.
	 * {@code join.isbn = from.normalBook.isbn} both dereference to {@code join.isbn}.
	 * {@code r.normalBook.isbn} dereferences to {@code join.isbn}.
	 * As a result, only a single join is required.
	 */
	@Test
	public void dereferenceNaturalIdInJoin3(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT r.normalBook.isbn FROM BookRef r JOIN r.normalBook b ON b.isbn = r.normalBook.isbn" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 1, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	/**
	 * The {@link Book} is joined with {@link BookRef} on {@code book.isbn = ref.normalBook.isbn}.
	 * {@code book.isbn} can be dereferenced from the {@code Book} table.
	 * {@code ref.normalBook.isbn} requires an implicit join with book.
	 * {@code ref.normalBook.isbn} in the final selection is available due to the aforementioned join.
	 * As a result, 2 joins are required.
	 */
	@Test
	public void dereferenceNaturalIdInJoin4(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"SELECT r.normalBook.isbn FROM BookRef r JOIN Book b ON b.isbn = r.normalBook.isbn" );
					query.getResultList();
					sqlStatementInterceptor.assertExecutedCount( 1 );
					assertEquals( 2, sqlStatementInterceptor.getNumberOfJoins( 0 ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13752")
	public void deleteWithNaturalIdBasedJoinTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"DELETE FROM Book b WHERE 1=0" );
					query.executeUpdate();
				}
		);
	}

	@Entity(name = "Book")
	@Table(name = "book")
	public static class Book {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@NaturalId
		@Column(name = "isbn", unique = true)
		private String isbn;

		@OneToMany
		@JoinTable(
			name = "similar_books",
			joinColumns = @JoinColumn(name = "base_isbn", referencedColumnName = "isbn"),
			inverseJoinColumns = @JoinColumn(name = "ref_isbn", referencedColumnName = "isbn_ref")
		)
		private Set<BookRef> similarBooks;

	}

	@Entity(name = "BookRef")
	@Table(name = "bookref")
	public static class BookRef {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ManyToOne(optional = true)
		@JoinColumn(nullable = true, name = "id_ref")
		private Book normalBook;

		@ManyToOne
		@JoinColumn(name = "isbn_ref", referencedColumnName = "isbn")
		private Book naturalBook;

	}

	@Entity(name = "BookRefRef")
	@Table(name = "bookrefref")
	public static class BookRefRef {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@ManyToOne
		@JoinColumn(nullable = true, name = "id_ref_ref")
		private BookRef normalBookRef;

		@ManyToOne
		@JoinColumn(name = "isbn_ref_Ref", referencedColumnName = "isbn_ref")
		private BookRef naturalBookRef;
	}

}
