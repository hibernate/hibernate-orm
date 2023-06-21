package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				IdClassWithOffsetDateTimeTest.MyEntity.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2")
})
@JiraKey("HHH-16791")
public class IdClassWithOffsetDateTimeTest {

	private static final Long ID = 1l;
	private static final Long ID_2 = 2l;
	private static final OffsetDateTime BOOK_DATE = OffsetDateTime.now();
	private static final OffsetDateTime BOOK_DATE_2 = OffsetDateTime.now().plus( Period.ofWeeks( 1 ) );

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( ID, BOOK_DATE, "one" );
					MyEntity myEntity2 = new MyEntity( ID_2, BOOK_DATE_2, "two" );
					session.persist( myEntity );
					session.persist( myEntity2 );
				}
		);
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.getReference( MyEntity.class, new MyEntityKey( ID_2, BOOK_DATE_2 )  );
					MyEntity myEntity = session.find( MyEntity.class, new MyEntityKey( ID, BOOK_DATE ) );
					assertThat( myEntity ).isNotNull();
				}
		);
	}

	@Test
	public void testFind2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, new MyEntityKey( ID, BOOK_DATE ) );
					assertThat( myEntity ).isNotNull();
				}
		);
	}

	@IdClass(MyEntityKey.class)
	@Entity(name = "MyEntity")
	public static class MyEntity {

		@Id
		private Long id;

		@Id
		@Column
		private OffsetDateTime bookDate;

		private String data;

		public MyEntity() {
		}

		public MyEntity(Long id, OffsetDateTime bookDate, String data) {
			this.id = id;
			this.bookDate = bookDate;
			this.data = data;
		}
	}

	public static class MyEntityKey implements Serializable {

		private Long id;

		private OffsetDateTime bookDate;

		public MyEntityKey() {
		}

		public MyEntityKey(Long id, OffsetDateTime bookDate) {
			this.id = id;
			this.bookDate = bookDate;
		}
	}
}
