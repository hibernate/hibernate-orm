package org.hibernate.orm.test.annotations.embeddables;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				GenericEmbeddableWthSubclassTest.PopularBook.class,
				GenericEmbeddableWthSubclassTest.RareBook.class,
		}
)
@SessionFactory
@TestForIssue( jiraKey = "HHH-15560")
public class GenericEmbeddableWthSubclassTest {
	private final static long POPULAR_BOOK_ID = 1l;
	private final static String POPULAR_BOOK_CODE = "POP";
	private final static Integer POPULAR_BOOK_YEAR = 1971;
	private final static long RARE_BOOK_ID = 2l;
	private final static Integer RARE_BOOK_CODE = 123;
	private final static String RARE_BOOK_STATE = "Good";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PopularEdition popularEdition = new PopularEdition(
							"Popular",
							POPULAR_BOOK_CODE,
							POPULAR_BOOK_YEAR
					);
					PopularBook popularBook = new PopularBook( POPULAR_BOOK_ID, popularEdition );

					RareEdition rareEdition = new RareEdition( "Rare", RARE_BOOK_CODE, RARE_BOOK_STATE );
					RareBook rareBook = new RareBook( RARE_BOOK_ID, rareEdition );

					session.persist( popularBook );
					session.persist( rareBook );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from PopularBook" ).executeUpdate();
					session.createQuery( "delete from RareBook" ).executeUpdate();
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-15560", reason = "Parametrized Embedded Subclasses not supported")
	public void testSelectSpecificEmbeddedAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<String> rareBookCodes = session.createQuery(
							"select b.edition.state from RareBook b where b.id = :id",
							String.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookCodes.size() ).isEqualTo( 1 );

					String code = rareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( RARE_BOOK_STATE );
				}
		);

		scope.inTransaction(
				session -> {
					List<Integer> populareBookCodes = session.createQuery(
							"select b.edition.year from PopularBook b where b.id = :id",
							Integer.class
					).setParameter( "id", POPULAR_BOOK_ID ).list();

					assertThat( populareBookCodes.size() ).isEqualTo( 1 );

					Integer code = populareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( POPULAR_BOOK_YEAR );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-15560", reason = "Parametrized Embedded Subclasses not supported")
	public void testDowcasting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<String> rareBookStates = session.createQuery(
							"select re.state from Base b left join treat(b.edition as RareEdition) re where b.id = :id",
							String.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookStates.size() ).isEqualTo( 1 );

					String state = rareBookStates.get( 0 );
					assertThat( state ).isEqualTo( RARE_BOOK_STATE );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> rareBookStates = session.createQuery(
							"select re.state from Base b left join b.edition re where b.id = :id",
							String.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookStates.size() ).isEqualTo( 1 );

					String state = rareBookStates.get( 0 );
					assertThat( state ).isEqualTo( RARE_BOOK_STATE );
				}
		);
	}

	@Test
	public void testSelectBaseEmbeddableAttribute(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Integer> rareBookCodes = session.createQuery(
							"select b.edition.code from RareBook b where b.id = :id",
							Integer.class
					).setParameter( "id", RARE_BOOK_ID ).list();

					assertThat( rareBookCodes.size() ).isEqualTo( 1 );

					Integer code = rareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( RARE_BOOK_CODE );
				}
		);

		scope.inTransaction(
				session -> {
					List<String> populareBookCodes = session.createQuery(
							"select b.edition.code from PopularBook b where b.id = :id",
							String.class
					).setParameter( "id", POPULAR_BOOK_ID ).list();

					assertThat( populareBookCodes.size() ).isEqualTo( 1 );

					String code = populareBookCodes.get( 0 );
					assertThat( code ).isEqualTo( POPULAR_BOOK_CODE );
				}
		);
	}

	@MappedSuperclass
	public static class Edition<T> {
		private String editorName;

		@Column(name = "CODE_COLUMN")
		private T code;

		public Edition() {
		}

		public Edition(String editorName, T code) {
			this.editorName = editorName;
			this.code = code;
		}
	}

	@Embeddable
	public static class RareEdition extends Edition<Integer> {

		private String state;

		public RareEdition() {
		}

		public RareEdition(String editorName, Integer code, String state) {
			super( editorName, code );
			this.state = state;
		}
	}

	@Embeddable
	public static class PopularEdition extends Edition<String> {

		@Column(name = "YEAR_COLUMN")
		private Integer year;

		public PopularEdition() {
		}

		public PopularEdition(String editorName, String code, Integer year) {
			super( editorName, code );
			this.year = year;
		}
	}


	@Entity(name = "Base")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Base {
		@Id
		private Long id;

		public Base() {
		}

		public Base(Long id) {
			this.id = id;
		}
	}


	@MappedSuperclass
	public static abstract class Book<T extends Edition> extends Base {
		@Embedded
		private T edition;

		public Book() {
		}

		public Book(Long id, T edition) {
			super( id );
			this.edition = edition;
		}
	}


	@Entity(name = "PopularBook")
	public static class PopularBook extends Book<PopularEdition> {

		public PopularBook() {
		}

		public PopularBook(Long id, PopularEdition edition) {
			super( id, edition );
		}
	}

	@Entity(name = "RareBook")
	public static class RareBook extends Book<RareEdition> {

		public RareBook() {
		}

		public RareBook(Long id, RareEdition edition) {
			super( id, edition );
		}

	}
}
