/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.entityNames.oneToManyAudited;

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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hern&aacute;n Chanfreau
 */
@RequiresDialect(H2Dialect.class)
@EnversTest
@DomainModel(xmlMappings = "mappings/entityNames/oneToManyAudited/mappings.hbm.xml")
@SessionFactory
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadEntityWithAuditedCollectionTest {

	private long id_car1;
	private long id_car2;

	private Car currentCar1;
	private Person currentPerson1;

	private long id_pers1;

	private Car car1_1;
	private Person person1_1;

	@Test
	@Order(1)
	public void initData(SessionFactoryScope scope) {
		scope.inSession( session -> {
			Person pers1 = new Person( "Hernan", 28 );
			Person pers2 = new Person( "Leandro", 29 );
			Person pers4 = new Person( "Camomo", 15 );

			List<Person> owners = new ArrayList<Person>();
			owners.add( pers1 );
			owners.add( pers2 );
			Car car1 = new Car( 5, owners );

			//REV 1
			session.getTransaction().begin();
			session.persist( car1 );
			session.getTransaction().commit();
			id_pers1 = pers1.getId();
			id_car1 = car1.getId();

			owners = new ArrayList<Person>();
			owners.add( pers2 );
			owners.add( pers4 );
			Car car2 = new Car( 27, owners );
			//REV 2
			session.getTransaction().begin();
			Person person1 = (Person) session.get( "Personaje", id_pers1 );
			person1.setName( "Hernan David" );
			person1.setAge( 40 );
			session.persist( car1 );
			session.persist( car2 );
			session.getTransaction().commit();
			id_car2 = car2.getId();

			final var auditReader = loadDataOnSessionAndAuditReader( session );
			checkEntityNames( session, auditReader);
		} );
	}

	private AuditReader loadDataOnSessionAndAuditReader(SessionImplementor session) {
		currentCar1 = (Car) session.get( Car.class, id_car1 );
		currentPerson1 = (Person) session.get( "Personaje", id_pers1 );

		final var auditReader = AuditReaderFactory.get( session );
		person1_1 = auditReader.find( Person.class, "Personaje", id_pers1, 1 );
		car1_1 = auditReader.find( Car.class, id_car1, 2 );
		Car car2 = auditReader.find( Car.class, id_car2, 2 );

		for ( Person owner : car1_1.getOwners() ) {
			owner.getName();
			owner.getAge();
		}
		for ( Person owner : car2.getOwners() ) {
			owner.getName();
			owner.getAge();
		}

		return auditReader;
	}

	private void checkEntityNames(SessionImplementor session, AuditReader auditReader) {
		String currCar1EN = session.getEntityName( currentCar1 );
		String currPerson1EN = session.getEntityName( currentPerson1 );

		String car1_1EN = auditReader.getEntityName( id_car1, 2, car1_1 );
		assert (currCar1EN.equals( car1_1EN ));

		String person1_1EN = auditReader.getEntityName( id_pers1, 1, person1_1 );
		assert (currPerson1EN.equals( person1_1EN ));
	}

	@Test
	public void testObtainEntityNameAuditedCollectionWithEntityNameInNewSession(SessionFactoryScope scope) {
		// force a new session and AR
		scope.inSession( session -> {
			final var auditReader = loadDataOnSessionAndAuditReader( session );
			checkEntityNames( session, auditReader );
		} );
	}
}
