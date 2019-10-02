/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bootstrap;

import java.util.Collections;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitInfoTests extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-13432" )
	@FailureExpected( jiraKey = "HHH-13432" )
	public void testJtaDataExposedAsProperty() {
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

		final Map<String, Object> properties = emf.getProperties();
		final Object o = properties.get( AvailableSettings.JPA_JTA_DATASOURCE );
		assertEquals( o, puDataSource );
	}
}
