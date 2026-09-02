/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.Length;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.engine.jdbc.Size;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies every direct community type-sizing profile owner.
///
/// @author Steve Ebersole
public class TypeSizingProfileTest {
	private static final int L32 = Length.LONG32;
	private static final long LOB = Size.DEFAULT_LOB_LENGTH;
	private static final int NO = TypeSizingProfile.UNSUPPORTED;

	@Test
	void directOwnersPreserveTheirEffectiveSizingTuples() {
		assertProfile( new AltibaseDialect(), 38, 6, 9, LOB, 24, 53, 32_000, 32_000, 32_000, 32_000, 32_000, 32_000 );
		assertProfile( new CUBRIDDialect(), 38, 3, 9, LOB, 21, 53, 1_073_741_823, 1_073_741_823, 1_073_741_823, 1_073_741_823, 1_073_741_823, 1_073_741_823 );
		assertProfile( new CacheDialect(), 19, 6, 9, LOB, 24, 53, L32, L32, L32, L32, L32, L32 );
		assertProfile( new CockroachLegacyDialect(), 38, 6, 6, LOB, 24, 53, L32, L32, L32, L32, L32, L32 );
		assertProfile( new DB2LegacyDialect(), 31, 6, 9, LOB, 24, 53, 32_672, 32_672, 32_672, 32_672, 32_672, 32_672 );
		assertProfile( new DerbyDialect(), 31, 9, 9, LOB, 23, 52, 32_672, 32_672, 32_672, 32_700, 32_672, 32_672 );
		assertProfile( new DerbyLegacyDialect(), 31, 9, 9, LOB, 23, 52, 32_672, 32_672, 32_672, 32_700, 32_672, 32_672 );
		assertProfile( new FirebirdDialect(), 18, 3, 9, LOB, 21, 53, 8191, 8191, 32_765, 8191, 8191, 32_765 );
		assertProfile( new GaussDBDialect(), 38, 6, 6, LOB, 24, 53, 10_485_760, 10_485_760, L32, 1_073_741_727, 10_485_760, L32 );
		assertProfile( new H2LegacyDialect(), 38, 6, 9, LOB, 24, 53, 1_048_576, 1_048_576, 1_048_576, 1_048_576, 1_048_576, 1_048_576 );
		assertProfile( new HANALegacyDialect(), 34, 7, 9, LOB, 24, 53, 5000, 5000, 5000, 5000, 5000, 5000 );
		assertProfile( new InformixDialect(), 32, 3, 9, LOB, 8, 16, 32_739, 32_739, NO, 32_739, 32_739, NO );
		assertProfile( new IngresDialect(), 39, 6, 9, LOB, 24, 53, 32_000, 16_000, 32_000, 32_000, 16_000, 32_000 );
		assertProfile( new InterSystemsIRISDialect(), 38, 6, 9, LOB, 24, 53, 32_767, 32_767, 32_767, 32_767, 32_767, 32_767 );
		assertProfile( new MaxDBDialect(), 38, 6, 9, LOB, 24, 53, L32, L32, NO, L32, L32, NO );
		assertProfile( new MimerSQLDialect(), 38, 6, 9, LOB, 24, 53, 15_000, 5000, 15_000, 15_000, 5000, 15_000 );
		assertProfile( new MySQLLegacyDialect(), 38, 6, 9, 16_777_215, 23, 53, 16_383, 16_383, 65_535, 16_383, 16_383, 65_535 );
		assertProfile( new OracleLegacyDialect(), 38, 6, 9, LOB, 24, 53, 4000, 4000, 2000, 4000, 4000, 2000 );
		assertProfile( new PostgreSQLLegacyDialect(), 38, 6, 6, LOB, 24, 53, 10_485_760, 10_485_760, L32, 1_073_741_824, 10_485_760, L32 );
		assertProfile( new RDMSOS2200Dialect(), 21, 6, 9, LOB, 24, 53, L32, L32, NO, L32, L32, NO );
		assertProfile( new SQLServerLegacyDialect(), 38, 7, 9, Length.LONG32, 24, 53, 8000, 4000, 8000, 8000, 4000, 8000 );
		assertProfile( new SQLiteDialect(), 38, 6, 9, LOB, 24, 53, L32, L32, NO, L32, L32, NO );
		assertProfile( new SingleStoreDialect(), 65, 6, 9, Length.LONG32, 23, 53, 21_844, 21_844, 65_533, 21_844, 21_844, 65_533 );
		assertProfile( new SybaseASELegacyDialect(), 38, 6, 9, LOB, 15, 48, 16_384, 16_384, 16_384, 16_384, 16_384, 16_384 );
		assertProfile( new SybaseAnywhereDialect(), 38, 6, 9, LOB, 24, 53, Length.LONG16, Length.LONG16, Length.LONG16, Length.LONG16, Length.LONG16, Length.LONG16 );
		assertProfile( new TeradataDialect(), 18, 6, 9, LOB, 24, 53, 32_000, 32_000, 64_000, 32_000, 32_000, 64_000 );
		assertProfile( new TimesTenDialect(), 40, 6, 9, LOB, 24, 53, 4_194_304, 4_194_304, 4_194_304, 4_194_304, 4_194_304, 4_194_304 );
	}

	@Test
	void inheritedFamiliesReuseTheirSizingOwner() {
		assertThat( new MariaDBLegacyDialect().getTypeSizingProfile().maxVarcharLength() )
				.isEqualTo( new MySQLLegacyDialect().getTypeSizingProfile().maxVarcharLength() );
		assertThat( new SybaseLegacyDialect().getTypeSizingProfile() )
				.isSameAs( TypeSizingProfile.STANDARD );
	}

	private static void assertProfile(
			Dialect dialect,
			int decimal,
			int timestamp,
			int interval,
			long lob,
			int floatPrecision,
			int doublePrecision,
			int varcharLength,
			int nvarcharLength,
			int varbinaryLength,
			int varcharCapacity,
			int nvarcharCapacity,
			int varbinaryCapacity) {
		final TypeSizingProfile profile = dialect.getTypeSizingProfile();
		assertThat( profile ).isSameAs( dialect.getTypeSizingProfile() );
		assertThat( profile.defaultDecimalPrecision() ).isEqualTo( decimal );
		assertThat( profile.defaultTimestampPrecision() ).isEqualTo( timestamp );
		assertThat( profile.defaultIntervalSecondScale() ).isEqualTo( interval );
		assertThat( profile.defaultLobLength() ).isEqualTo( lob );
		assertThat( profile.floatPrecision() ).isEqualTo( floatPrecision );
		assertThat( profile.doublePrecision() ).isEqualTo( doublePrecision );
		assertThat( profile.maxVarcharLength() ).isEqualTo( varcharLength );
		assertThat( profile.maxNVarcharLength() ).isEqualTo( nvarcharLength );
		assertThat( profile.maxVarbinaryLength() ).isEqualTo( varbinaryLength );
		assertThat( profile.maxVarcharCapacity() ).isEqualTo( varcharCapacity );
		assertThat( profile.maxNVarcharCapacity() ).isEqualTo( nvarcharCapacity );
		assertThat( profile.maxVarbinaryCapacity() ).isEqualTo( varbinaryCapacity );
	}
}
