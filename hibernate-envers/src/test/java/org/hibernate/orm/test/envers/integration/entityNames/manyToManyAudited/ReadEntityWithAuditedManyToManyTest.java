/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.entityNames.manyToManyAudited;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Hern&aacute;n Chanfreau
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(xmlMappings = "mappings/entityNames/manyToManyAudited/mappings.hbm.xml")
@SessionFactory
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadEntityWithAuditedManyToManyTest {

	private long id_car1;
	private long id_car2;

	private long id_pers1;

	private Person person1;
	private Car car1;

	private Person person1_1;
	private Car car1_2;

	@Test
	public void testGetEntityNameManyYoManyWithEntityName(SessionFactoryScope scope) {
		scope.inSession( session -> {
			Person pers1 = new Person( "Hernan", 28 );
			Person pers2 = new Person( "Leandro", 29 );
			Person pers3 = new Person( "Barba", 32 );
			Person pers4 = new Person( "Camomo", 15 );

			//REV 1
			session.getTransaction().begin();
			List<Person> owners = new ArrayList<Person>();
			owners.add( pers1 );
			owners.add( pers2 );
			owners.add( pers3 );
			Car car1 = new Car( 5, owners );

			session.persist( car1 );
			session.getTransaction().commit();
			id_pers1 = pers1.getId();
			id_car1 = car1.getId();

			owners = new ArrayList<Person>();
			owners.add( pers2 );
			owners.add( pers3 );
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

			final var auditReader = AuditReaderFactory.get( session );
			loadDataOnSessionAndAuditReader( session, auditReader );
			checkEntityNames( session, auditReader );
		} );
	}

	private void loadDataOnSessionAndAuditReader(SessionImplementor session, AuditReader auditReader) {
		car1_2 = auditReader.find( Car.class, id_car1, 2 );
		Car car2_2 = auditReader.find( Car.class, id_car2, 2 );

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

		car1 = (Car) session.get( Car.class, id_car1 );
		person1 = (Person) session.get( "Personaje", id_pers1 );
		person1_1 = auditReader.find( Person.class, "Personaje", id_pers1, 1 );
	}

	private void checkEntityNames(SessionImplementor session, AuditReader auditReader) {
		String currPerson1EN = session.getEntityName( person1 );
		String currCar1EN = session.getEntityName( car1 );

		String person1_1EN = auditReader.getEntityName( id_pers1, 1, person1_1 );
		assert (currPerson1EN.equals( person1_1EN ));

		String car1_2EN = auditReader.getEntityName( id_car1, 2, car1_2 );
		assert (currCar1EN.equals( car1_2EN ));
	}

	@Test
	public void testGetEntityNameManyYoManyWithEntityNameInNewSession(SessionFactoryScope scope) {
		scope.inSession( session -> {
			//force new session and AR
			final var auditReader = AuditReaderFactory.get( session );
			loadDataOnSessionAndAuditReader( session, auditReader );
			checkEntityNames( session, auditReader );
		} );
	}
}
