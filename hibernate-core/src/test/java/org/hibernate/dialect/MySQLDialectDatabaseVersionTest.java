/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Map;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

@RequiresDialect(MySQLDialect.class)
@JiraKey(value = "HHH-18518")
public class MySQLDialectDatabaseVersionTest extends BaseUnitTestCase {

	@Test
	public void versionWithSuffix() {
		String version = "8.0.37-azure";
		Dialect dialect = new MySQLDialect( new TestingMySQLDialectResolutionInfo( version ) );

		assertEquals(8, dialect.getVersion().getMajor());
		assertEquals(0, dialect.getVersion().getMinor());
		assertEquals(37, dialect.getVersion().getMicro());
	}

	@Test
	public void releaseVersion() {
		String version = "8.0.37";
		Dialect dialect = new MySQLDialect( new TestingMySQLDialectResolutionInfo( version ) );

		assertEquals(8, dialect.getVersion().getMajor());
		assertEquals(0, dialect.getVersion().getMinor());
		assertEquals(37, dialect.getVersion().getMicro());
	}

	static final class TestingMySQLDialectResolutionInfo implements DialectResolutionInfo {
		private final String databaseVersion;

		TestingMySQLDialectResolutionInfo(String databaseVersion) {
			this.databaseVersion = databaseVersion;
		}


		@Override
		public String getDatabaseName() {
			return "MySQL";
		}

		@Override
		public String getDatabaseVersion() {
			return this.databaseVersion;
		}

		@Override
		public int getDatabaseMajorVersion() {
			return 8;
		}

		@Override
		public int getDatabaseMinorVersion() {
			return 0;
		}

		@Override
		public String getDriverName() {
			return "MySQL JDBC Driver";
		}

		@Override
		public int getDriverMajorVersion() {
			return 8;
		}

		@Override
		public int getDriverMinorVersion() {
			return 3;
		}

		@Override
		public String getSQLKeywords() {
			return "";
		}

		@Override
		public String toString() {
			return "8.3.0";
		}

		@Override
		public Map<String, Object> getConfigurationValues() {
			return Map.of();
		}

	}

}
