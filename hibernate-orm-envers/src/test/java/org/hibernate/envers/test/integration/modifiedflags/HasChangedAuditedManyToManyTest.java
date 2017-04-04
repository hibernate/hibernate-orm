/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Car;
import org.hibernate.envers.test.integration.entityNames.manyToManyAudited.Person;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.hibernate.envers.test.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.envers.test.tools.TestTools.makeList;

/**
 * @author Hern&aacute;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedAuditedManyToManyTest extends AbstractModifiedFlagsOneSessionTest {

	private long id_car1;

	private long id_pers1;
	private long id_pers2;

	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/entityNames/manyToManyAudited/mappings.hbm.xml"
		);
		config.addFile( new File( url.toURI() ) );
	}

	@Test
	@Priority(10)
	public void initData() {

		initializeSession();

		Person pers1 = new Person( "Hernan", 28 );
		Person pers2 = new Person( "Leandro", 29 );
		Person pers3 = new Person( "Barba", 32 );
		Person pers4 = new Person( "Camomo", 15 );

		//REV 1
		getSession().getTransaction().begin();
		List<Person> owners = new ArrayList<Person>();
		owners.add( pers1 );
		owners.add( pers2 );
		owners.add( pers3 );
		Car car1 = new Car( 5, owners );

		getSession().persist( car1 );
		getSession().getTransaction().commit();
		id_pers1 = pers1.getId();
		id_car1 = car1.getId();
		id_pers2 = pers2.getId();

		owners = new ArrayList<Person>();
		owners.add( pers2 );
		owners.add( pers3 );
		owners.add( pers4 );
		Car car2 = new Car( 27, owners );
		//REV 2
		getSession().getTransaction().begin();
		Person person1 = (Person) getSession().get( "Personaje", id_pers1 );
		person1.setName( "Hernan David" );
		person1.setAge( 40 );
		getSession().persist( car1 );
		getSession().persist( car2 );
		getSession().getTransaction().commit();
	}

	@Test
	public void testHasChangedPerson1() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
				.add( AuditEntity.id().eq( id_pers1 ) )
				.add( AuditEntity.property( "cars" ).hasChanged() )
				.getResultList();
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
				.add( AuditEntity.id().eq( id_pers1 ) )
				.add( AuditEntity.property( "cars" ).hasNotChanged() )
				.getResultList();
		assertEquals( 1, list.size() );
		assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );
	}

	@Test
	public void testHasChangedPerson2() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
				.add( AuditEntity.id().eq( id_pers2 ) )
				.add( AuditEntity.property( "cars" ).hasChanged() )
				.getResultList();
		assertEquals( 2, list.size() );
		assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

		list = getAuditReader().createQuery().forRevisionsOfEntity( Person.class, "Personaje", false, false )
				.add( AuditEntity.id().eq( id_pers2 ) )
				.add( AuditEntity.property( "cars" ).hasNotChanged() )
				.getResultList();
		assertEquals( 0, list.size() );
	}

	@Test
	public void testHasChangedCar1() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( Car.class, false, false )
				.add( AuditEntity.id().eq( id_car1 ) )
				.add( AuditEntity.property( "owners" ).hasChanged() )
				.getResultList();
		assertEquals( 1, list.size() );
		assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

		list = getAuditReader().createQuery().forRevisionsOfEntity( Car.class, false, false )
				.add( AuditEntity.id().eq( id_car1 ) )
				.add( AuditEntity.property( "owners" ).hasNotChanged() )
				.getResultList();
		assertEquals( 0, list.size() );
	}
}
