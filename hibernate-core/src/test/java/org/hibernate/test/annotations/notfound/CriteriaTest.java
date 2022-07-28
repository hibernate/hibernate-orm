package org.hibernate.test.annotations.notfound;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.PropertyProjection;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

public class CriteriaTest extends BaseCoreFunctionalTestCase {

	private Long personId = 1l;
	private Long addressId = 2l;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Address address = new Address( addressId, "Lollard Street, London" );
					Person person = new Person( personId, "andrea", address );

					session.save( address );
					session.save( person );
				}
		);
	}

	@Test
	public void selectId() {
		inTransaction(
				session -> {
					Criteria criteria = session.createCriteria( Person.class, "p" );
					ProjectionList projList = Projections.projectionList();
					PropertyProjection property = Projections.property( "address.id" );
					projList.add( property );
					criteria.setProjection( projList );
					criteria.list();
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		Long id;

		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		public Address address;

		public Person() {
		}

		public Person(Long id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}
	}

	@Entity(name="Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {
		@Id
		private Long id;

		String address;

		public Address() {
		}

		public Address(Long id, String address) {
			this.id = id;
			this.address = address;
		}
	}
}
