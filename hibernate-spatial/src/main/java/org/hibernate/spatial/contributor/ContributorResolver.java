/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.contributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.dialect.cockroachdb.CockroachDbContributor;
import org.hibernate.spatial.dialect.postgis.PostgisDialectContributor;

class ContributorResolver {

	private final static Map<Class<? extends Dialect>,
			Function<ServiceRegistry, ContributorImplementor>> CONTRIBUTOR_MAP = new HashMap<>();


	static {
		//TypeContributorImplementor
		CONTRIBUTOR_MAP.put( PostgreSQLDialect.class, PostgisDialectContributor::new );
		CONTRIBUTOR_MAP.put( CockroachDialect.class, CockroachDbContributor::new );
	}

	private ContributorResolver() {
	}

	static ContributorImplementor resolveSpatialtypeContributorImplementor(ServiceRegistry serviceRegistry) {
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		Dialect dialect = jdbcServices.getDialect();
		for ( Class<?> dialectClass : CONTRIBUTOR_MAP.keySet() ) {
			if ( dialectClass.isAssignableFrom( dialect.getClass() ) ) {
				return CONTRIBUTOR_MAP.get( dialectClass ).apply( serviceRegistry );
			}
		}
		return null;
	}

}
