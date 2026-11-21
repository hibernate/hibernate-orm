package org.hibernate.orm.test.dialect;

import java.util.stream.Stream;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
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
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.MutationFeature.INSERT_CONFLICT;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.Placement.TOP_LEVEL;
import static org.hibernate.dialect.sql.ast.spi.CteSupport.RecursiveFeature.RECURSIVE;

class DialectCapabilityLookupTests {
	@ParameterizedTest
	@MethodSource("dialects")
	void capabilityProfilesAreReused(Dialect dialect) {
		assertThat( dialect.getCteSupport() ).isNotNull().isSameAs( dialect.getCteSupport() );
		assertThat( dialect.getMutationSyntaxSupport() ).isNotNull().isSameAs( dialect.getMutationSyntaxSupport() );
	}

	private static Stream<Dialect> dialects() {
		return Stream.of(
				new CockroachDialect(),
				new DB2Dialect(),
				new H2Dialect(),
				new HANADialect(),
				new HSQLDialect(),
				new MariaDBDialect(),
				new MySQLDialect(),
				new OracleDialect(),
				new PostgreSQLDialect(),
				new SQLServerDialect(),
				new SpannerDialect(),
				new SpannerPostgreSQLDialect(),
				new SybaseASEDialect(),
				new SybaseDialect()
		);
	}

	@Test
	void mysqlRecursiveCteSupportDependsOnTheDatabaseVersion() {
		final var before = new MySQLDialect( DatabaseVersion.make( 8, 0, 13 ) );
		final var after = new MySQLDialect( DatabaseVersion.make( 8, 0, 14 ) );

		assertThat( before.getCteSupport().supports( RECURSIVE ) ).isFalse();
		assertThat( after.getCteSupport().supports( RECURSIVE ) ).isTrue();
	}

	@Test
	void hsqlCteSupportFromDatabaseMetadata() {
		final var dialect = new HSQLDialect( TestingDialectResolutionInfo.forDatabaseInfo( "HSQL Database Engine", 2, 7 ) );
		final var support = dialect.getCteSupport();

		assertThat( support.supports( RECURSIVE ) ).isTrue();
		assertThat( support.getPlacement() ).isEqualTo( TOP_LEVEL );
		assertThat( support ).isSameAs( dialect.getCteSupport() );
	}

	@Test
	void inheritedCteProfilesKeepTheSubclassCapabilities() {
		final var mariaDb = new MariaDBDialect().getCteSupport();
		assertThat( mariaDb.getPlacement() ).isEqualTo( TOP_LEVEL );
		assertThat( mariaDb.supports( RECURSIVE ) ).isTrue();

		final var spanner = new SpannerPostgreSQLDialect().getCteSupport();
		assertThat( spanner.getPlacement() ).isEqualTo( TOP_LEVEL );
		assertThat( spanner.supports( RECURSIVE ) ).isFalse();
		assertThat( spanner.supportsCteHeaderColumnList() ).isFalse();
		assertThat( spanner.supports( INSERT_CONFLICT ) ).isTrue();
	}
}
