/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sequence.spi.ANSISequenceSupport;
import org.hibernate.dialect.sequence.spi.DB2SequenceSupport;
import org.hibernate.dialect.sequence.spi.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the community Dialect sequence strategy families and supply
/// points.
///
/// @author Steve Ebersole
public class SequenceSupportTest {
	@Test
	void communityImplementationsExtendEverySupportedFamily() {
		final SequenceSupport nextval = new AltibaseDialect().getSequenceSupport();
		assertThat( nextval ).isInstanceOf( NextvalSequenceSupport.class );
		assertThat( nextval.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "seq.nextval" );

		final SequenceSupport db2 = new DerbyDialect().getSequenceSupport();
		assertThat( db2 ).isInstanceOf( DB2SequenceSupport.class );
		assertThat( db2.getSequenceNextValString( "seq" ) ).isEqualTo( "values next value for seq" );

		final SequenceSupport ansi = new MimerSQLDialect().getSequenceSupport();
		assertThat( ansi ).isInstanceOf( ANSISequenceSupport.class );
		assertThat( ansi.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "next value for seq" );
	}

	@Test
	void legacyMappingsPreserveTheirGrammar() {
		assertThat( new CockroachLegacyDialect().getSequenceSupport()
				.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "nextval('seq')" );
		assertThat( new DB2LegacyDialect( DatabaseVersion.make( 11 ) ).getSequenceSupport() )
				.isSameAs( SequenceSupports.db2() );
		assertThat( new DB2iLegacyDialect( DatabaseVersion.make( 7, 3 ) ).getSequenceSupport()
				.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "nextval for seq" );
		assertThat( new DB2zLegacyDialect( DatabaseVersion.make( 8 ) ).getSequenceSupport() )
				.isSameAs( CommunitySequenceSupports.db2z() );
		assertThat( new H2LegacyDialect( DatabaseVersion.make( 2, 0, 202 ) ).getSequenceSupport() )
				.isSameAs( CommunitySequenceSupports.h2v2() );
		assertThat( new HANALegacyDialect().getSequenceSupport() ).isSameAs( CommunitySequenceSupports.hana() );
		assertThat( new HSQLLegacyDialect().getSequenceSupport() ).isSameAs( CommunitySequenceSupports.hsql() );
		assertThat( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 3 ) ).getSequenceSupport() )
				.isSameAs( CommunitySequenceSupports.mariaDB() );
		assertThat( new MySQLLegacyDialect().getSequenceSupport() ).isSameAs( SequenceSupports.none() );
		assertThat( new OracleLegacyDialect().getSequenceSupport()
				.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "seq.nextval" );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 2 ) ).getSequenceSupport()
				.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "nextval('seq')" );
		assertThat( new SQLServerLegacyDialect( DatabaseVersion.make( 16 ) ).getSequenceSupport() )
				.isSameAs( CommunitySequenceSupports.sqlServer16() );
	}
}
