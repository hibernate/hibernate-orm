/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entitynames.audited.manytomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.test.EnversSingleSessionBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Car;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class ReadEntityWithAuditedManyToManyTest extends EnversSingleSessionBasedFunctionalTest {

	private long id_car1;
	private long id_car2;

	private long id_pers1;

	private Person person1;
	private Car car1;

	private Person person1_1;
	private Car car1_2;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/manytomany/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final Person pers1 = new Person( "Hernan", 28 );
		final Person pers2 = new Person( "Leandro", 29 );
		final Person pers3 = new Person( "Barba", 32 );
		final Person pers4 = new Person( "Camomo", 15 );

		inSession(
				session -> {
					try {
						// Revision 1
						session.getTransaction().begin();

						List<Person> owners = new ArrayList<>();
						owners.add( pers1 );
						owners.add( pers2 );
						owners.add( pers3 );

						final Car car1 = new Car( 5, owners );
						session.persist( car1 );
						session.getTransaction().commit();

						this.id_pers1 = pers1.getId();
						this.id_car1 = car1.getId();

						// Revision 2
						session.getTransaction().begin();
						owners = new ArrayList<>();
						owners.add( pers2 );
						owners.add( pers3 );
						owners.add( pers4 );

						final Car car2 = new Car( 27, owners );
						final Person person1 = (Person) session.get( "Personaje", id_pers1 );
						person1.setName( "Hernan David" );
						person1.setAge( 40 );
						session.persist( car1 );
						session.persist( car2 );
						session.getTransaction().commit();

						this.id_car2 = car2.getId();
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
	public void testGetEntityNameManyYoManyWithEntityName() {
		loadDataOnSessionAndAuditReader();
		assertEntityNames();
	}


	@DynamicTest
	public void testGetEntityNameManyYoManyWithEntityNameInNewSession() {
		//force new session and AR
		forceNewSession();
		loadDataOnSessionAndAuditReader();
		assertEntityNames();
	}

	private void loadDataOnSessionAndAuditReader() {
		car1_2 = getAuditReader().find( Car.class, id_car1, 2 );
		Car car2_2 = getAuditReader().find( Car.class, id_car2, 2 );

		// navigate through relations to load objects
		for ( Person owner : car1_2.getOwners() ) {
			for ( Car ownedCar : owner.getCars() ) {
				ownedCar.getRegistrationNumber();
			}
		}

		for ( Person owner : car2_2.getOwners() ) {
			for ( Car ownedCar : owner.getCars() ) {
				ownedCar.getRegistrationNumber();
			}
		}

		car1 = inSession( session -> (Car) session.get( Car.class, id_car1 ) );
		person1 = inSession( session -> (Person) session.get( "Personaje", id_pers1 ) );
		person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
	}

	private void assertEntityNames() {
		final String personName = inSession( session -> { return session.getEntityName( person1 ); } );
		final String carName = inSession( session -> { return session.getEntityName( car1 ); } );

		assertThat( getAuditReader().getEntityName( id_pers1, 1, person1_1 ), equalTo( personName ) );
		assertThat( getAuditReader().getEntityName( id_car1, 2, car1_2 ), equalTo( carName ) );
	}
}
