/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Car;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Person;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.envers.tools.Pair;

import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EntityNamesTest extends BaseEnversFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/entityNames/manyToManyAudited/mappings.hbm.xml"};
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "true" );
	}

	@Test
	@Priority(10)
	public void initData() {
		Person pers1 = new Person( "Hernan", 28 );
		Person pers2 = new Person( "Leandro", 29 );
		Person pers3 = new Person( "Barba", 32 );
		Person pers4 = new Person( "Camomo", 15 );

		// Revision 1
		getSession().getTransaction().begin();
		List<Person> owners = new ArrayList<Person>();
		owners.add( pers1 );
		owners.add( pers2 );
		owners.add( pers3 );
		Car car1 = new Car( 5, owners );
		getSession().persist( car1 );
		getSession().getTransaction().commit();
		long person1Id = pers1.getId();

		// Revision 2
		owners = new ArrayList<Person>();
		owners.add( pers2 );
		owners.add( pers3 );
		owners.add( pers4 );
		Car car2 = new Car( 27, owners );
		getSession().getTransaction().begin();
		Person person1 = (Person) getSession().get( "Personaje", person1Id );
		person1.setName( "Hernan David" );
		person1.setAge( 40 );
		getSession().persist( car1 );
		getSession().persist( car2 );
		getSession().getTransaction().commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testModifiedEntityTypes() {
		assert TestTools.makeSet(
				Pair.make( Car.class.getName(), Car.class ),
				Pair.make( "Personaje", Person.class )
		)
				.equals( getAuditReader().getCrossTypeRevisionChangesReader().findEntityTypes( 1 ) );
		assert TestTools.makeSet(
				Pair.make( Car.class.getName(), Car.class ),
				Pair.make( "Personaje", Person.class )
		)
				.equals( getAuditReader().getCrossTypeRevisionChangesReader().findEntityTypes( 2 ) );
	}
}