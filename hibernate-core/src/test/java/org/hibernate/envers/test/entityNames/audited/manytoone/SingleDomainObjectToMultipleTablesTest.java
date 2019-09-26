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

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class SingleDomainObjectToMultipleTablesTest extends EnversSingleSessionBasedFunctionalTest {
	private long carId = 0;
	private long ownerId = 0;
	private long driverId = 0;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/manytoone/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					try {
						session.getTransaction().begin();
						Person owner = new Person( "Lukasz", 25 );
						Person driver = new Person( "Kinga", 24 );
						Car car = new Car( 1, owner, driver );
						session.persist( "Personaje", owner );
						session.persist( "Driveraje", driver );
						session.persist( car );
						session.getTransaction().commit();

						carId = car.getId();
						ownerId = owner.getId();
						driverId = driver.getId();
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
	@TestForIssue(jiraKey = "HHH-4648")
	public void testSingleDomainObjectToMultipleTablesMapping() {
		Car carVer1 = getAuditReader().find( Car.class, carId, 1 );
		Person ownerVer1 = getAuditReader().find( Person.class, "Personaje", ownerId, 1 );
		Person driverVer1 = getAuditReader().find( Person.class, "Driveraje", driverId, 1 );

        /* Check ids. */
		assertThat( carVer1.getOwner().getId(), equalTo( ownerVer1.getId() ) );
		assertThat( carVer1.getDriver().getId(), equalTo( driverVer1.getId() ) );

        /* Check object properties. */
		assertThat( ownerVer1.getName(), equalTo( "Lukasz" ) );
		assertThat( driverVer1.getName(), equalTo( "Kinga" ) );
		assertThat( carVer1.getNumber(), equalTo( 1 ) );
	}
}
