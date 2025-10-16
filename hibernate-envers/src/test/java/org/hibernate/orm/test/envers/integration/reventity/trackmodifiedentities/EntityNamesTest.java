/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import org.hibernate.Session;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.tools.Pair;
import org.hibernate.orm.test.envers.integration.entityNames.manyToManyAudited.Car;
import org.hibernate.orm.test.envers.integration.entityNames.manyToManyAudited.Person;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@Jpa(xmlMappings = "mappings/entityNames/manyToManyAudited/mappings.hbm.xml",
		integrationSettings = @Setting(name = EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, value = "true"))
public class EntityNamesTest {
	private Long person1Id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		Person pers1 = new Person( "Hernan", 28 );
		Person pers2 = new Person( "Leandro", 29 );
		Person pers3 = new Person( "Barba", 32 );
		Person pers4 = new Person( "Camomo", 15 );

		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();
			List<Person> owners = new ArrayList<Person>();
			owners.add( pers1 );
			owners.add( pers2 );
			owners.add( pers3 );
			Car car1 = new Car( 5, owners );
			em.persist( car1 );
			em.getTransaction().commit();
			long person1Id = pers1.getId();

			// Revision 2
			owners = new ArrayList<Person>();
			owners.add( pers2 );
			owners.add( pers3 );
			owners.add( pers4 );
			Car car2 = new Car( 27, owners );
			em.getTransaction().begin();
			Person person1 = (Person) em.unwrap( Session.class ).get( "Personaje", person1Id );
			person1.setName( "Hernan David" );
			person1.setAge( 40 );
			em.persist( car1 );
			em.persist( car2 );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testModifiedEntityTypes(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( TestTools.makeSet(
					Pair.make( Car.class.getName(), Car.class ),
					Pair.make( "Personaje", Person.class )
			), AuditReaderFactory.get( em ).getCrossTypeRevisionChangesReader().findEntityTypes( 1 ) );

			assertEquals( TestTools.makeSet(
					Pair.make( Car.class.getName(), Car.class ),
					Pair.make( "Personaje", Person.class )
			), AuditReaderFactory.get( em ).getCrossTypeRevisionChangesReader().findEntityTypes( 2 ) );
		} );
	}
}
