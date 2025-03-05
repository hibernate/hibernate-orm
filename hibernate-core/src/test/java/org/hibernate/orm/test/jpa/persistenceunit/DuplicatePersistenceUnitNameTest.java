/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.ConfigHelper.findAsResource;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11845")
public class DuplicatePersistenceUnitNameTest extends BaseUnitTestCase {
	private Triggerable triggerable;

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, PersistenceXmlParser.class.getName() )
	);

	@Before
	public void setUp() {
		final Set messagesPrefixes = new HashSet<>();
		messagesPrefixes.add( "HHH015018" );
		triggerable = logInspection.watchForLogMessages( messagesPrefixes );
	}

	@After
	public void tearDown() {
		triggerable.reset();
	}

	@Test
	public void testDuplicatePersistenceUnitNameLogAWarnMessage() {
		PersistenceXmlParser.create().parse( List.of(
				findAsResource(
						"org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml"
				),
				findAsResource(
						"org/hibernate/jpa/test/persistenceunit/META-INF/persistenceUnitForNameDuplicationTest.xml"
				)
		) );
		assertTrue( "The warn HHH015018 has not been logged ", triggerable.wasTriggered() );
	}
}
