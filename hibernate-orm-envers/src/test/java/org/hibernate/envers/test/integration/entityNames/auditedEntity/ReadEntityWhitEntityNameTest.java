/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.entityNames.auditedEntity;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractOneSessionTest;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Hern&aacute;n Chanfreau
 */
public class ReadEntityWhitEntityNameTest extends AbstractOneSessionTest {

	private long id_pers1;
	private long id_pers2;
	private long id_pers3;

	private Person person1_1;
	private Person person1_2;
	private Person person1_3;

	private Person currentPers1;

	protected void initMappings() throws MappingException, URISyntaxException {
		URL url = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/entityNames/auditedEntity/mappings.hbm.xml"
		);
		config.addFile( new File( url.toURI() ) );
	}


	@Test
	@Priority(10)
	public void initData() {

		initializeSession();

		Person pers1 = new Person( "Hernan", 28 );
		Person pers2 = new Person( "Leandro", 29 );
		Person pers3 = new Person( "Barba", 30 );

		//REV 1
		getSession().getTransaction().begin();
		getSession().persist( "Personaje", pers1 );
		id_pers1 = pers1.getId();
		getSession().getTransaction().commit();

		//REV 2
		getSession().getTransaction().begin();
		pers1 = (Person) getSession().get( "Personaje", id_pers1 );
		pers1.setAge( 29 );
		getSession().persist( "Personaje", pers1 );
		getSession().persist( "Personaje", pers2 );
		id_pers2 = pers2.getId();
		getSession().getTransaction().commit();

		//REV
		getSession().getTransaction().begin();
		pers1 = (Person) getSession().get( "Personaje", id_pers1 );
		pers1.setName( "Hernan David" );
		pers2 = (Person) getSession().get( "Personaje", id_pers2 );
		pers2.setAge( 30 );
		getSession().persist( "Personaje", pers1 );
		getSession().persist( "Personaje", pers2 );
		getSession().persist( "Personaje", pers3 );
		id_pers3 = pers3.getId();
		getSession().getTransaction().commit();

		getSession().getTransaction().begin();
		currentPers1 = (Person) getSession().get( "Personaje", id_pers1 );
		getSession().getTransaction().commit();

	}


	@Test
	public void testRetrieveRevisionsWithEntityName() {
		List<Number> pers1Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers1 );
		List<Number> pers2Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers2 );
		List<Number> pers3Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers3 );

		assert (pers1Revs.size() == 3);
		assert (pers2Revs.size() == 2);
		assert (pers3Revs.size() == 1);
	}

	@Test
	public void testRetrieveAuditedEntityWithEntityName() {
		person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
		person1_2 = getAuditReader().find( Person.class, "Personaje", id_pers1, 2 );
		person1_3 = getAuditReader().find( Person.class, "Personaje", id_pers1, 3 );

		assert (person1_1 != null);
		assert (person1_2 != null);
		assert (person1_3 != null);

	}

	@Test
	public void testObtainEntityNameAuditedEntityWithEntityName() {
		person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
		person1_2 = getAuditReader().find( Person.class, "Personaje", id_pers1, 2 );
		person1_3 = getAuditReader().find( Person.class, "Personaje", id_pers1, 3 );

		String currentPers1EN = getSession().getEntityName( currentPers1 );

		String person1EN = getAuditReader().getEntityName( person1_1.getId(), 1, person1_1 );
		assert (currentPers1EN.equals( person1EN ));

		String person2EN = getAuditReader().getEntityName( person1_2.getId(), 2, person1_2 );
		assert (currentPers1EN.equals( person2EN ));

		String person3EN = getAuditReader().getEntityName( person1_3.getId(), 3, person1_3 );
		assert (currentPers1EN.equals( person3EN ));

	}

	@Test
	public void testRetrieveAuditedEntityWithEntityNameWithNewSession() {

		// force a new session and AR
		forceNewSession();

		person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
		person1_2 = getAuditReader().find( Person.class, "Personaje", id_pers1, 2 );
		person1_3 = getAuditReader().find( Person.class, "Personaje", id_pers1, 3 );

		assert (person1_1 != null);
		assert (person1_2 != null);
		assert (person1_3 != null);
	}


}
