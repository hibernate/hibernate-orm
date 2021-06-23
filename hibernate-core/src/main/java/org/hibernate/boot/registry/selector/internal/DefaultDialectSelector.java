/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Objects;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2390Dialect;
import org.hibernate.dialect.DB2390V8Dialect;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2400V7R3Dialect;
import org.hibernate.dialect.DB297Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DerbyTenFiveDialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.DerbyTenSixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANACloudColumnStoreDialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HANARowStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDB102Dialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MariaDB10Dialect;
import org.hibernate.dialect.MariaDB53Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQL55Dialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL91Dialect;
import org.hibernate.dialect.PostgreSQL92Dialect;
import org.hibernate.dialect.PostgreSQL93Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE157Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
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
				return DB297Dialect.class;
			case "DB2390":
				return DB2390Dialect.class;
			case "DB2390V8":
				return DB2390V8Dialect.class;
			case "DB2400":
				return DB2400Dialect.class;
			case "DB2400V7R3":
				return DB2400V7R3Dialect.class;
			case "Derby":
				return DerbyDialect.class;
			case "DerbyTenFive":
				return DerbyTenFiveDialect.class;
			case "DerbyTenSix":
				return DerbyTenSixDialect.class;
			case "DerbyTenSeven":
				return DerbyTenSevenDialect.class;
			case "H2":
				return H2Dialect.class;
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
				return MariaDB53Dialect.class;
			case "MariaDB10":
				return MariaDB10Dialect.class;
			case "MariaDB102":
				return MariaDB102Dialect.class;
			case "MariaDB103":
				return MariaDB103Dialect.class;
			case "MySQL":
				return MySQLDialect.class;
			case "MySQL5":
				return MySQL5Dialect.class;
			case "MySQL55":
				return MySQL55Dialect.class;
			case "MySQL57":
				return MySQL57Dialect.class;
			case "MySQL8":
				return MySQL8Dialect.class;
			case "Oracle":
				return OracleDialect.class;
			case "Oracle8i":
				return Oracle8iDialect.class;
			case "Oracle9i":
				return Oracle9iDialect.class;
			case "Oracle10g":
				return Oracle10gDialect.class;
			case "Oracle12c":
				return Oracle12cDialect.class;
			case "PostgresPlus":
				return PostgresPlusDialect.class;
			case "PostgreSQL":
				return PostgreSQLDialect.class;
			case "PostgreSQL81":
				return PostgreSQL81Dialect.class;
			case "PostgreSQL82":
				return PostgreSQL82Dialect.class;
			case "PostgreSQL9":
				return PostgreSQL9Dialect.class;
			case "PostgreSQL91":
				return PostgreSQL91Dialect.class;
			case "PostgreSQL92":
				return PostgreSQL92Dialect.class;
			case "PostgreSQL93":
				return PostgreSQL93Dialect.class;
			case "PostgreSQL94":
				return PostgreSQL94Dialect.class;
			case "PostgreSQL95":
				return PostgreSQL95Dialect.class;
			case "Spanner":
				return SpannerDialect.class;
			case "SQLServer":
				return SQLServerDialect.class;
			case "SQLServer2005":
				return SQLServer2005Dialect.class;
			case "SQLServer2008":
				return SQLServer2008Dialect.class;
			case "SQLServer2012":
				return SQLServer2012Dialect.class;
			case "Sybase":
				return SybaseDialect.class;
			case "Sybase11":
				return Sybase11Dialect.class;
			case "SybaseASE":
				return SybaseASEDialect.class;
			case "SybaseASE15":
				return SybaseASE15Dialect.class;
			case "SybaseASE157":
				return SybaseASE157Dialect.class;
		}
		return null;
	}

}
