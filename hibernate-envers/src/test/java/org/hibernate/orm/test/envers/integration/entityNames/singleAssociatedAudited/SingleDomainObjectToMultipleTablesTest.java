/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.entityNames.singleAssociatedAudited;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(xmlMappings = "mappings/entityNames/singleAssociatedAudited/mappings.hbm.xml")
@SessionFactory
@EnversTest
public class SingleDomainObjectToMultipleTablesTest {
	private long carId;
	private long ownerId;
	private long driverId;

	@Test
	public void testSingleDomainObjectToMultipleTablesMapping(SessionFactoryScope scope) {
		scope.inSession( session -> {
			//REV 1
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

			final var auditReader = AuditReaderFactory.get( session );
			doAssertions( auditReader );
		} );
	}

	public void doAssertions(AuditReader auditReader) {
		Car carVer1 = auditReader.find( Car.class, carId, 1 );
		Person ownerVer1 = auditReader.find( Person.class, "Personaje", ownerId, 1 );
		Person driverVer1 = auditReader.find( Person.class, "Driveraje", driverId, 1 );

		/* Check ids. */
		assertEquals( ownerVer1.getId(), carVer1.getOwner().getId() );
		assertEquals( driverVer1.getId(), carVer1.getDriver().getId() );

		/* Check object properties. */
		assertEquals( "Lukasz", ownerVer1.getName() );
		assertEquals( "Kinga", driverVer1.getName() );
		assertEquals( 1, carVer1.getNumber() );
	}
}
