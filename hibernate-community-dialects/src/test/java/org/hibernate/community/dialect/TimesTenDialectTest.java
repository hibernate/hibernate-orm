package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests pagination on TimesTen, which uses {@code select first n ...} and {@code select rows m to n ...},
 * where {@code m} and {@code n} are 1-based and inclusive and the clause must precede {@code distinct}.
 * <p>
 * Run against a TimesTen database with {@code -Pdb=timesten}.
 */
@RequiresDialect(TimesTenDialect.class)
@DomainModel(annotatedClasses = TimesTenDialectTest.Book.class)
@SessionFactory
public class TimesTenDialectTest {

	@BeforeEach
	void createBooks(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 12; i++ ) {
				session.persist( new Book( i, String.format( "title%02d", i ), i % 3 ) );
			}
		} );
	}

	@AfterEach
	void dropBooks(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlFirstPage(SessionFactoryScope scope) {
		assertThat( hqlIds( scope, 0, 5 ) ).containsExactly( 1, 2, 3, 4, 5 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlLaterPages(SessionFactoryScope scope) {
		assertThat( hqlIds( scope, 5, 5 ) ).containsExactly( 6, 7, 8, 9, 10 );
		assertThat( hqlIds( scope, 10, 5 ) ).containsExactly( 11, 12 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlMaxResultsOnly(SessionFactoryScope scope) {
		assertThat( hqlIds( scope, null, 5 ) ).containsExactly( 1, 2, 3, 4, 5 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21179")
	void hqlOffsetOnly(SessionFactoryScope scope) {
		assertThat( hqlIds( scope, 10, null ) ).containsExactly( 11, 12 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeFirstPage(SessionFactoryScope scope) {
		assertThat( nativeIds( scope, "select id from Book order by id", 0, 5 ) ).containsExactly( 1, 2, 3, 4, 5 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeLaterPages(SessionFactoryScope scope) {
		assertThat( nativeIds( scope, "select id from Book order by id", 5, 5 ) ).containsExactly( 6, 7, 8, 9, 10 );
		assertThat( nativeIds( scope, "select id from Book order by id", 10, 5 ) ).containsExactly( 11, 12 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeMaxResultsOnly(SessionFactoryScope scope) {
		assertThat( nativeIds( scope, "select id from Book order by id", null, 5 ) ).containsExactly( 1, 2, 3, 4, 5 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21180")
	void nativeOffsetOnly(SessionFactoryScope scope) {
		assertThat( nativeIds( scope, "select id from Book order by id", 10, null ) ).containsExactly( 11, 12 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21184")
	void hqlDistinct(SessionFactoryScope scope) {
		final List<Integer> groups = scope.fromSession( session -> session
				.createSelectionQuery( "select distinct b.grp from Book b order by b.grp", Integer.class )
				.setFirstResult( 1 )
				.setMaxResults( 2 )
				.getResultList() );
		assertThat( groups ).containsExactly( 1, 2 );

		final List<String> titles = scope.fromSession( session -> session
				.createSelectionQuery( "select distinct b.title from Book b order by b.title", String.class )
				.setFirstResult( 5 )
				.setMaxResults( 5 )
				.getResultList() );
		assertThat( titles ).containsExactly( "title06", "title07", "title08", "title09", "title10" );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21184")
	void hqlDistinctMaxResultsOnly(SessionFactoryScope scope) {
		final List<Integer> groups = scope.fromSession( session -> session
				.createSelectionQuery( "select distinct b.grp from Book b order by b.grp", Integer.class )
				.setMaxResults( 2 )
				.getResultList() );
		assertThat( groups ).containsExactly( 0, 1 );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21184")
	void nativeDistinct(SessionFactoryScope scope) {
		assertThat( nativeIds( scope, "select distinct grp from Book order by grp", 1, 2 ) ).containsExactly( 1, 2 );
	}

	private static List<Integer> hqlIds(SessionFactoryScope scope, Integer firstResult, Integer maxResults) {
		return scope.fromSession( session -> {
			final var query = session.createSelectionQuery( "select b.id from Book b order by b.id", Integer.class );
			if ( firstResult != null ) {
				query.setFirstResult( firstResult );
			}
			if ( maxResults != null ) {
				query.setMaxResults( maxResults );
			}
			return query.getResultList();
		} );
	}

	private static List<Integer> nativeIds(
			SessionFactoryScope scope,
			String sql,
			Integer firstResult,
			Integer maxResults) {
		return scope.fromSession( session -> {
			final var query = session.createNativeQuery( sql, Integer.class );
			if ( firstResult != null ) {
				query.setFirstResult( firstResult );
			}
			if ( maxResults != null ) {
				query.setMaxResults( maxResults );
			}
			return query.getResultList();
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Integer id;
		@Basic
		private String title;
		@Basic
		private Integer grp;

		public Book() {
		}

		public Book(Integer id, String title, Integer grp) {
			this.id = id;
			this.title = title;
			this.grp = grp;
		}
	}
}
