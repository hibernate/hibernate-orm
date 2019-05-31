/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entitynames.audited.manytoone;

import org.hibernate.envers.test.EnversSingleSessionBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytoone.Car;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytoone.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class ReadEntityAssociatedAuditedTest extends EnversSingleSessionBasedFunctionalTest {

	private long id_car1;
	private long id_car2;

	private Car currentCar1;
	private Car car1;

	private long id_pers1;
	private long id_pers2;

	private Person currentPerson1;
	private Person person1;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/manytoone/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					try {
						Person pers1 = new Person( "Hernan", 15 );
						Person pers2 = new Person( "Leandro", 19 );

						Car car1 = new Car( 1, pers1, null );
						Car car2 = new Car( 2, pers2, null );

						//REV 1
						session.getTransaction().begin();
						session.persist( "Personaje", pers1 );
						session.persist( car1 );
						session.getTransaction().commit();
						id_car1 = car1.getId();
						id_pers1 = pers1.getId();

						//REV 2
						session.getTransaction().begin();
						pers1.setAge( 50 );
						session.persist( "Personaje", pers1 );
						session.persist( "Personaje", pers2 );
						session.persist( car2 );
						session.getTransaction().commit();
						id_car2 = car2.getId();
						id_pers2 = pers2.getId();
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
	public void testGetAssociationWithEntityName() {
		loadDataOnSessionAndAuditReader();
		assertEntityState();
		assertEntityNames();
	}

	@DynamicTest
	public void testGetAssociationWithEntityNameInNewSession() {
		//force a new session and AR
		forceNewSession();

		loadDataOnSessionAndAuditReader();
		assertEntityState();
		assertEntityNames();
	}

	private void loadDataOnSessionAndAuditReader() {
		currentCar1 = inSession( session -> (Car) session.get( Car.class, id_car1) );
		currentPerson1 = inSession( session -> (Person) session.get( "Personaje", id_pers1 ) );

		car1 = getAuditReader().find( Car.class, id_car1, 1 );
		person1 = car1.getOwner();
	}

	private void assertEntityState() {
		assertThat( person1.getAge(), equalTo( currentPerson1.getAge() ) );

		final Person person2 = inSession( session -> (Person) session.get( "Personaje", id_pers2 ) );
		assertThat( getAuditReader().find( Car.class, id_car2, 2).getOwner().getAge(), equalTo( person2.getAge() ) );
	}

	private void assertEntityNames() {
		final String carName = inSession( session -> { return session.getEntityName( currentCar1 ); } );
		final String personName = inSession( session -> { return session.getEntityName( currentPerson1 ); } );

		assertThat( getAuditReader().getEntityName( id_car1, 1, car1 ), equalTo( carName ) );
		assertThat( getAuditReader().getEntityName( id_pers1, 1, person1 ), equalTo( personName ) );
	}
}
