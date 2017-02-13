package org.hibernate.userguide.pc;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadeDetachTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Test
	public void detachTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );
			phone.setOwner( person );

			entityManager.persist( person );
			entityManager.persist( phone );

			//tag::cascade-detach-test-example[]
			phone = entityManager.find( Phone.class, 1L );
			//end::cascade-detach-test-example[]

			person = phone.getOwner();

			System.out.println( entityManager.contains( phone ));
			System.out.println( entityManager.contains( person ));

			//tag::cascade-detach-test-example[]
			entityManager.detach( phone );
			//end::cascade-detach-test-example[]

			System.out.println( entityManager.contains( phone ));
			System.out.println( entityManager.contains( person ));
		} );
	}

	//tag::cascade-detach-entities-example[]
	@Entity
	public static class Person {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne
		@Cascade( CascadeType.DETACH )
		private Person owner;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	}
	//end::cascade-detach-entities-example[]
}