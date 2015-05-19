/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.entityNames.singleAssociatedNotAudited;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractOneSessionTest;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Hern&aacute;n Chanfreau
 */
public class ReadEntityAssociatedNotAuditedTest extends AbstractOneSessionTest {

	private long id_car1;
	private long id_car2;

	private long id_pers1;
	private long id_pers2;

	private Car car1;
	private Car car2;
	private Person person1_1;
	private Person person2;
	private Person currentPerson1;
	private Car currentCar1;


	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/entityNames/singleAssociatedNotAudited/mappings.hbm.xml"
		);
		config.addFile( new File( url.toURI() ) );
	}

	@Test
	@Priority(10)
	public void initData() {

		initializeSession();

		Person pers1 = new Person( "Hernan", 15 );
		Person pers2 = new Person( "Leandro", 19 );

		Car car1 = new Car( 1, pers1 );
		Car car2 = new Car( 2, pers2 );

		//REV 1
		getSession().getTransaction().begin();
		getSession().persist( "Personaje", pers1 );
		getSession().persist( car1 );
		getSession().getTransaction().commit();
		id_car1 = car1.getId();
		id_pers1 = pers1.getId();

		//REV 2
		getSession().getTransaction().begin();
		pers1.setAge( 50 );
		getSession().persist( "Personaje", pers1 );
		getSession().persist( "Personaje", pers2 );
		getSession().persist( car2 );
		getSession().getTransaction().commit();
		id_car2 = car2.getId();
		id_pers2 = pers2.getId();

	}

	private void loadDataOnSessionAndAuditReader() {
		currentPerson1 = (Person) getSession().get( "Personaje", id_pers1 );
		person2 = (Person) getSession().get( "Personaje", id_pers2 );

		currentCar1 = (Car) getSession().get( Car.class, id_car1 );

		car1 = getAuditReader().find( Car.class, id_car1, 1 );
		car2 = getAuditReader().find( Car.class, id_car2, 2 );

	}

	private void checkEntityNames() {

		String currentCar1EN = getSession().getEntityName( currentCar1 );

		String car1EN = getAuditReader().getEntityName( id_car1, 1, car1 );

		assert (currentCar1EN.equals( car1EN ));

	}

	private void checkEntities() {

		person1_1 = car1.getOwner();
		Person person2_1 = car2.getOwner();

		assert (currentPerson1.getAge() == person1_1.getAge());
		assert (person2.getAge() == person2_1.getAge());
	}

	@Test
	public void testObtainEntityNameAssociationWithEntityNameAndNotAuditedMode() {
		loadDataOnSessionAndAuditReader();

		checkEntities();

		checkEntityNames();
	}


	@Test
	public void testObtainEntityNameAssociationWithEntityNameAndNotAuditedModeInNewSession() {
		//force a new session and AR
		forceNewSession();

		loadDataOnSessionAndAuditReader();

		checkEntities();

		checkEntityNames();

	}

}
