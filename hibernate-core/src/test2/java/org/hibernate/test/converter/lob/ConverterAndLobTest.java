/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.lob;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class ConverterAndLobTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-9615" )
	@SuppressWarnings("unchecked")
	public void basicTest() {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during store...
		PostalAreaConverter.clearCounts();

		Session session = openSession();
		session.getTransaction().begin();
		session.save( new Address( 1, "123 Main St.", null, PostalArea._78729 ) );
		session.getTransaction().commit();
		session.close();

		assertThat( PostalAreaConverter.toDatabaseCallCount, is(1) );
		assertThat( PostalAreaConverter.toDomainCallCount, is(0) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test during load...
		PostalAreaConverter.clearCounts();

		session = openSession();
		session.getTransaction().begin();
		Address address = session.get( Address.class, 1 );
		session.getTransaction().commit();
		session.close();

		assertThat( PostalAreaConverter.toDatabaseCallCount, is(0) );
		assertThat( PostalAreaConverter.toDomainCallCount, is(1) );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cleanup
		session = openSession();
		session.getTransaction().begin();
		session.delete( address );
		session.getTransaction().commit();
		session.close();
	}


	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class };
	}

}
