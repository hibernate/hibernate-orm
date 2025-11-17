/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;

import java.util.Objects;

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
			case "DB297":
			case "DB2":
				return DB2Dialect.class;
			case "DB2400V7R3":
			case "DB2400":
			case "DB2i":
				return DB2iDialect.class;
			case "DB2390":
			case "DB2390V8":
			case "DB2z":
				return DB2zDialect.class;
			case "H2":
				return H2Dialect.class;
			case "HANARowStore":
			case "HANAColumnStore":
			case "HANACloudColumnStore":
			case "HANA":
				return HANADialect.class;
			case "HSQL":
				return HSQLDialect.class;
			case "MariaDB53":
			case "MariaDB10":
			case "MariaDB102":
			case "MariaDB103":
			case "MariaDB106":
			case "MariaDB":
				return MariaDBDialect.class;
			case "MySQL5":
			case "MySQL55":
			case "MySQL57":
			case "MySQL8":
			case "MySQL":
				return MySQLDialect.class;
			case "Oracle8i":
			case "Oracle9i":
			case "Oracle10g":
			case "Oracle12c":
			case "Oracle":
				return OracleDialect.class;
			case "PostgresPlus":
				return PostgresPlusDialect.class;
			case "PostgreSQL81":
			case "PostgreSQL82":
			case "PostgreSQL9":
			case "PostgreSQL91":
			case "PostgreSQL92":
			case "PostgreSQL93":
			case "PostgreSQL94":
			case "PostgreSQL95":
			case "PostgreSQL10":
			case "PostgreSQL":
				return PostgreSQLDialect.class;
			case "Spanner":
				return SpannerDialect.class;
			case "SQLServer2005":
			case "SQLServer2008":
			case "SQLServer2012":
			case "SQLServer2016":
			case "SQLServer":
				return SQLServerDialect.class;
			case "Sybase":
			case "Sybase11":
				return SybaseDialect.class;
			case "SybaseASE15":
			case "SybaseASE157":
			case "SybaseASE":
				return SybaseASEDialect.class;
		}
		return null;
	}
}
