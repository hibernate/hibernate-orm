/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Jan Schatteman
 */
@RequiresDialect(CockroachDialect.class)
public class CockroachDialectVersionTest {

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension
			.builder().setLogger(
					Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, CockroachDialect.class.getName()  )
			).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages("HHH000512" );
		triggerable.reset();
	}

	@Test
	@JiraKey(value = "HHH-15511")
	public void testCockroachDialectVersionParsing() {
		DatabaseVersion dv;
		// using a fictitious major version, to avoid minimum version warnings
		Dialect dialect = new CockroachDBTestDialect( "CockroachDB CCL v99.2.10 (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)" );

		dv = dialect.getVersion();
		assertNotNull( dv );
		assertEquals( 99, dv.getMajor() );
		assertEquals( 2, dv.getMinor() );
		assertEquals( 10, dv.getMicro() );

		dialect = new CockroachDBTestDialect("CockroachDB CCL v99.2. (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)");
		dv = dialect.getVersion();
		assertNotNull( dv );
		assertEquals( 99, dv.getMajor() );
		assertEquals( 2, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );

		dialect = new CockroachDBTestDialect("CockroachDB CCL v99.2 (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)");
		dv = dialect.getVersion();
		assertNotNull( dv );
		assertEquals( 99, dv.getMajor() );
		assertEquals( 2, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );

		dialect = new CockroachDBTestDialect("CockroachDB CCL v99. (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)");
		dv = dialect.getVersion();
		assertNotNull( dv );
		assertEquals( 99, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );

		dialect = new CockroachDBTestDialect("CockroachDB CCL v99 (x86_64-unknown-linux-gnu, built 2022/05/02 17:38:58, go1.16.6)");
		dv = dialect.getVersion();
		assertNotNull( dv );
		assertEquals( 99, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );
	}

	private static final class CockroachDBTestDialect extends CockroachDialect {
		private CockroachDBTestDialect(String versionString) {
			super (parseVersion( versionString ));
		}

		private DatabaseVersion getMinimumVersion() {
			return getMinimumSupportedVersion();
		}
	}
}
