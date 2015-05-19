/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.basic;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.integration.collection.norevision.Name;
import org.hibernate.envers.test.integration.collection.norevision.Person;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-5565")
public class OutsideTransactionTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, Person.class, Name.class};
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, "true" );
	}

	@Test(expected = AuditException.class)
	public void testInsertOutsideActiveTransaction() {
		Session session = openSession();

		// Illegal insertion of entity outside of active transaction.
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.flush();

		session.close();
	}

	@Test(expected = AuditException.class)
	public void testUpdateOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Illegal modification of entity state outside of active transaction.
		entity.setStr( "modified data" );
		session.update( entity );
		session.flush();

		session.close();
	}

	@Test(expected = AuditException.class)
	public void testDeleteOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Illegal removal of entity outside of active transaction.
		session.delete( entity );
		session.flush();

		session.close();
	}

	@Test(expected = AuditException.class)
	public void testCollectionUpdateOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		Person person = new Person();
		Name name = new Name();
		name.setName( "Name" );
		person.getNames().add( name );
		session.saveOrUpdate( person );
		session.getTransaction().commit();

		// Illegal collection update outside of active transaction.
		person.getNames().remove( name );
		session.saveOrUpdate( person );
		session.flush();

		session.close();
	}

	@Test(expected = AuditException.class)
	public void testCollectionRemovalOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		Person person = new Person();
		Name name = new Name();
		name.setName( "Name" );
		person.getNames().add( name );
		session.saveOrUpdate( person );
		session.getTransaction().commit();

		// Illegal collection removal outside of active transaction.
		person.setNames( null );
		session.saveOrUpdate( person );
		session.flush();

		session.close();
	}
}
