/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.MappingException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies the supported sequence strategy families and every maintained
/// Dialect supply point.
///
/// @author Steve Ebersole
public class SequenceSupportTest {
	@Test
	void stockStrategiesAreStableAndPreserveTheirGrammar() {
		assertThat( SequenceSupports.none() ).isSameAs( SequenceSupports.none() );
		assertThat( SequenceSupports.ansi() ).isSameAs( SequenceSupports.ansi() );
		assertThat( SequenceSupports.nextval() ).isSameAs( SequenceSupports.nextval() );
		assertThat( SequenceSupports.db2() ).isSameAs( SequenceSupports.db2() );

		final SequenceSupport none = SequenceSupports.none();
		assertThat( none.supportsSequences() ).isFalse();
		assertThat( none.supportsPooledSequences() ).isFalse();
		assertThatThrownBy( () -> none.getSelectSequenceNextValString( "seq" ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getSequenceNextValString( "seq" ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getSequenceNextValString( "seq", 4 ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getCreateSequenceString( "seq" ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getCreateSequenceString( "seq", 1, 4 ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getCreateSequenceStrings( "seq", 1, 4 ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getCreateSequenceStrings( "seq", 1, 4, "cache 8" ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getDropSequenceString( "seq" ) )
				.isInstanceOf( MappingException.class );
		assertThatThrownBy( () -> none.getDropSequenceStrings( "seq" ) )
				.isInstanceOf( MappingException.class );

		final SequenceSupport ansi = SequenceSupports.ansi();
		assertThat( ansi.supportsSequences() ).isTrue();
		assertThat( ansi.supportsPooledSequences() ).isTrue();
		assertThat( ansi.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "next value for seq" );
		assertThat( ansi.getSelectSequencePreviousValString( "seq" ) ).isEqualTo( "current value for seq" );
		assertThat( ansi.getSequenceNextValString( "seq" ) ).isEqualTo( "select next value for seq" );
		assertThat( ansi.getSequenceNextValString( "seq", 4 ) ).isEqualTo( "select next value for seq" );
		assertThat( ansi.getSequencePreviousValString( "seq" ) ).isEqualTo( "select current value for seq" );
		assertThat( ansi.getFromDual() ).isEmpty();
		assertThat( ansi.getCreateSequenceString( "seq" ) ).isEqualTo( "create sequence seq" );
		assertThat( ansi.getCreateSequenceString( "seq", 3, 4 ) )
				.isEqualTo( "create sequence seq start with 3 increment by 4" );
		assertThat( ansi.getCreateSequenceStrings( "seq", 3, 4 ) )
				.containsExactly( "create sequence seq start with 3 increment by 4" );
		assertThat( ansi.getCreateSequenceStrings( "seq", 3, 4, "cache 8" ) )
				.containsExactly( "create sequence seq start with 3 increment by 4 cache 8" );
		assertThat( ansi.getDropSequenceString( "seq" ) ).isEqualTo( "drop sequence seq" );
		assertThat( ansi.getDropSequenceStrings( "seq" ) ).containsExactly( "drop sequence seq" );
		assertThat( ansi.getRestartSequenceString( "seq", 9 ) )
				.isEqualTo( "alter sequence seq restart with 9" );
		assertThat( ansi.sometimesNeedsStartingValue() ).isFalse();
		assertThat( ansi.startingValue( -3, 4 ) ).isEmpty();

		final SequenceSupport nextval = SequenceSupports.nextval();
		assertThat( nextval.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "seq.nextval" );
		assertThat( nextval.getSelectSequencePreviousValString( "seq" ) ).isEqualTo( "seq.currval" );
		assertThat( nextval.getSequenceNextValString( "seq" ) ).isEqualTo( "select seq.nextval" );
		assertThat( nextval.getSequencePreviousValString( "seq" ) ).isEqualTo( "select seq.currval" );

		final SequenceSupport db2 = SequenceSupports.db2();
		assertThat( db2.getSelectSequenceNextValString( "seq" ) ).isEqualTo( "next value for seq" );
		assertThat( db2.getSelectSequencePreviousValString( "seq" ) ).isEqualTo( "previous value for seq" );
		assertThat( db2.getSequenceNextValString( "seq" ) ).isEqualTo( "values next value for seq" );
		assertThat( db2.getSequencePreviousValString( "seq" ) ).isEqualTo( "values previous value for seq" );
		assertThat( db2.getDropSequenceString( "seq" ) ).isEqualTo( "drop sequence seq restrict" );

		assertThatThrownBy( () -> ansi.getCreateSequenceString( "seq", 1, 0 ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "increment size must not be 0" );
	}

	@Test
	void signSensitiveStartingValuesPreserveBothDirections() {
		final SequenceSupport support = new SequenceSupport() {
			@Override
			public String getSelectSequenceNextValString(String sequenceName) {
				return sequenceName;
			}

			@Override
			public boolean sometimesNeedsStartingValue() {
				return true;
			}
		};

		assertThat( support.startingValue( -5, 2 ) ).isEqualTo( " minvalue -5" );
		assertThat( support.startingValue( 5, -2 ) ).isEqualTo( " maxvalue 5" );
		assertThat( support.startingValue( 5, 2 ) ).isEmpty();
		assertThat( support.startingValue( -5, -2 ) ).isEmpty();
		assertThat( support.getCreateSequenceString( "seq", -5, 2 ) )
				.isEqualTo( "create sequence seq minvalue -5 start with -5 increment by 2" );
		assertThat( support.getCreateSequenceString( "seq", 5, -2 ) )
				.isEqualTo( "create sequence seq maxvalue 5 start with 5 increment by -2" );
	}

	@Test
	void maintainedDialectsKeepTheirStrategyFamilies() {
		assertDialectSupply( new CockroachDialect(), "PostgreSQLSequenceSupport" );
		assertDialectSupply( new DB2Dialect(), "DB2SequenceSupport" );
		assertDialectSupply(
				new DB2iDialect( DatabaseVersion.make( 7, 3 ) ),
				"DB2iSequenceSupport"
		);
		assertDialectSupply( new DB2zDialect(), "DB2zSequenceSupport" );
		assertDialectSupply( new H2Dialect(), "H2V2SequenceSupport" );
		assertDialectSupply( new HANADialect(), "HANASequenceSupport" );
		assertDialectSupply( new HSQLDialect(), "HSQLSequenceSupport" );
		assertDialectSupply( new MariaDBDialect(), "MariaDBSequenceSupport" );
		assertDialectSupply( new MySQLDialect(), "NoSequenceSupport" );
		assertDialectSupply( new OracleDialect(), "OracleSequenceSupport" );
		assertDialectSupply( new PostgreSQLDialect(), "PostgreSQLSequenceSupport" );
		assertDialectSupply(
				new SQLServerDialect( DatabaseVersion.make( 16 ) ),
				"SQLServer16SequenceSupport"
		);
		assertDialectSupply( new SpannerDialect(), "SpannerSequenceSupport" );
		assertDialectSupply(
				new SpannerPostgreSQLDialect(),
				"SpannerPostgreSQLSequenceSupport"
		);
	}

	private static void assertDialectSupply(
			Dialect dialect,
			String strategySimpleName) {
		assertThat( dialect.getSequenceSupport().getClass().getSimpleName() ).isEqualTo( strategySimpleName );
	}
}
