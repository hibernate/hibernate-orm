/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Car;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Hern&aacute;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class HasChangedAuditedManyToManyTest extends AbstractModifiedFlagsOneSessionTest {

	private long id_car1;

	private long id_pers1;
	private long id_pers2;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/manytomany/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					Person pers1 = new Person( "Hernan", 28 );
					Person pers2 = new Person( "Leandro", 29 );
					Person pers3 = new Person( "Barba", 32 );
					Person pers4 = new Person( "Camomo", 15 );

					//REV 1
					session.getTransaction().begin();
					List<Person> owners = new ArrayList<>();
					owners.add( pers1 );
					owners.add( pers2 );
					owners.add( pers3 );
					Car car1 = new Car( 5, owners );

					session.persist( car1 );
					session.getTransaction().commit();
					id_pers1 = pers1.getId();
					id_car1 = car1.getId();
					id_pers2 = pers2.getId();

					owners = new ArrayList<>();
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
				}
		);
	}

	@DynamicTest
	public void testHasChangedPerson1() {
		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "personaje", false, false )
								.add( AuditEntity.id().eq( id_pers1 ) )
								.add( AuditEntity.property( "cars" ).hasChanged() )
								.getResultList()
				),
				contains( 1 )
		);

		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
								.add( AuditEntity.id().eq( id_pers1 ) )
								.add( AuditEntity.property( "cars" ).hasNotChanged() )
								.getResultList()
				),
				contains( 2 )
		);
	}

	@DynamicTest
	public void testHasChangedPerson2() {
		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
								.add( AuditEntity.id().eq( id_pers2 ) )
								.add( AuditEntity.property( "cars" ).hasChanged() )
								.getResultList()
				),
				contains( 1, 2 )
		);

		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
								.add( AuditEntity.id().eq( id_pers2 ) )
								.add( AuditEntity.property( "cars" ).hasNotChanged() )
								.getResultList()
				),
				CollectionMatchers.isEmpty()
		);
	}

	@DynamicTest
	public void testHasChangedCar1() {
		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Car.class, false, false )
								.add( AuditEntity.id().eq( id_car1 ) )
								.add( AuditEntity.property( "owners" ).hasChanged() )
								.getResultList()
				),
				contains( 1 )
		);

		assertThat(
				extractRevisions(
						getAuditReader().createQuery().forRevisionsOfEntity( Car.class, false, false )
								.add( AuditEntity.id().eq( id_car1 ) )
								.add( AuditEntity.property( "owners" ).hasNotChanged() )
								.getResultList()
				),
				CollectionMatchers.isEmpty()
		);
	}
}
