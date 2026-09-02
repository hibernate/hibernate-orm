/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.util.List;
import java.util.function.Predicate;

import org.hibernate.community.dialect.CacheDialect;
import org.hibernate.community.dialect.DB2LegacyDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.DerbyLegacyDialect;
import org.hibernate.community.dialect.H2LegacyDialect;
import org.hibernate.community.dialect.HSQLLegacyDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.community.dialect.IngresDialect;
import org.hibernate.community.dialect.SQLServerLegacyDialect;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.community.dialect.TeradataDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the test-side assumptions which replaced database-environment
/// characteristics formerly declared by production Dialects.
///
/// @author Steve Ebersole
class DialectTestSupportDatabaseCharacteristicsTest {
	@Test
	void rejectsNullDialect() {
		assertThrows(
				NullPointerException.class,
				() -> DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( null )
		);
		assertThrows(
				NullPointerException.class,
				() -> DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( null )
		);
		assertThrows(
				NullPointerException.class,
				() -> DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( null )
		);
	}

	@Test
	void preservesTheUnmatchedDialectDefaults() {
		final Dialect dialect = new UnmatchedDialect();

		assertTrue( DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( dialect ) );
		assertFalse( DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( dialect ) );
		assertFalse( DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( dialect ) );
	}

	@Test
	void preservesForwardOnlyCursorPositioningAssumptions() {
		assertFalseForEach(
				DialectTestSupport::supportsResultSetPositioningOnForwardOnlyCursor,
				new DB2Dialect(),
				new DB2iDialect(),
				new DB2LegacyDialect(),
				new SQLServerDialect(),
				new SQLServerLegacyDialect(),
				new DerbyDialect(),
				new DerbyLegacyDialect(),
				new CacheDialect()
		);
		assertTrue( DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( new HSQLDialect() ) );
	}

	@Test
	void preservesReadCommittedBlockingAssumptions() {
		assertTrueForEach(
				DialectTestSupport::doesReadCommittedCauseWritersToBlockReaders,
				new SybaseDialect(),
				new DB2iDialect(),
				new DB2zDialect(),
				new DB2LegacyDialect(),
				new HSQLDialect(),
				new DerbyDialect(),
				new DerbyLegacyDialect(),
				new InformixDialect(),
				new SQLiteDialect(),
				new H2LegacyDialect(),
				new TeradataDialect()
		);
		assertFalseForEach(
				DialectTestSupport::doesReadCommittedCauseWritersToBlockReaders,
				new UnmatchedDialect(),
				new SQLServerDialect(),
				new SQLServerLegacyDialect(),
				new DB2Dialect()
		);
	}

	@Test
	void preservesRepeatableReadBlockingAssumptions() {
		assertTrueForEach(
				DialectTestSupport::doesRepeatableReadCauseReadersToBlockWriters,
				new SybaseDialect(),
				new HSQLDialect(),
				new SQLiteDialect(),
				new TeradataDialect()
		);
		assertFalseForEach(
				DialectTestSupport::doesRepeatableReadCauseReadersToBlockWriters,
				new UnmatchedDialect(),
				new SQLServerDialect(),
				new SQLServerLegacyDialect(),
				new DB2Dialect(),
				new DB2LegacyDialect()
		);
	}

	@Test
	void preservesVersionBoundaries() {
		assertFalse( DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders(
				new IngresDialect( DatabaseVersion.make( 9, 2 ) )
		) );
		assertTrue( DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders(
				new IngresDialect( DatabaseVersion.make( 9, 3 ) )
		) );
		assertFalse( DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters(
				new IngresDialect( DatabaseVersion.make( 9, 2 ) )
		) );
		assertTrue( DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters(
				new IngresDialect( DatabaseVersion.make( 9, 3 ) )
		) );

		assertFalse( DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders(
				new HSQLLegacyDialect( DatabaseVersion.make( 1, 9 ) )
		) );
		assertTrue( DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders(
				new HSQLLegacyDialect( DatabaseVersion.make( 2 ) )
		) );
		assertFalse( DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters(
				new HSQLLegacyDialect( DatabaseVersion.make( 1, 9 ) )
		) );
		assertTrue( DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters(
				new HSQLLegacyDialect( DatabaseVersion.make( 2 ) )
		) );
	}

	@Test
	void legacyAndJunitFeatureChecksDelegateToTheSameAssumptions() {
		for ( Dialect dialect : representativeDialects() ) {
			assertEquals(
					DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( dialect ),
					new DialectChecks.SupportsResultSetPositioningOnForwardOnlyCursorCheck().isMatch( dialect )
			);
			assertEquals(
					DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( dialect ),
					new DialectFeatureChecks.SupportsResultSetPositioningOnForwardOnlyCursorCheck().apply( dialect )
			);
			assertEquals(
					DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( dialect ),
					new DialectChecks.DoesReadCommittedCauseWritersToBlockReadersCheck().isMatch( dialect )
			);
			assertEquals(
					DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( dialect ),
					new DialectFeatureChecks.DoesReadCommittedCauseWritersToBlockReadersCheck().apply( dialect )
			);
			assertEquals(
					DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( dialect ),
					new DialectChecks.DoesRepeatableReadCauseReadersToBlockWritersCheck().isMatch( dialect )
			);
			assertEquals(
					DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( dialect ),
					new DialectFeatureChecks.DoesRepeatableReadCauseReadersToBlockWritersCheck().apply( dialect )
			);
		}
	}

	private static List<Dialect> representativeDialects() {
		return List.of(
				new UnmatchedDialect(),
				new DB2Dialect(),
				new DB2iDialect(),
				new SQLServerDialect(),
				new SybaseDialect(),
				new IngresDialect( DatabaseVersion.make( 9, 2 ) ),
				new IngresDialect( DatabaseVersion.make( 9, 3 ) ),
				new HSQLLegacyDialect( DatabaseVersion.make( 1, 9 ) ),
				new HSQLLegacyDialect( DatabaseVersion.make( 2 ) )
		);
	}

	private static void assertTrueForEach(Predicate<Dialect> check, Dialect... dialects) {
		for ( Dialect dialect : dialects ) {
			assertTrue( check.test( dialect ), dialect.getClass().getName() );
		}
	}

	private static void assertFalseForEach(Predicate<Dialect> check, Dialect... dialects) {
		for ( Dialect dialect : dialects ) {
			assertFalse( check.test( dialect ), dialect.getClass().getName() );
		}
	}

	private static class UnmatchedDialect extends Dialect {
		private UnmatchedDialect() {
			super( DatabaseVersion.make( 1 ) );
		}
	}
}
