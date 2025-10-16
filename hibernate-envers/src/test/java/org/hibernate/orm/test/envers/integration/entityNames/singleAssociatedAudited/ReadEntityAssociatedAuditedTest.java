/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.entityNames.singleAssociatedAudited;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Hern&aacute;n Chanfreau
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(xmlMappings = "mappings/entityNames/singleAssociatedAudited/mappings.hbm.xml")
@SessionFactory
@EnversTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadEntityAssociatedAuditedTest {

	private long id_car1;
	private long id_car2;

	private Car currentCar1;
	private Car car1;

	private long id_pers1;
	private long id_pers2;

	private Person currentPerson1;
	private Person person1;


	@Test
	@Order(1)
	public void testGetAssociationWithEntityName(SessionFactoryScope scope) {
		Person pers1 = new Person( "Hernan", 15 );
		Person pers2 = new Person( "Leandro", 19 );

		Car car1 = new Car( 1, pers1, null );
		Car car2 = new Car( 2, pers2, null );

		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
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

			// first test with the same session and AR
			loadDataOnSessionAndAuditReader( session, auditReader );
			checkEntities( session, auditReader );
			checkEntityNames( session, auditReader );
		} );
	}

	private void loadDataOnSessionAndAuditReader(SessionImplementor session, AuditReader auditReader) {
		currentCar1 = (Car) session.get( Car.class, id_car1 );
		currentPerson1 = (Person) session.get( "Personaje", id_pers1 );
		car1 = auditReader.find( Car.class, id_car1, 1 );
		person1 = car1.getOwner();
	}

	private void checkEntities(SessionImplementor session, AuditReader auditReader) {
		assertNotEquals( currentPerson1.getAge(), person1.getAge() );

		Person person2 = (Person) session.get( "Personaje", id_pers2 );
		Car car2 = auditReader.find( Car.class, id_car2, 2 );
		Person person2_1 = car2.getOwner();
		assertEquals( person2.getAge(), person2_1.getAge() );
	}

	private void checkEntityNames(SessionImplementor session, AuditReader auditReader) {
		String currentCar1EN = session.getEntityName( currentCar1 );

		String currentPerson1EN = session.getEntityName( currentPerson1 );

		String car1EN = auditReader.getEntityName( id_car1, 1, car1 );
		assertEquals( currentCar1EN, car1EN );

		String person1EN = auditReader.getEntityName( id_pers1, 1, person1 );
		assertEquals( currentPerson1EN, person1EN );
	}

	@Test
	public void testGetAssociationWithEntityNameInNewSession(SessionFactoryScope scope) {
		scope.inSession( session -> {
			//force a new session and AR
			final var auditReader = AuditReaderFactory.get( session );
			loadDataOnSessionAndAuditReader( session, auditReader );
			checkEntities( session, auditReader );
			checkEntityNames( session, auditReader );
		} );
	}
}
