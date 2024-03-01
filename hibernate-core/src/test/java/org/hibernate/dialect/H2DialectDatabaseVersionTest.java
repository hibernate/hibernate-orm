package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

@RequiresDialect(H2Dialect.class)
@TestForIssue(jiraKey = "HHH-17791")
public class H2DialectDatabaseVersionTest extends BaseUnitTestCase {

	@Test
	public void snapshotVersion() {
		String version = "2.2.229-SNAPSHOT (2023-08-22)";
		Dialect dialect = new H2Dialect( new TestingH2DialectResolutionInfo( version ) );
		assertEquals(229, dialect.getVersion().getMicro());
	}
	
	@Test
	public void releaseVersion() {
		String version = "2.2.224 (2023-09-17)";
		Dialect dialect = new H2Dialect( new TestingH2DialectResolutionInfo( version ) );
		assertEquals(224, dialect.getVersion().getMicro());
	}

	static final class TestingH2DialectResolutionInfo implements DialectResolutionInfo {
		private final String databaseVersion;

		TestingH2DialectResolutionInfo(String databaseVersion) {
			this.databaseVersion = databaseVersion;
		}


		@Override
		public String getDatabaseName() {
			return "H2";
		}

		@Override
		public String getDatabaseVersion() {
			return this.databaseVersion;
		}

		@Override
		public int getDatabaseMajorVersion() {
			return 2;
		}

		@Override
		public int getDatabaseMinorVersion() {
			return 2;
		}

		@Override
		public String getDriverName() {
			return "H2 JDBC Driver";
		}

		@Override
		public int getDriverMajorVersion() {
			return 2;
		}

		@Override
		public int getDriverMinorVersion() {
			return 2;
		}

		@Override
		public String getSQLKeywords() {
			return "";
		}

		@Override
		public String toString() {
			return "2.2";
		}

		@Override
		public Map<String, Object> getConfigurationValues() {
			return Map.of();
		}
		
	}

}
