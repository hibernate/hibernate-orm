/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Objects;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2400V7R3Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANACloudColumnStoreDialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HANARowStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SQLServer2016Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;

public class DefaultDialectSelector implements DialectSelector {

	@Override
	public Class<? extends Dialect> resolve(final String name) {
		Objects.requireNonNull( name );
		if ( name.isEmpty() ) {
			return null;
		}
		switch ( name ) {
			case "Cockroach":
				return CockroachDialect.class;
			case "DB2":
				return DB2Dialect.class;
			case "DB2i":
				return DB2iDialect.class;
			case "DB2z":
				return DB2zDialect.class;
			case "DB297":
				return findCommunityDialect( name );
			case "DB2390":
			case "DB2390V8":
				return findCommunityDialect( name );
			case "DB2400":
				return DB2400Dialect.class;
			case "DB2400V7R3":
				return DB2400V7R3Dialect.class;
			case "Derby":
				return DerbyDialect.class;
			case "DerbyTenFive":
			case "DerbyTenSix":
			case "DerbyTenSeven":
				return findCommunityDialect( name );
			case "H2":
				return H2Dialect.class;
			case "HANA":
				return HANADialect.class;
			case "HANACloudColumnStore":
				return HANACloudColumnStoreDialect.class;
			case "HANAColumnStore":
				return HANAColumnStoreDialect.class;
			case "HANARowStore":
				return HANARowStoreDialect.class;
			case "HSQL":
				return HSQLDialect.class;
			case "MariaDB":
				return MariaDBDialect.class;
			case "MariaDB53":
			case "MariaDB10":
			case "MariaDB102":
			case "MariaDB103":
				return findCommunityDialect( name );
			case "MySQL":
				return MySQLDialect.class;
			case "MySQL5":
			case "MySQL55":
			case "MySQL57":
				return findCommunityDialect( name );
			case "MySQL8":
				return MySQL8Dialect.class;
			case "Oracle":
				return OracleDialect.class;
			case "Oracle8i":
			case "Oracle9i":
			case "Oracle10g":
			case "Oracle12c":
				return findCommunityDialect( name );
			case "PostgresPlus":
				return PostgresPlusDialect.class;
			case "PostgreSQL":
				return PostgreSQLDialect.class;
			case "PostgreSQL81":
			case "PostgreSQL82":
			case "PostgreSQL9":
			case "PostgreSQL91":
			case "PostgreSQL92":
			case "PostgreSQL93":
			case "PostgreSQL94":
			case "PostgreSQL95":
			case "PostgreSQL10":
				return findCommunityDialect( name );
			case "Spanner":
				return SpannerDialect.class;
			case "SQLServer":
				return SQLServerDialect.class;
			case "SQLServer2005":
			case "SQLServer2008":
				return findCommunityDialect( name );
			case "SQLServer2012":
				return SQLServer2012Dialect.class;
			case "SQLServer2016":
				return SQLServer2016Dialect.class;
			case "Sybase":
				return SybaseDialect.class;
			case "Sybase11":
				return findCommunityDialect( name );
			case "SybaseASE":
				return SybaseASEDialect.class;
			case "SybaseASE15":
			case "SybaseASE157":
				return findCommunityDialect( name );
		}
		return null;
	}

	private static Class<? extends Dialect> findCommunityDialect(String name) {
		try {
			//noinspection unchecked
			return (Class<? extends Dialect>) DefaultDialectSelector.class.getClassLoader().loadClass(
					"org.hibernate.community.dialect." + name + "Dialect"
			);
		}
		catch (ClassNotFoundException e) {
			throw new StrategySelectionException(
					"Couldn't load the dialect class for the `hibernate.dialect` short name [" + name + "], " +
							"because the application is missing a dependency on the hibernate-community-dialects module. " +
							"Hibernate 6.2 dropped support for database versions that are unsupported by vendors  " +
							"and code for old versions was moved to the hibernate-community-dialects module. " +
							"For further information, read https://in.relation.to/2023/02/15/hibernate-orm-62-db-version-support/",
					e
			);
		}
	}

}
