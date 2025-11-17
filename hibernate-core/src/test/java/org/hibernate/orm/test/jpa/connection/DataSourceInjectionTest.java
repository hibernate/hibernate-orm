/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.connection;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import jakarta.persistence.PersistenceException;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.orm.test.jpa.Distributor;
import org.hibernate.orm.test.jpa.Item;
import org.hibernate.orm.test.jpa.xml.Light;
import org.hibernate.orm.test.jpa.xml.Lighter;

import org.hibernate.testing.util.jpa.PersistenceUnitInfoAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
public class DataSourceInjectionTest {
	@Test
	public void testDatasourceInjection() throws Exception {
		withPuRoot(
				puRootUrl -> {
					final PersistenceUnitInfoAdapter persistenceUnitInfo = createPuDescriptor( puRootUrl, new FakeDataSource() );

					// otherwise the FakeDataSourceException will be eaten trying to resolve the Dialect
					final Map<String, Object> intgOverrides = Collections.singletonMap(
							AvailableSettings.DIALECT,
							HSQLDialect.class
					);

					final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();
					try ( final SessionFactoryImplementor sf = provider.createContainerEntityManagerFactory(
							persistenceUnitInfo,
							intgOverrides
					).unwrap( SessionFactoryImplementor.class ) ) {

						try ( final SessionImplementor session = sf.openSession().unwrap( SessionImplementor.class ) ) {
							session.createQuery( "select i from Item i" ).list();
							Assertions.fail( "Expecting FakeDataSourceException" );
						}
						catch (PersistenceException pe) {
							try {
								throw (RuntimeException) pe.getCause();
							}
							catch (FakeDataSourceException fde) {
								//success
							}
						}
						catch (FakeDataSourceException fde) {
							//success
						}
					}
				}
		);
	}

	protected PersistenceUnitInfoAdapter createPuDescriptor(URL puRootUrl, DataSource dataSource) {
		return new PersistenceUnitInfoAdapter() {
			@Override
			public DataSource getNonJtaDataSource() {
				return dataSource;
			}

			@Override
			public URL getPersistenceUnitRootUrl() {
				return puRootUrl;
			}

			public List<String> getManagedClassNames() {
				List<String> classes = new ArrayList<>();
				classes.add( Item.class.getName() );
				classes.add( Distributor.class.getName() );
				classes.add( Light.class.getName() );
				classes.add( Lighter.class.getName() );
				return classes;
			}
		};
	}

	private void withPuRoot(Consumer<URL> puRootUrlConsumer) throws Exception {
		// create a temporary directory to serve as the "PU root URL"
		final Path puroot = Files.createTempDirectory( "puroot" );
		final URL puRootUrl = puroot.toUri().toURL();

		try {
			puRootUrlConsumer.accept( puRootUrl );
		}
		finally {
			Files.deleteIfExists( puroot );
		}
	}
}
