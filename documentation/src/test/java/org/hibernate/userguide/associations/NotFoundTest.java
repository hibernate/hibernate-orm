package org.hibernate.userguide.associations;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Fábio Ueno
 */
public class NotFoundTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::associations-not-found-example[]
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );

			person.addPhone( phone );

			entityManager.persist( person );
			//end::associations-not-found-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );

			entityManager.createNativeQuery( "ALTER TABLE Person DROP FOREIGN KEY FK_OWNER" ).executeUpdate();

			entityManager.remove( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Phone phone = entityManager.find( Phone.class, 1L );

			Person owner = phone.getOwner();

			assertEquals( null, owner );
		} );
	}

	//tag::associations-not-found-domain-model-example[]
	@Entity
	@Table( name = "Person" )
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany( mappedBy = "owner", cascade = CascadeType.PERSIST )
		private List<Phone> phones = new ArrayList<>();

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

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			this.phones.add( phone );
			phone.setOwner( this );
		}
	}

	@Entity
	@Table( name = "Phone" )
	public static class Phone {

		@Id
		private Long id;

		private String number;

		@ManyToOne( fetch = FetchType.LAZY )
		@NotFound ( action = NotFoundAction.IGNORE )
		@JoinColumn( foreignKey = @ForeignKey( name = "FK_OWNER" ) )
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
	//end::associations-not-found-domain-model-example[]
}