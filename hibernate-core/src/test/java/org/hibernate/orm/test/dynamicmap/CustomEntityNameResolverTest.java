/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dynamicmap;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.EntityNameResolver;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.hibernate.tool.schema.Action.ACTION_CREATE_THEN_DROP;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18486" )
public class CustomEntityNameResolverTest {
	@Test
	public void test() {
		final Configuration configuration = new Configuration();
		configuration.setProperty( AvailableSettings.HBM2DDL_AUTO, ACTION_CREATE_THEN_DROP );
		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		configuration.addResource( "org/hibernate/orm/test/dynamicmap/artist.hbm.xml" );
		configuration.addEntityNameResolver( new HibernateEntityNameResolver() );
		try (final SessionFactoryImplementor sf = (SessionFactoryImplementor) configuration.buildSessionFactory()) {
			inTransaction( sf, session -> {
				final Map<String, Object> artistEntity = new HashMap<>();
				artistEntity.put( "id", 1 );
				artistEntity.put( "firstname", "John" );
				artistEntity.put( "lastname", "Doe" );
				// Persist the dynamic map entity
				session.persist( artistEntity );
			} );
			sf.getSchemaManager().truncateMappedObjects();
		}
	}

	static class HibernateEntityNameResolver implements EntityNameResolver {
		@Override
		public String resolveEntityName(Object entity) {
			if ( entity instanceof Map ) {
				return "artist";
			}
			return null;
		}
	}
}
