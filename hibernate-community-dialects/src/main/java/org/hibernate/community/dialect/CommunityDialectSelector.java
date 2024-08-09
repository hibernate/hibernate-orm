/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.util.Objects;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;

public class CommunityDialectSelector implements DialectSelector {

	@Override
	public Class<? extends Dialect> resolve(String name) {
		Objects.requireNonNull( name );
		if ( name.isEmpty() ) {
			return null;
		}
		switch ( name ) {
			case "DB297":
				return DB297Dialect.class;
			case "DB2390":
				return DB2390Dialect.class;
			case "DB2390V8":
				return DB2390V8Dialect.class;
			case "Cache":
				return CacheDialect.class;
			case "Cache71":
				return Cache71Dialect.class;
			case "CUBRID":
				return CUBRIDDialect.class;
			case "Altibase":
				return AltibaseDialect.class;
			case "DerbyTenFive":
				return DerbyTenFiveDialect.class;
			case "DerbyTenSix":
				return DerbyTenSixDialect.class;
			case "DerbyTenSeven":
				return DerbyTenSevenDialect.class;
			case "Firebird":
				return FirebirdDialect.class;
			case "Informix":
				return InformixDialect.class;
			case "Informix10":
				return Informix10Dialect.class;
			case "Ingres":
				return IngresDialect.class;
			case "Ingres9":
				return Ingres9Dialect.class;
			case "Ingres10":
				return Ingres10Dialect.class;
			case "MariaDB53":
				return MariaDB53Dialect.class;
			case "MariaDB10":
				return MariaDB10Dialect.class;
			case "MariaDB102":
				return MariaDB102Dialect.class;
			case "MariaDB103":
				return MariaDB103Dialect.class;
			case "MimerSQL":
				return MimerSQLDialect.class;
			case "MySQL5":
				return MySQL5Dialect.class;
			case "MySQL55":
				return MySQL55Dialect.class;
			case "MySQL57":
				return MySQL57Dialect.class;
			case "Oracle8i":
				return Oracle8iDialect.class;
			case "Oracle9i":
				return Oracle9iDialect.class;
			case "Oracle10g":
				return Oracle10gDialect.class;
			case "Oracle12c":
				return Oracle12cDialect.class;
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
			case "PostgreSQL10":
				return PostgreSQL10Dialect.class;
			case "RDMSOS2200":
				return RDMSOS2200Dialect.class;
			case "SAPDB":
				return SAPDBDialect.class;
			case "SQLServer2005":
				return SQLServer2005Dialect.class;
			case "SQLServer2008":
				return SQLServer2008Dialect.class;
			case "MaxDB":
				return MaxDBDialect.class;
			case "Sybase11":
				return Sybase11Dialect.class;
			case "SybaseAnywhere":
				return SybaseAnywhereDialect.class;
			case "SybaseASE15":
				return SybaseASE15Dialect.class;
			case "SybaseASE157":
				return SybaseASE157Dialect.class;
			case "Teradata":
				return TeradataDialect.class;
			case "Teradata14":
				return Teradata14Dialect.class;
			case "TimesTen":
				return TimesTenDialect.class;
			case "SingleStore":
				return SingleStoreDialect.class;
		}
		return null;
	}

}
