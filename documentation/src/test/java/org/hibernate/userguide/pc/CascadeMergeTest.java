package org.hibernate.userguide.pc;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Fábio Takeo Ueno
 */
public class CascadeMergeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Phone.class
		};
	}

	@Test
	public void mergeTest() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setName( "John Doe" );

			entityManager.persist( person );

			//tag::cascade-merge-test-example[]
			person = entityManager.find( Person.class, 1L );

			Phone phone = new Phone();
			phone.setId( 1L );
			phone.setNumber( "123-456-7890" );
			phone.setOwner( person );

			person.getPhones().add( phone );

			entityManager.merge( person );
			//end::cascade-merge-test-example[]
		} );
	}

	//tag::cascade-merge-entities-example[]
	@Entity
	public static class Person {

		@Id
		private Long id;

		private String name;

		@Cascade( CascadeType.MERGE )
		@OneToMany(mappedBy = "owner")
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

		public void setPhones(List<Phone> phones) {
			this.phones = phones;
		}
	}

	@Entity
	public static class Phone {

		@Id
		private Long id;

		@Column(name = "`number`")
		private String number;

		@ManyToOne
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
	//end::cascade-merge-entities-example[]
}