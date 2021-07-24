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

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;

class ContributorResolver {

	private final static Map<Class<? extends Dialect>,
			Function<ServiceRegistry, ContributorImplementor>> ContributorMap = new HashMap<>();


	static {
		//TypeContributorImplementor
		ContributorMap.put( PostgreSQLDialect.class, PostgreSQLDialectContributor::new );
	}

	private ContributorResolver() {
	}

	static ContributorImplementor resolveSpatialtypeContributorImplementor(ServiceRegistry serviceRegistry) {
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		Dialect dialect = jdbcServices.getDialect();
		for ( Class<?> dialectClass : ContributorMap.keySet() ) {
			if ( dialectClass.isAssignableFrom( dialect.getClass() ) ) {
				return ContributorMap.get( dialectClass ).apply( serviceRegistry );
			}
		}
		return null;
	}

}
