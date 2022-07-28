package org.hibernate.test.annotations.notfound;

import java.util.List;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.PropertyProjection;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CriteriaTest extends BaseCoreFunctionalTestCase {

	private static final Long PERSON_ID = 1l;
	private Long ADDRESS_ID = 2l;
	private Long NOT_EXISTING_ADDRESS_ID = 100l;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Address.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Address address = new Address( ADDRESS_ID, "Lollard Street, London" );
					Person person = new Person( PERSON_ID, "andrea", address );

					session.save( address );
					session.save( person );
				}
		);

		inTransaction(
				session ->
						session.createNativeQuery( "update PERSON_TABLE set address_id = " + NOT_EXISTING_ADDRESS_ID ).executeUpdate()
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Address" ).executeUpdate();
				}
		);
	}

	@Test
	public void selectAssociationId() {
		inTransaction(
				session -> {
					Criteria criteria = session.createCriteria( Person.class, "p" );
					ProjectionList projList = Projections.projectionList();
					PropertyProjection property = Projections.property( "address.id" );
					projList.add( property );
					criteria.setProjection( projList );

					List results = criteria.list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void selectAssociationIdWithRestrictions() {
		inTransaction(
				session -> {
					Criteria criteria = session.createCriteria( Person.class, "p" );
					ProjectionList projList = Projections.projectionList();
					PropertyProjection property = Projections.property( "address.id" );
					projList.add( property );
					criteria.setProjection( projList );
					criteria.add( Restrictions.eq( "address.id", NOT_EXISTING_ADDRESS_ID ) );

					List results = criteria.list();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testRestrictionOnAssociationId() {
		inTransaction(
				session -> {
					Criteria criteria = session.createCriteria( Person.class, "p" );
					criteria.add( Restrictions.eq( "address.id", NOT_EXISTING_ADDRESS_ID ) );

					List results = criteria.list();
					assertThat( results.size(), is( 1 ) );
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
		@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
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
