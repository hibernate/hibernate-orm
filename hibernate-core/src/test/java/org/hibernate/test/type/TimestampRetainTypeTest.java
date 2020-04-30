package org.hibernate.test.type;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class TimestampRetainTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				UserAccount.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			UserAccount user = new UserAccount()
					.setId( 1L )
					.setFirstName( "Vlad" )
					.setLastName( "Mihalcea" )
					.setSubscribedOn(
							parseTimestamp("2013-09-29 12:30:00")
					);

			entityManager.persist( user );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			UserAccount userAccount = entityManager.find(
					UserAccount.class, 1L
			);

			assertEquals(
					parseTimestamp("2013-09-29 12:30:00"),
					userAccount.getSubscribedOn()
			);

			assertEquals( Date.class, userAccount.getSubscribedOn().getClass() );
		} );
	}

	private final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");

	private java.util.Date parseTimestamp(String timestamp) {
		try {
			return DATE_TIME_FORMAT.parse(timestamp);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Entity(name = "UserAccount")
	@Table(name = "user_account")
	public static class UserAccount {

		@Id
		private Long id;

		@Column(name = "first_name", length = 50)
		private String firstName;

		@Column(name = "last_name", length = 50)
		private String lastName;

		@Column(name = "subscribed_on")
		@Temporal(TemporalType.TIMESTAMP)
		private Date subscribedOn;

		public Long getId() {
			return id;
		}

		public UserAccount setId(Long id) {
			this.id = id;
			return this;
		}

		public String getFirstName() {
			return firstName;
		}

		public UserAccount setFirstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public String getLastName() {
			return lastName;
		}

		public UserAccount setLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public Date getSubscribedOn() {
			return subscribedOn;
		}

		public UserAccount setSubscribedOn(Date subscribedOn) {
			this.subscribedOn = subscribedOn;
			return this;
		}
	}
}
