/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.Map;
import jakarta.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.integration.collection.norevision.Name;
import org.hibernate.orm.test.envers.integration.collection.norevision.Person;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-5565")
@SkipForDialect(value = MySQLDialect.class, comment = "The test hangs on")
public class OutsideTransactionTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, Person.class, Name.class};
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, "true" );
	}

	@Test(expected = TransactionRequiredException.class)
	public void testInsertOutsideActiveTransaction() {
		Session session = openSession();

		// Illegal insertion of entity outside of active transaction.
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.flush();

		session.close();
	}

	@Test(expected = TransactionRequiredException.class)
	public void testMergeOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Illegal modification of entity state outside of active transaction.
		entity.setStr( "modified data" );
		session.merge( entity );
		session.flush();

		session.close();
	}

	@Test(expected = TransactionRequiredException.class)
	public void testDeleteOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "data" );
		session.persist( entity );
		session.getTransaction().commit();

		// Illegal removal of entity outside of active transaction.
		session.remove( entity );
		session.flush();

		session.close();
	}

	@Test(expected = TransactionRequiredException.class)
	public void testCollectionUpdateOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		Person person = new Person();
		Name name = new Name();
		name.setName( "Name" );
		person.getNames().add( name );
		session.persist( person );
		session.getTransaction().commit();

		// Illegal collection update outside of active transaction.
		person.getNames().remove( name );
		session.merge( person );
		session.flush();

		session.close();
	}

	@Test(expected = TransactionRequiredException.class)
	public void testCollectionRemovalOutsideActiveTransaction() {
		Session session = openSession();

		// Revision 1
		session.getTransaction().begin();
		Person person = new Person();
		Name name = new Name();
		name.setName( "Name" );
		person.getNames().add( name );
		session.persist( person );
		session.getTransaction().commit();

		// Illegal collection removal outside of active transaction.
		person.setNames( null );
		session.merge( person );
		session.flush();

		session.close();
	}
}
