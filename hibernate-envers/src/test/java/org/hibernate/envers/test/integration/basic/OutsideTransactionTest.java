package org.hibernate.envers.test.integration.basic;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.integration.collection.norevision.Name;
import org.hibernate.envers.test.integration.collection.norevision.Person;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

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
	protected void configure(Configuration configuration) {
		configuration.setProperty( EnversSettings.STORE_DATA_AT_DELETE, "true" );
		configuration.setProperty( EnversSettings.REVISION_ON_COLLECTION_CHANGE, "true" );
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
