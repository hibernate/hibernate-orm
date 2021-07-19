/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;

class TypeContributorResolver {

	private TypeContributorResolver() {
	}

	static SpatialTypeContributorImplementor resolve(ServiceRegistry serviceRegistry) {
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		Dialect dialect = jdbcServices.getDialect();
		if ( dialect.getClass().isAssignableFrom( PostgreSQLDialect.class ) ) {
			return new PostgreSQLDialectTypeContributor( serviceRegistry );
		}
		return null;
	}

}
