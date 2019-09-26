/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entitynames.audited.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.test.EnversSingleSessionBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.entitynames.audited.onetomany.Car;
import org.hibernate.envers.test.support.domains.entitynames.audited.onetomany.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class ReadEntityWithAuditedCollectionTest extends EnversSingleSessionBasedFunctionalTest {

	private long id_car1;
	private long id_car2;

	private Car currentCar1;
	private Person currentPerson1;

	private long id_pers1;

	private Car car1_1;
	private Person person1_1;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/onetomany/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					try {
						Person pers1 = new Person( "Hernan", 28 );
						Person pers2 = new Person( "Leandro", 29 );
						Person pers4 = new Person( "Camomo", 15 );

						List<Person> owners = new ArrayList<>();
						owners.add( pers1 );
						owners.add( pers2 );
						Car car1 = new Car( 5, owners );

						// Revision 1
						session.getTransaction().begin();
						session.persist( car1 );
						session.getTransaction().commit();
						id_pers1 = pers1.getId();
						id_car1 = car1.getId();

						owners = new ArrayList<>();
						owners.add( pers2 );
						owners.add( pers4 );
						Car car2 = new Car( 27, owners );

						// Revision 2
						session.getTransaction().begin();
						Person person1 = (Person) session.get( "Personaje", id_pers1 );
						person1.setName( "Hernan David" );
						person1.setAge( 40 );
						session.persist( car1 );
						session.persist( car2 );
						session.getTransaction().commit();

						id_car2 = car2.getId();
					}
					catch ( Exception e ) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@DynamicTest
	public void testObtainEntityNameAuditedCollectionWithEntityName() {
		loadDataOnSessionAndAuditReader();
		assertEntityNames();
	}

	@DynamicTest
	public void testObtainEntityNameAuditedCollectionWithEntityNameInNewSession() {
		// force a new session and AR
		forceNewSession();

		loadDataOnSessionAndAuditReader();
		assertEntityNames();
	}

	private void loadDataOnSessionAndAuditReader() {
		currentCar1 = inSession( session -> (Car) session.get( Car.class, id_car1 ) );
		currentPerson1 = inSession( session -> (Person) session.get( "Personaje", id_pers1 ) );

		person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
		car1_1 = getAuditReader().find( Car.class, id_car1, 2 );
		Car car2 = getAuditReader().find( Car.class, id_car2, 2 );

		for ( Person owner : car1_1.getOwners() ) {
			owner.getName();
			owner.getAge();
		}

		for ( Person owner : car2.getOwners() ) {
			owner.getName();
			owner.getAge();
		}
	}

	private void assertEntityNames() {
		final String carName = inSession( session -> { return session.getEntityName( currentCar1 ); } );
		final String personName = inSession( session -> { return session.getEntityName( currentPerson1 ); } );

		assertThat( getAuditReader().getEntityName( id_car1, 2, car1_1 ), equalTo( carName ) );
		assertThat( getAuditReader().getEntityName( id_pers1, 1, person1_1 ), equalTo( personName ) );
	}
}

