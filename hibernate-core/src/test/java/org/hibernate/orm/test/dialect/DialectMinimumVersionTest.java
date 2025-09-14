/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SimpleDatabaseVersion;

import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;



import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Jan Schatteman
 */
public class DialectMinimumVersionTest {

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension.builder().setLogger( CORE_LOGGER ).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages("HHH000511" );
		triggerable.reset();
	}

	@Test
	public void testLessThanDialectMinimumVersion() {
		String failMsg = "HHH000511: The version for ... is no longer supported ... should have been logged";

		DummyDialect dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 9, 5, 1 ) );
		Assertions.assertTrue( triggerable.wasTriggered(),  failMsg);
		triggerable.reset();
		dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 10, 4, 1 ) );
		Assertions.assertTrue( triggerable.wasTriggered(),  failMsg);
		triggerable.reset();
		dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 10, 5, 0 ) );
		Assertions.assertTrue( triggerable.wasTriggered(),  failMsg);
	}

	@Test
	public void testMoreThanDialectMinimumVersion() {
		String failMsg = "HHH000511: The version for ... is no longer supported ... should not have been logged";

		DummyDialect dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 11, 5, 1 ) );
		Assertions.assertFalse( triggerable.wasTriggered(), failMsg );
		triggerable.reset();
		dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 10, 6, 1 ) );
		Assertions.assertFalse( triggerable.wasTriggered(), failMsg );
		triggerable.reset();
		dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 10, 5, 2 ) );
		Assertions.assertFalse( triggerable.wasTriggered(), failMsg );
	}

	@Test
	public void testSameAsDialectMinimumVersion() {
		DummyDialect dummyDialect = new DummyDialect( new SimpleDatabaseVersion( 10, 5, 1 ) );
		Assertions.assertFalse( triggerable.wasTriggered(), "HHH000511: The version for ... is no longer supported ... should not have been logged" );
	}

	private final static class DummyDialect extends Dialect {
		private DummyDialect(DatabaseVersion version) {
			super( version );
		}

		@Override
		protected DatabaseVersion getMinimumSupportedVersion() {
			return new SimpleDatabaseVersion( 10, 5, 1 );
		}
	}
}
