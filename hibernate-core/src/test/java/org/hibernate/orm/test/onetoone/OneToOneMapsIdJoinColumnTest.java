/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetoone;

import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;


import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneMapsIdJoinColumnTest extends EntityManagerFactoryBasedFunctionalTest {


	@Override
	protected void applySettings(Map<Object, Object> settings) {
		settings.put( AvailableSettings.STATEMENT_INSPECTOR, SQLStatementInspector.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				PersonDetails.class
		};
	}

	@BeforeEach
	public void setUp(){
		doInJPA( this::entityManagerFactory, entityManager -> {

			Person person = new Person( "ABC-123" );

			PersonDetails details = new PersonDetails();
			details.setNickName( "John Doe" );

			person.setDetails( details );
			entityManager.persist( person );

			return person;
		} );
	}

	@Test
	public void testLifecycle() {
		SQLStatementInspector statementInspector = getSqlStatementInspector();

		statementInspector.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, "ABC-123" );
			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

			statementInspector.clear();

			PersonDetails details = entityManager.find( PersonDetails.class, "ABC-123" );
			statementInspector.assertExecutedCount( 0 );

			assertSame(details.getPerson(), person);
			statementInspector.assertExecutedCount( 0 );
		} );
	}

	private SQLStatementInspector getSqlStatementInspector() {
		EntityManagerFactory entityManagerFactory = entityManagerFactory();
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		return (SQLStatementInspector) sessionFactory.getSessionFactoryOptions().getStatementInspector();
	}

	@Entity(name = "Person")
	public static class Person  {

		@Id
		private String id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST, optional = false)
		private PersonDetails details;

		public Person() {}

		public Person(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setDetails(PersonDetails details) {
			this.details = details;
			details.setPerson( this );
		}
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails  {

		@Id
		private String id;

		private String nickName;

		@OneToOne
		@MapsId
		@JoinColumn(name = "person_id")
		private Person person;

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

}
