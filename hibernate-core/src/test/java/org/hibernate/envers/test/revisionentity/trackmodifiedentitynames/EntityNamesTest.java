/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Car;
import org.hibernate.envers.test.support.domains.entitynames.audited.manytomany.Person;
import org.hibernate.envers.tools.Pair;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class EntityNamesTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/manytomany/mappings.hbm.xml" };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inSession(
				session -> {
					final Person pers1 = new Person( "Hernan", 28 );
					final Person pers2 = new Person( "Leandro", 29 );
					final Person pers3 = new Person( "Barba", 32 );
					final Person pers4 = new Person( "Camomo", 15 );

					// Revision 1
					session.getTransaction().begin();
					List<Person> owners1 = new ArrayList<>();
					owners1.add( pers1 );
					owners1.add( pers2 );
					owners1.add( pers3 );

					final Car car1 = new Car( 5, owners1 );
					session.persist( car1 );
					session.getTransaction().commit();

					// Revision 2
					session.getTransaction().begin();
					List<Person> owners2 = new ArrayList<>();
					owners2.add( pers2 );
					owners2.add( pers3 );
					owners2.add( pers4 );

					final Car car2 = new Car( 27, owners2 );
					final Person person1 = (Person) session.get( "Personaje", pers1.getId() );
					person1.setName( "Hernan David" );
					person1.setAge( 40 );
					session.persist( car1 );
					session.persist( car2 );
					session.getTransaction().commit();
				}
		);
	}

	@DynamicTest
	public void testModifiedEntityTypes() {
		assertThat(
				getAuditReader().getCrossTypeRevisionChangesReader().findEntityTypes( 1 ),
				containsInAnyOrder(
						Pair.make( Car.class.getName(), Car.class ),
						Pair.make( "Personaje", Person.class )
				)
		);

		assertThat(
				getAuditReader().getCrossTypeRevisionChangesReader().findEntityTypes( 2 ),
				containsInAnyOrder(
						Pair.make( Car.class.getName(), Car.class ),
						Pair.make( "Personaje", Person.class )
				)
		);
	}
}