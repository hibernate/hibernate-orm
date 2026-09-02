/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.spi.DB2JdbcTypes;
import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.type.spi.MariaDBJdbcTypes;
import org.hibernate.dialect.type.spi.MySQLJdbcTypes;
import org.hibernate.dialect.type.spi.OracleJdbcTypes;
import org.hibernate.dialect.type.spi.PostgreSQLJdbcTypes;
import org.hibernate.dialect.type.spi.SQLServerJdbcTypes;
import org.hibernate.dialect.type.spi.SybaseJdbcTypes;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies representative legacy-Dialect contributions through the supported
/// stock JDBC-type facades.
///
/// @author Steve Ebersole
@BaseUnitTest
public class JdbcTypeContributionTest {
	private StandardServiceRegistry serviceRegistry;

	@BeforeEach
	void setUp() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder().build();
	}

	@AfterEach
	void tearDown() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	void contributesDb2H2MySqlAndMariaDbStockTypes() {
		final JdbcTypeRegistry db2 = contributeTypes( new DB2LegacyDialect() );
		assertThat( db2.getDescriptor( SqlTypes.STRUCT ) ).isSameAs( DB2JdbcTypes.struct() );
		assertThat( db2.getDescriptor( SqlTypes.INSTANT ) ).isSameAs( DB2JdbcTypes.instant() );
		assertThat( db2.getDescriptor( SqlTypes.LOCAL_DATE ) ).isSameAs( DB2JdbcTypes.localDate() );
		assertThat( db2.getDescriptor( SqlTypes.LOCAL_TIME ) ).isSameAs( DB2JdbcTypes.localTime() );
		assertThat( db2.getDescriptor( SqlTypes.LOCAL_DATE_TIME ) ).isSameAs( DB2JdbcTypes.localDateTime() );
		assertThat( db2.getDescriptor( SqlTypes.OFFSET_TIME ) ).isSameAs( DB2JdbcTypes.offsetTime() );
		assertThat( db2.getDescriptor( SqlTypes.OFFSET_DATE_TIME ) ).isSameAs( DB2JdbcTypes.offsetDateTime() );
		assertThat( db2.getDescriptor( SqlTypes.ZONED_DATE_TIME ) ).isSameAs( DB2JdbcTypes.zonedDateTime() );

		final JdbcTypeRegistry h2 = contributeTypes( new H2LegacyDialect( DatabaseVersion.make( 2 ) ) );
		assertThat( h2.getDescriptor( SqlTypes.INTERVAL_SECOND ) ).isSameAs( H2JdbcTypes.durationIntervalSecond() );
		assertThat( h2.getDescriptor( SqlTypes.JSON ) ).isSameAs( H2JdbcTypes.json() );
		assertThat( h2.getConstructor( SqlTypes.JSON_ARRAY ) ).isSameAs( H2JdbcTypes.jsonArrayConstructor() );

		final JdbcTypeRegistry mariaDb = contributeTypes( new MariaDBLegacyDialect() );
		assertThat( mariaDb.getDescriptor( SqlTypes.JSON ) ).isSameAs( MariaDBJdbcTypes.castingJson() );
		assertThat( mariaDb.getConstructor( SqlTypes.JSON_ARRAY ) )
				.isSameAs( MariaDBJdbcTypes.castingJsonArrayConstructor() );

		final JdbcTypeRegistry mySql = contributeTypes( new MySQLLegacyDialect( DatabaseVersion.make( 5, 7 ) ) );
		assertThat( mySql.getDescriptor( SqlTypes.JSON ) ).isSameAs( MySQLJdbcTypes.castingJson() );
		assertThat( mySql.getConstructor( SqlTypes.JSON_ARRAY ) )
				.isSameAs( MySQLJdbcTypes.castingJsonArrayConstructor() );
	}

	@Test
	void contributesOraclePostgreSqlAndSqlServerStockTypes() {
		final JdbcTypeRegistry oracle = contributeTypes( new OracleLegacyDialect() );
		assertThat( oracle.getDescriptor( SqlTypes.BOOLEAN ) ).isSameAs( OracleJdbcTypes.booleanType() );
		assertThat( oracle.getDescriptor( SqlTypes.SQLXML ) ).isSameAs( OracleJdbcTypes.xml() );

		final JdbcTypeRegistry postgreSql = contributeTypes( new PostgreSQLLegacyDialect() );
		assertThat( postgreSql.getDescriptor( SqlTypes.NAMED_ENUM ) ).isSameAs( PostgreSQLJdbcTypes.enumType() );
		assertThat( postgreSql.getDescriptor( SqlTypes.NAMED_ORDINAL_ENUM ) )
				.isSameAs( PostgreSQLJdbcTypes.ordinalEnumType() );
		assertThat( postgreSql.getConstructor( SqlTypes.ARRAY ) ).isSameAs( PostgreSQLJdbcTypes.arrayConstructor() );

		final JdbcTypeRegistry sqlServer = contributeTypes( new SQLServerLegacyDialect() );
		assertThat( sqlServer.getDescriptor( SqlTypes.SQLXML ) ).isSameAs( SQLServerJdbcTypes.castingXml() );
		assertThat( sqlServer.getConstructor( SqlTypes.XML_ARRAY ) )
				.isSameAs( SQLServerJdbcTypes.castingXmlArrayConstructor() );
	}

	@Test
	void contributesSybaseJtdsStockTypes() {
		final SybaseLegacyDialect dialect = new SybaseLegacyDialect(
				TestingDialectResolutionInfo.forDatabaseInfo(
						"Adaptive Server Enterprise",
						"jTDS Type 4 JDBC Driver for MS SQL Server and Sybase",
						11,
						0
				)
		);
		final JdbcTypeRegistry registry = contributeTypes( dialect );

		assertThat( registry.getDescriptor( SqlTypes.NCLOB ) ).isSameAs( SybaseJdbcTypes.jtdsNClob() );
		assertThat( registry.getDescriptor( SqlTypes.JSON ) ).isSameAs( SybaseJdbcTypes.jtdsJson() );
		assertThat( registry.getDescriptor( SqlTypes.SQLXML ) ).isSameAs( SybaseJdbcTypes.jtdsXml() );
		assertThat( registry.getConstructor( SqlTypes.JSON_ARRAY ) )
				.isSameAs( SybaseJdbcTypes.jtdsJsonArrayConstructor() );
		assertThat( registry.getConstructor( SqlTypes.XML_ARRAY ) )
				.isSameAs( SybaseJdbcTypes.jtdsXmlArrayConstructor() );
	}

	private JdbcTypeRegistry contributeTypes(Dialect dialect) {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		dialect.contributeTypes( () -> typeConfiguration, serviceRegistry );
		return typeConfiguration.getJdbcTypeRegistry();
	}
}
