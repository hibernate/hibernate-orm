/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.contributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.dialect.cockroachdb.CockroachDbContributor;
import org.hibernate.spatial.dialect.h2gis.H2GisDialectContributor;
import org.hibernate.spatial.dialect.mariadb.MariaDBDialectContributor;
import org.hibernate.spatial.dialect.mysql.MySQLDialectContributor;
import org.hibernate.spatial.dialect.oracle.OracleDialectContributor;
import org.hibernate.spatial.dialect.postgis.PostgisDialectContributor;
import org.hibernate.spatial.dialect.sqlserver.SqlServerDialectContributor;

class ContributorResolver {

	private final static Map<Class<? extends Dialect>,
			Function<ServiceRegistry, ContributorImplementor>> CONTRIBUTOR_MAP = new HashMap<>();

	static {
		//TypeContributorImplementor
		CONTRIBUTOR_MAP.put( PostgreSQLDialect.class, PostgisDialectContributor::new );
		CONTRIBUTOR_MAP.put( CockroachDialect.class, CockroachDbContributor::new );
		CONTRIBUTOR_MAP.put( MariaDBDialect.class, MariaDBDialectContributor::new );
		CONTRIBUTOR_MAP.put( MySQLDialect.class, MySQLDialectContributor::new );
		CONTRIBUTOR_MAP.put( H2Dialect.class, H2GisDialectContributor::new );
		CONTRIBUTOR_MAP.put( OracleDialect.class, OracleDialectContributor::new );
		CONTRIBUTOR_MAP.put( SQLServerDialect.class, SqlServerDialectContributor::new );
	}

	private ContributorResolver() {
	}

	static ContributorImplementor resolveSpatialtypeContributorImplementor(ServiceRegistry serviceRegistry) {
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		Dialect dialect = jdbcServices.getDialect();
		Function<ServiceRegistry, ContributorImplementor> creator =
				CONTRIBUTOR_MAP.get( dialect.getClass() );
		if ( creator != null ) {
			creator.apply( serviceRegistry );
		}
		for ( Map.Entry<Class<? extends Dialect>, Function<ServiceRegistry, ContributorImplementor>> entry :
				CONTRIBUTOR_MAP.entrySet() ) {
			if ( entry.getKey().isAssignableFrom( dialect.getClass() ) ) {
				return entry.getValue().apply( serviceRegistry );
			}
		}
		return null;
	}

}
