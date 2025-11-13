/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.integration.entityNames.manyToManyAudited.Car;
import org.hibernate.orm.test.envers.integration.entityNames.manyToManyAudited.Person;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Hern&aacute;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(xmlMappings = "mappings/entityNames/manyToManyAudited/mappings.hbm.xml")
@SessionFactory
@ServiceRegistry(
		settings = @Setting(
				name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG,
				value = "true"
		)
)
@EnversTest
public class HasChangedAuditedManyToManyTest {

	private long id_car1;

	private long id_pers1;
	private long id_pers2;


	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person pers1 = new Person( "Hernan", 28 );
					Person pers2 = new Person( "Leandro", 29 );
					Person pers3 = new Person( "Barba", 32 );
					Person pers4 = new Person( "Camomo", 15 );

					//REV 1
					List<Person> owners = new ArrayList<Person>();
					owners.add( pers1 );
					owners.add( pers2 );
					owners.add( pers3 );
					Car car1 = new Car( 5, owners );

					session.persist( car1 );
					session.getTransaction().commit();
					id_pers1 = pers1.getId();
					id_car1 = car1.getId();
					id_pers2 = pers2.getId();

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
				}
		);
	}

	@Test
	public void testHasChangedPerson1(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Person.class, "Personaje", false, false )
							.add( AuditEntity.id().eq( id_pers1 ) )
							.add( AuditEntity.property( "cars" ).hasChanged() )
							.getResultList();
					assertThat( list ).hasSize( 1 );
					assertThat( extractRevisionNumbers( list ) ).isEqualTo( makeList( 1 ) );

					list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Person.class, "Personaje", false, false )
							.add( AuditEntity.id().eq( id_pers1 ) )
							.add( AuditEntity.property( "cars" ).hasNotChanged() )
							.getResultList();
					assertThat( list ).hasSize( 1 );
					assertThat( extractRevisionNumbers( list ) ).isEqualTo( makeList( 2 ) );
				}
		);

	}

	@Test
	public void testHasChangedPerson2(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Person.class, "Personaje", false, false )
							.add( AuditEntity.id().eq( id_pers2 ) )
							.add( AuditEntity.property( "cars" ).hasChanged() )
							.getResultList();
					assertThat( list ).hasSize( 2 );
					assertThat( extractRevisionNumbers( list ) )
							.isEqualTo( makeList( 1, 2 ) );

					list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Person.class, "Personaje", false, false )
							.add( AuditEntity.id().eq( id_pers2 ) )
							.add( AuditEntity.property( "cars" ).hasNotChanged() )
							.getResultList();
					assertThat( list ).hasSize( 0 );
				}
		);
	}

	@Test
	public void testHasChangedCar1(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Car.class, false, false )
							.add( AuditEntity.id().eq( id_car1 ) )
							.add( AuditEntity.property( "owners" ).hasChanged() )
							.getResultList();
					assertThat( list ).hasSize( 1 );
					assertThat( extractRevisionNumbers( list ) ).isEqualTo( makeList( 1 ) );

					list = AuditReaderFactory.get( session ).createQuery()
							.forRevisionsOfEntity( Car.class, false, false )
							.add( AuditEntity.id().eq( id_car1 ) )
							.add( AuditEntity.property( "owners" ).hasNotChanged() )
							.getResultList();
					assertThat( list ).hasSize( 0 );
				}
		);

	}
}
