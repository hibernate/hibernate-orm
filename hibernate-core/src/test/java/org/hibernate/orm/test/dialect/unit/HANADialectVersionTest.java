/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HANAServerConfiguration;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(HANADialect.class)
public class HANADialectVersionTest {

	@Test
	public void testStaticVersion() {
		// HANA database 2.0 SPS 07
		DatabaseVersion dv = HANAServerConfiguration.staticDetermineDatabaseVersion( "2.00.076.00.1705400033" );
		assertEquals( 2, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 76, dv.getMicro() );

		// HANA Cloud version QRC 3/2024
		dv = HANAServerConfiguration.staticDetermineDatabaseVersion( "4.00.000.00.1730808477" );
		assertNotNull( dv );
		assertEquals( 4, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );

		// HANA Cloud version QRC 2/2025
		dv = HANAServerConfiguration.staticDetermineDatabaseVersion( "4.00.000.00.1755603748" );
		assertNotNull( dv );
		assertEquals( 4, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );
	}

	@Test
	public void testDynamicVersion() {
		// HANA database 2.0 SPS 07
		DatabaseVersion dv = HANAServerConfiguration.determineDatabaseVersion( "2.00.076.00.1705400033 (fa/hana2sp07)" );
		assertEquals( 2, dv.getMajor() );
		assertEquals( 0, dv.getMinor() );
		assertEquals( 76, dv.getMicro() );

		// HANA Cloud version QRC 3/2024
		dv = HANAServerConfiguration.determineDatabaseVersion( "4.00.000.00.1730808477 (fa/CE2024.42)" );
		assertNotNull( dv );
		assertEquals( 4, dv.getMajor() );
		assertEquals( 2024_4, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );

		// HANA Cloud version QRC 2/2025
		dv = HANAServerConfiguration.determineDatabaseVersion( "4.00.000.00.1755603748 (fa/CE2025.14)" );
		assertNotNull( dv );
		assertEquals( 4, dv.getMajor() );
		assertEquals( 2025_2, dv.getMinor() );
		assertEquals( 0, dv.getMicro() );
	}
}
