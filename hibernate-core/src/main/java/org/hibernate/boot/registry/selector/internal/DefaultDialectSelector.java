/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.Objects;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.dialect.Cache71Dialect;
import org.hibernate.dialect.DB2390Dialect;
import org.hibernate.dialect.DB2390V8Dialect;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2400V7R3Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenFiveDialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.DerbyTenSixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FirebirdDialect;
import org.hibernate.dialect.FrontBaseDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANACloudColumnStoreDialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HANARowStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.Ingres10Dialect;
import org.hibernate.dialect.Ingres9Dialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.InterbaseDialect;
import org.hibernate.dialect.JDataStoreDialect;
import org.hibernate.dialect.MckoiDialect;
import org.hibernate.dialect.MimerSQLDialect;
import org.hibernate.dialect.MySQL57Dialect;
import org.hibernate.dialect.MySQL57InnoDBDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PointbaseDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.ProgressDialect;
import org.hibernate.dialect.SAPDBDialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE157Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.dialect.TimesTenDialect;

public class DefaultDialectSelector implements LazyServiceResolver<Dialect> {

	@Override
	public Class<? extends Dialect> resolve(final String name) {
		Objects.requireNonNull( name);
		if ( name.isEmpty() ) {
			return null;
		}
		//Let's organize all string matches in groups by first letter:
		final char n = name.charAt( 0 );
		switch ( n ) {
			case 'C': return caseC( name );
			case 'D': return caseD( name );
			case 'F': return caseF( name );
			case 'H': return caseH( name );
			case 'I': return caseI( name );
			case 'J': return caseJ( name );
			case 'M': return caseM( name );
			case 'O': return caseO( name );
			case 'P': return caseP( name );
			case 'S': return caseS( name );
			case 'T': return caseT( name );
		}
		return null;
	}

	private static Class<? extends Dialect> caseC(final String name) {
		if ( name.equals( "Cache71" ) ) {
			return Cache71Dialect.class;
		}
		if ( name.equals( "CUBRID" ) ) {
			return CUBRIDDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseD(final String name) {
		if ( name.equals( "DB2" ) ) {
			return DB2Dialect.class;
		}
		if ( name.equals( "DB2390" ) ) {
			return DB2390Dialect.class;
		}
		if ( name.equals( "DB2390V8" ) ) {
			return DB2390V8Dialect.class;
		}
		if ( name.equals( "DB2400" ) ) {
			return DB2400Dialect.class;
		}
		if ( name.equals( "DB2400V7R3" ) ) {
			return DB2400V7R3Dialect.class;
		}
		if ( name.equals( "DerbyTenFive" ) ) {
			return DerbyTenFiveDialect.class;
		}
		if ( name.equals( "DerbyTenSix" ) ) {
			return DerbyTenSixDialect.class;
		}
		if ( name.equals( "DerbyTenSeven" ) ) {
			return DerbyTenSevenDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseF(final String name) {
		if ( name.equals( "Firebird" ) ) {
			return FirebirdDialect.class;
		}
		if ( name.equals( "FrontBase" ) ) {
			return FrontBaseDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseH(final String name) {
		if ( name.equals( "H2" ) ) {
			return H2Dialect.class;
		}
		if ( name.equals( "HANACloudColumnStore" ) ) {
			return HANACloudColumnStoreDialect.class;
		}
		if ( name.equals( "HANAColumnStore" ) ) {
			return HANAColumnStoreDialect.class;
		}
		if ( name.equals( "HANARowStore" ) ) {
			return HANARowStoreDialect.class;
		}
		if ( name.equals( "HSQL" ) ) {
			return HSQLDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseI(final String name) {
		if ( name.equals( "Informix" ) ) {
			return InformixDialect.class;
		}
		if ( name.equals( "Ingres" ) ) {
			return IngresDialect.class;
		}
		if ( name.equals( "Ingres9" ) ) {
			return Ingres9Dialect.class;
		}
		if ( name.equals( "Ingres10" ) ) {
			return Ingres10Dialect.class;
		}
		if ( name.equals( "Interbase" ) ) {
			return InterbaseDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseJ(final String name) {
		if ( name.equals( "JDataStore" ) ) {
			return JDataStoreDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseM(final String name) {
		if ( name.equals( "Mckoi" ) ) {
			return MckoiDialect.class;
		}
		if ( name.equals( "MimerSQL" ) ) {
			return MimerSQLDialect.class;
		}
		if ( name.equals( "MySQL5" ) ) {
			return MySQL5Dialect.class;
		}
		if ( name.equals( "MySQL5InnoDB" ) ) {
			return MySQL5InnoDBDialect.class;
		}
		if ( name.equals( "MySQL57InnoDB" ) ) {
			return MySQL57InnoDBDialect.class;
		}
		if ( name.equals( "MySQL57" ) ) {
			return MySQL57Dialect.class;
		}
		if ( name.equals( "MySQL8" ) ) {
			return MySQL8Dialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseO(final String name) {
		if ( name.equals( "Oracle8i" ) ) {
			return Oracle8iDialect.class;
		}
		if ( name.equals( "Oracle9i" ) ) {
			return Oracle9iDialect.class;
		}
		if ( name.equals( "Oracle10g" ) ) {
			return Oracle10gDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseP(final String name) {
		if ( name.equals( "Pointbase" ) ) {
			return PointbaseDialect.class;
		}
		if ( name.equals( "PostgresPlus" ) ) {
			return PostgresPlusDialect.class;
		}
		if ( name.equals( "PostgreSQL81" ) ) {
			return PostgreSQL81Dialect.class;
		}
		if ( name.equals( "PostgreSQL82" ) ) {
			return PostgreSQL82Dialect.class;
		}
		if ( name.equals( "PostgreSQL9" ) ) {
			return PostgreSQL9Dialect.class;
		}
		if ( name.equals( "Progress" ) ) {
			return ProgressDialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseS(final String name) {
		if ( name.equals( "SAPDB" ) ) {
			return SAPDBDialect.class;
		}
		if ( name.equals( "SQLServer" ) ) {
			return SQLServerDialect.class;
		}
		if ( name.equals( "SQLServer2005" ) ) {
			return SQLServer2005Dialect.class;
		}
		if ( name.equals( "SQLServer2008" ) ) {
			return SQLServer2008Dialect.class;
		}
		if ( name.equals( "Sybase11" ) ) {
			return Sybase11Dialect.class;
		}
		if ( name.equals( "SybaseAnywhere" ) ) {
			return SybaseAnywhereDialect.class;
		}
		if ( name.equals( "Sybase11" ) ) {
			return Sybase11Dialect.class;
		}
		if ( name.equals( "SybaseAnywhere" ) ) {
			return SybaseAnywhereDialect.class;
		}
		if ( name.equals( "SybaseASE15" ) ) {
			return SybaseASE15Dialect.class;
		}
		if ( name.equals( "SybaseASE157" ) ) {
			return SybaseASE157Dialect.class;
		}
		return null;
	}

	private static Class<? extends Dialect> caseT(final String name) {
		if ( name.equals( "Teradata" ) ) {
			return TeradataDialect.class;
		}
		if ( name.equals( "TimesTen" ) ) {
			return TimesTenDialect.class;
		}
		return null;
	}

}
