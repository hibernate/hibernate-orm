/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.events.nocdi;

import org.hibernate.InstantiationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.cdi.events.Monitor;
import org.hibernate.orm.test.cdi.events.TheEntity;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Attempt to use CDI injection when no CDI is available
 *
 * @author Steve Ebersole
 */
public class InvalidNoCdiSupportTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		Monitor.reset();

		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build();

		final SessionFactoryImplementor sessionFactory;

		try {
			// because there is no CDI available, building the SF should immediately
			// try to build the ManagedBeans which should fail here
			sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
			sessionFactory.close();
			fail( "Expecting failure" );
		}
		catch ( Exception e ) {
			StandardServiceRegistryBuilder.destroy( ssr );
			assertThat( e, instanceOf( InstantiationException.class ) );
		}
	}
}
