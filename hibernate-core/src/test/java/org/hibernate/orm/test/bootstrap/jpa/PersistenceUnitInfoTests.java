/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.jpa;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DataSourceConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.testing.jdbc.DataSourceStub;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class PersistenceUnitInfoTests {

	@Test
	@JiraKey(value = "HHH-13432")
	public void testNonJtaDataExposedAsProperty() {
		final DataSource puDataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getNonJtaDataSource() {
				return puDataSource;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		try (EntityManagerFactory emf = provider.createContainerEntityManagerFactory( info, Collections.emptyMap() )) {
			// first let's check the DataSource used in the EMF...
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DataSourceConnectionProvider.class );
			final DataSourceConnectionProvider dsCp = (DataSourceConnectionProvider) connectionProvider;
			assertThat( dsCp ).isNotNull();
			assertThat( dsCp.getDataSource() ).isEqualTo( puDataSource );

			// now let's check that it is exposed via the EMF properties
			//		- note : the spec does not indicate that this should work, but
			//			it worked this way in previous versions
			final Object o = emf.getProperties().get( AvailableSettings.JAKARTA_NON_JTA_DATASOURCE );
			assertThat( o ).isEqualTo( puDataSource );
		}
	}

	@Test
	@JiraKey(value = "HHH-13432")
	public void testJtaDataExposedAsProperty() {
		final DataSource puDataSource = new DataSourceStub( "puDataSource" );
		final PersistenceUnitInfoAdapter info = new PersistenceUnitInfoAdapter() {

			@Override
			public DataSource getJtaDataSource() {
				return puDataSource;
			}
		};

		final PersistenceProvider provider = new HibernatePersistenceProvider();

		try (EntityManagerFactory emf = provider.createContainerEntityManagerFactory( info, Collections.emptyMap() )) {

			// first let's check the DataSource used in the EMF...
			final ConnectionProvider connectionProvider = emf.unwrap( SessionFactoryImplementor.class )
					.getServiceRegistry()
					.getService( ConnectionProvider.class );
			assertThat( connectionProvider ).isInstanceOf( DataSourceConnectionProvider.class );
			final DataSourceConnectionProvider dsCp = (DataSourceConnectionProvider) connectionProvider;
			assertThat( dsCp ).isNotNull();
			assertThat( dsCp.getDataSource() ).isEqualTo( puDataSource );

			// now let's check that it is exposed via the EMF properties
			//		- again, the spec does not indicate that this should work, but
			//			it worked this way in previous versions
			final Map<String, Object> properties = emf.getProperties();
			final Object o = properties.get( AvailableSettings.JAKARTA_JTA_DATASOURCE );
			assertThat( o ).isEqualTo( puDataSource );
		}
	}
}
