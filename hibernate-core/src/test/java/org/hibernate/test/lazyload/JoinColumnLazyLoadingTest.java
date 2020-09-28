package org.hibernate.test.lazyload;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Yann Briançon
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14233" )
public class JoinColumnLazyLoadingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Car.class,
				Company.class
		};
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			Company company1 = new Company();
			company1.siren = "siren1";

			Company company2 = new Company();
			company2.siren = "siren2";

			session.save( company1 );
			session.save( company2 );

			Car car1 = new Car();
			car1.company = company1;

			Car car2 = new Car();
			car2.company = company2;

			session.save( car1 );
			session.save( car2 );
		} );
	}

	@Test
	public void testLazyJoinColumnLazilyLoaded() {
		inTransaction( session -> {
			List<Car> cars = session.createQuery( "from Car", Car.class ).getResultList();
			cars.forEach( car -> assertFalse( Hibernate.isInitialized( car.company ) ) );
		} );
	}

	@Entity(name = "Car")
	static class Car implements Serializable {
		@Id @GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "siren", referencedColumnName = "siren")
		Company company;
	}

	@Entity(name = "Company")
	static class Company implements Serializable {
		@Id @GeneratedValue
		Long id;

		@Column(unique = true)
		String siren;

		@OneToMany(mappedBy = "company")
		Set<Car> cars = new HashSet<>();
	}
}
