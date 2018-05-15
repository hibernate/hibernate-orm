/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Map;

import javax.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.Name;
import org.hibernate.envers.test.support.domains.basic.Person;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SkipForDialect;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-5565")
@SkipForDialect(dialectClass = MySQL5Dialect.class, matchSubTypes = true, reason = "Test hangs on this platform")
public class OutsideTransactionTest extends EnversSessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class, Person.class, Name.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, Boolean.TRUE );
		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, Boolean.TRUE );
	}

	@DynamicTest(expected = TransactionRequiredException.class)
	public void testInsertOutsideActiveTransaction() {
		Session session = openSession();
		try {
			final StrTestEntity entity = new StrTestEntity( "data" );
			session.persist( entity );
			session.flush();
		}
		finally {
			session.close();
		}
	}

	@DynamicTest(expected = TransactionRequiredException.class)
	public void testUpdateOutsideActiveTransaction() {
		Session session = openSession();
		try {
			// Save audited entity
			session.getTransaction().begin();
			final StrTestEntity entity = new StrTestEntity( "data" );
			session.persist( entity );
			session.getTransaction().commit();

			// Attempt to update outside transaction
			entity.setStr( "modified data" );
			session.update( entity );
			session.flush();
		}
		finally {
			session.close();
		}
	}

	@DynamicTest(expected = TransactionRequiredException.class)
	public void testDeleteOutsideActiveTransaction() {
		Session session = openSession();
		try {
			// Save audited entity
			session.getTransaction().begin();
			final StrTestEntity entity = new StrTestEntity( "data" );
			session.persist( entity );
			session.getTransaction().commit();

			// Attempt to delete outside transaction
			session.delete( entity );
			session.flush();
		}
		finally {
			session.close();
		}
	}
	
	@DynamicTest(expected = TransactionRequiredException.class)
	@Disabled("NYI")
	public void testCollectionUpdateOutsideActiveTransaction() {
		Session session = openSession();
		try {
			// Save audited entity
			session.getTransaction().begin();
			final Person person = new Person();
			final Name name = new Name();
			name.setName( "Name" );
			person.getNames().add( name );
			session.saveOrUpdate( person );
			session.getTransaction().commit();

			// Illegal collection update outside of active transaction
			person.getNames().remove( name );
			session.saveOrUpdate( person );
			session.flush();
		}
		finally {
			session.close();
		}
	}

	@DynamicTest(expected = TransactionRequiredException.class)
	@Disabled("NYI")
	public void testCollectionRemovalOutsideActiveTransaction() {
		Session session = openSession();
		try {
			// Save audited entity
			session.getTransaction().begin();
			final Person person = new Person();
			final Name name = new Name();
			name.setName( "Name" );
			person.getNames().add( name );
			session.saveOrUpdate( person );
			session.getTransaction().commit();

			// Illegal collection update outside of active transaction
			person.setNames( null );
			session.saveOrUpdate( person );
			session.flush();
		}
		finally {
			session.close();
		}
	}
}
