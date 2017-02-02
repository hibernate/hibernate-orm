/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.hql;

import java.util.Collections;

import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.internal.log.DeprecationLogger;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests that the forms of referencing parts of and info about collections as a property
 * gets logged as a deprecation warning.  E.g. {@code `h.family.elements`} is
 * deprecated in preference for {@code `elements(h.family)`}
 *
 * @author Steve Ebersole
 */
public class CollectionPropertyDeprecationsTest extends BaseCoreFunctionalTestCase {
	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			DeprecationLogger.DEPRECATION_LOGGER
	);

	@Override
	public String[] getMappings() {
		return new String[] {"hql/Animal.hbm.xml"};
	}

	@Override
	public boolean createSchema() {
		return false;
	}

	@Override
	public boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingBagElements() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select elements(h.friends) from Human h" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select h from Human h where h in elements(h.friends)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select h.friends.elements from Human h" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingSetElements() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select elements(h.nickNames) from Human h" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select h from Human h where h.name.first in elements(h.nickNames)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select h.nickNames.elements from Human h" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingListElements() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select elements(u.permissions) from User u" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select u from User u where u.userName in elements(u.permissions)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select u.permissions.elements from User u" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingListIndices() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select indices(u.permissions) from User u" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select u from User u where u.userName in indices(u.permissions)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select u.permissions.indices from User u" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingMapElements() {
		// NOTE : JPA's VALUE ought to work fine as we never supported
		// 		that in the legacy form...

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select elements(h.family) from Human h" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select h from Human h where h.name.first in elements(h.family)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select h.family.elements from Human h" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingMapIndices() {
		// NOTE : JPA's KEY ought to work fine as we never supported
		// 		that in the legacy form...

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select indices(h.family) from Human h" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select h from Human h where h.name.first in indices(h.family)" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select h.family.indices from Human h" );
		assertTrue( triggerable.wasTriggered() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11400" )
	public void testReferencingSize() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH90000016" );

		// first the accepted ways
		compileQuery( "select size(h.family) from Human h" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();
		compileQuery( "select h from Human h where size(h.family) = 1" );
		assertFalse( triggerable.wasTriggered() );
		triggerable.reset();

		// then the deprecated way
		compileQuery( "select h.family.size from Human h" );
		assertTrue( triggerable.wasTriggered() );
	}




	private QueryTranslatorImpl compileQuery(String hql) {
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslatorImpl newQueryTranslator = (QueryTranslatorImpl) ast.createQueryTranslator(
				hql,
				hql,
				Collections.EMPTY_MAP,
				sessionFactory(),
				null
		);
		newQueryTranslator.compile( Collections.emptyMap(), false );
		return newQueryTranslator;
	}
}
