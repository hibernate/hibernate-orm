/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import java.util.Collections;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "12097")
public class ClosedFactoryTests extends BaseUnitTestCase {
	@Test
	public void testClosedChecks() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.JPA_CLOSED_COMPLIANCE, "true" )
				.build();

		try {
			final SessionFactoryBuilderImplementor factoryBuilder = (SessionFactoryBuilderImplementor) new MetadataSources( ssr )
					.buildMetadata()
					.getSessionFactoryBuilder();
			final SessionFactory sf = factoryBuilder.build();

			sf.close();

			assertTrue( sf.isClosed() );

			// we now have a closed SF (EMF)... test the closed checks in various methods

			try {
				sf.getCache();
				fail( "#getCache did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#getCache failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.getMetamodel();
				fail( "#getMetamodel did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#getMetamodel failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.getCriteriaBuilder();
				fail( "#getCriteriaBuilder did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#getCriteriaBuilder failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.getProperties();
				fail( "#getProperties did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#getProperties failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.getPersistenceUnitUtil();
				fail( "#getPersistenceUnitUtil did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#getPersistenceUnitUtil failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.close();
				fail( "#close did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#close failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.createEntityManager();
				fail( "#createEntityManager did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#createEntityManager failed, but not with the expected IllegalStateException : " + e.toString() );
			}

			try {
				sf.createEntityManager( Collections.emptyMap() );
				fail( "#createEntityManager(Map) did not fail" );
			}
			catch (IllegalStateException expected) {
				// this is the expected outcome
			}
			catch (Exception e) {
				fail( "#createEntityManager(Map) failed, but not with the expected IllegalStateException : " + e.toString() );
			}
		}
		catch (Exception e) {
			// if an exception is
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
