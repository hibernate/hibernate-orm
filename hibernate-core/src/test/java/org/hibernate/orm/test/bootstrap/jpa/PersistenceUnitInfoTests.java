/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap.jpa;

import java.util.Collections;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.testing.jdbc.DataSourceStub;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoTests extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-13432" )
	public void testNonJtaDataExposedAsProperty() {
		final DataSource puDataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return puDataSource;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				Collections.emptyMap()
		);

		// first let's check the DataSource used in the EMF...
		final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry()
				.getService( ConnectionProvider.class );
		assertThat( connectionProvider, instanceOf( DatasourceConnectionProviderImpl.class ) );
		final DatasourceConnectionProviderImpl dsCp = (DatasourceConnectionProviderImpl) connectionProvider;
		assertThat( dsCp.getDataSource(), is( puDataSource ) );

		// now let's check that it is exposed via the EMF properties
		//		- note : the spec does not indicate that this should work, but
		//			it worked this way in previous versions
		final Object o = emf.getProperties().get( AvailableSettings.JPA_NON_JTA_DATASOURCE );
		assertThat( o, is( puDataSource ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13432" )
	public void testJtaDataExposedAsProperty() {
		final DataSource puDataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getJtaDataSource() {
				return puDataSource;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		final EntityManagerFactory emf = provider.createContainerEntityManagerFactory(
				info,
				Collections.emptyMap()
		);

		// first let's check the DataSource used in the EMF...
		final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry()
				.getService( ConnectionProvider.class );
		assertThat( connectionProvider, instanceOf( DatasourceConnectionProviderImpl.class ) );
		final DatasourceConnectionProviderImpl dsCp = (DatasourceConnectionProviderImpl) connectionProvider;
		assertThat( dsCp.getDataSource(), is( puDataSource ) );

		// now let's check that it is exposed via the EMF properties
		//		- again, the spec does not indicate that this should work, but
		//			it worked this way in previous versions
		final Map<String, Object> properties = emf.getProperties();
		final Object o = properties.get( AvailableSettings.JPA_JTA_DATASOURCE );
		assertEquals( puDataSource, o );
	}
}
