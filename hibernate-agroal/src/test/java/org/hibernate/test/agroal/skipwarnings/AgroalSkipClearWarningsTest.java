/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.agroal.skipwarnings;

import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.agroal.util.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * Testing the impact of org.hibernate.agroal.internal.AgroalConnectionProvider#connectionWarningsResetCanBeSkippedOnClose()
 * returning true.
 *
 * @author Sanne Grinovero
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class AgroalSkipClearWarningsTest extends BaseCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(true );

	@Override
	protected void configure(Configuration configuration) {
		Properties properties = configuration.getProperties();
		//Disable logging of JDBC warnings, as that has an impact:
		properties.put( AvailableSettings.LOG_JDBC_WARNINGS, false );
		properties.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ City.class };
	}

	@Test
	public void test() {
		connectionProvider.clear();
		doInHibernate( this::sessionFactory, session -> {
			City city = new City();
			city.setId( 1L );
			city.setName( "London" );
			session.persist( city );
			assertEquals( 0, connectionProvider.getCountInvocationsToClearWarnings() );
		} );
		assertEquals( 0, connectionProvider.getCountInvocationsToClearWarnings() );
		doInHibernate( this::sessionFactory, session -> {
			City city = session.find( City.class, 1L );
			assertEquals( "London", city.getName() );
		} );
		assertEquals( 0, connectionProvider.getCountInvocationsToClearWarnings() );
	}

	@Entity(name = "City" )
	public static class City {

		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
