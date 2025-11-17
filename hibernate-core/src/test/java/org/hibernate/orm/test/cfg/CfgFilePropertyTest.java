/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.persistence.Persistence.createEntityManagerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.internal.util.ConfigHelper.findAsResource;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13227")
@BaseUnitTest
public class CfgFilePropertyTest {

	@Test
	public void test() throws Exception {

		final AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

		Thread thread = new Thread( () -> {
			final Properties props = new Properties();
			props.setProperty( AvailableSettings.CFG_XML_FILE,
					"/org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" );
			ServiceRegistryUtil.applySettings( props );
			try (EntityManagerFactory emF = createEntityManagerFactory( "ExcludeUnlistedClassesTest1", props )) {
			}
			catch (Exception e) {
				exceptionHolder.set( e );
			}
		} );
		thread.setContextClassLoader( new ClassLoader() {

			@Override
			protected Enumeration<URL> findResources(String name) throws IOException {
				return name.equals( "META-INF/persistence.xml" ) ?
						Collections.enumeration(
								Collections.singletonList(
										findAsResource(
												"org/hibernate/jpa/test/persistenceunit/META-INF/persistence.xml" )
								)
						) :
						Collections.emptyEnumeration();
			}
		} );

		thread.start();
		thread.join();

		assertThat( exceptionHolder.get() ).isNull();
	}
}
