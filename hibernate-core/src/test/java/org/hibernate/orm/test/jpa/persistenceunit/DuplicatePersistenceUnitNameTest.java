/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.util.List;

import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import org.hibernate.jpa.internal.JpaLogger;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.LoggingInspections;
import org.hibernate.testing.orm.junit.LoggingInspectionsScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.internal.util.ConfigHelper.findAsResource;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11845")
@LoggingInspections(
		messages = {
				@LoggingInspections.Message(
						messageKey = "HHH008518",
						loggers = @org.hibernate.testing.orm.junit.Logger(loggerName = JpaLogger.NAME)
				)
		}
)
public class DuplicatePersistenceUnitNameTest {

	@Test
	public void testDuplicatePersistenceUnitNameLogAWarnMessage(LoggingInspectionsScope scope) {
		PersistenceXmlParser.create().parse( List.of(
				findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" ),
				findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/persistenceUnitForNameDuplicationTest.xml" )
		) );
		assertTrue( scope.getWatcher( "HHH008518", JpaLogger.NAME ).wasTriggered(),
				"The warning HHH015018 was not logged" );
	}
}
