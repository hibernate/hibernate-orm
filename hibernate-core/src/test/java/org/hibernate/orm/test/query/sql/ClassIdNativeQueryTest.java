package org.hibernate.orm.test.query.sql;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				ClassIdNativeQueryTest.Book.class,
				ClassIdNativeQueryTest.Publisher.class,
		}
)
@SessionFactory
@JiraKey("HHH-17108")
public class ClassIdNativeQueryTest {
	private static final String FILE_ID = "file1";
	private static final String VERSION_ID = "version1";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Publisher publisher = new Publisher( FILE_ID );
					publisher.setVersionid( VERSION_ID );
					publisher.setDescription( "Dodo Books" );
					session.persist( publisher );
					assertEquals( FILE_ID, publisher.getFileId() );
					assertEquals( VERSION_ID, publisher.getVersionid() );

					Book book = new Book( FILE_ID, VERSION_ID );
					book.setTitle( "Birdwatchers Guide to Dodos" );
					book.setDescription( "A complete guide" );
					session.persist( book );
				}
		);
	}

	@Test
	public void testNativeQueryWithPlaceholders(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NativeQuery query = session
							.createNativeQuery(
									"select {book.*}, {publisher.*} from BOOK_T book, PUBLISHER_T publisher where book.fileid = publisher.fileid" ); // and book.versionid = publisher.versionid
					query.addEntity( "book", Book.class );
					query.addEntity( "publisher", Publisher.class );
					List<Object[]> results = query.list();

					assertEquals( 1, results.size() );
					Object[] row = results.get( 0 );
					assertEquals( 2, row.length );
					Book retrievedBook = (Book) row[0];
					Publisher retrievedPublisher = (Publisher) row[1];

					assertEquals( "A complete guide", retrievedBook.getDescription() );
					assertEquals( "Dodo Books", retrievedPublisher.getDescription() );
				}
		);
	}

	@Entity(name = "Book")
	@IdClass(BookPK.class)
	@Table(name = "BOOK_T")
	public static class Book {
		@Id
		private String fileid;

		@Id
		private String versionid;

		@Column(name = "description")
		private String description;

		@Column(name = "title")
		private String title;

		public Book() {
		}

		public Book(final String fileid, final String versionid) {
			this.fileid = fileid;
			this.versionid = versionid;
		}

		public String getFileId() {
			return fileid;
		}

		public void setFileId(final String fileid) {
			this.fileid = fileid;
		}

		public String getVersionid() {
			return versionid;
		}

		public void setVersionid(final String versionid) {
			this.versionid = versionid;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(final String description) {
			this.description = description;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(final String title) {
			this.title = title;
		}

	}

	@Entity(name = "Publisher")
	@Table(name = "PUBLISHER_T")
	public static class Publisher {
		@Id
		private String fileid;

		private String versionid;

		@Column(name = "description")
		private String description;

		public Publisher() {
		}

		public Publisher(final String fileid) {
			this.fileid = fileid;
		}

		public String getFileId() {
			return fileid;
		}

		public void setFileId(final String pk) {
			this.fileid = pk;
		}

		public String getFileid() {
			return fileid;
		}

		public void setFileid(final String fileid) {
			this.fileid = fileid;
		}

		public String getVersionid() {
			return versionid;
		}

		public void setVersionid(final String versionid) {
			this.versionid = versionid;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(final String description) {
			this.description = description;
		}
	}

	public static class BookPK implements Serializable {

		protected String fileid;

		protected String versionid;

		public BookPK() {

		}

		public BookPK(final String fileid) {
			this( fileid, "" );
		}

		public BookPK(final String fileid, final String versionid) {
			this.fileid = fileid;
			this.versionid = versionid;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			BookPK bookPK = (BookPK) o;
			return Objects.equals( fileid, bookPK.fileid ) && Objects.equals( versionid, bookPK.versionid );
		}

		@Override
		public int hashCode() {
			return Objects.hash( fileid, versionid );
		}
	}

}
