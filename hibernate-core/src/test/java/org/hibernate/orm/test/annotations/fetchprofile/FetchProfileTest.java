/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test case for HHH-4812
 *
 * @author Hardy Ferentschik
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-4812" )
public class FetchProfileTest {
	@Test
	@DomainModel(annotatedClasses = {Customer.class, Order.class, SupportTickets.class, Country.class})
	@SessionFactory
	public void testFetchProfileConfigured(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		assertThat( sessionFactory.containsFetchProfileDefinition( "customer-with-orders" ) ).isTrue();
		assertThat( sessionFactory.containsFetchProfileDefinition( "package-profile-1" ) ).isFalse();
	}

	@Test
	@ServiceRegistry
	public void testWrongAssociationName(ServiceRegistryScope registryScope) {
		try {
			MetadataBuildingTestHelper.buildMetadata(
					registryScope.getRegistry(),
					Customer2.class,
					Order.class,
					Country.class
			);
			fail( "Expecting an exception, but none thrown" );
		}
		catch (MappingException expected) {
		}
	}

	@Test
	@ServiceRegistry
	public void testWrongClass(ServiceRegistryScope registryScope) {
		try {
			MetadataBuildingTestHelper.buildMetadata(
					registryScope.getRegistry(),
					Customer2.class,
					Order.class,
					Country.class
			);
			fail( "Expecting an exception, but none thrown" );
		}
		catch (MappingException expected) {
		}
	}

	@Test
	@DomainModel(annotatedClasses = {Customer4.class, Order.class, Country.class})
	@SessionFactory
	public void testNowSupportedFetchMode(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		assertThat( sessionFactory.containsFetchProfileDefinition( "unsupported-fetch-mode" ) ).isTrue();
	}

	@Test
	@ServiceRegistry
	@Jira( "https://hibernate.atlassian.net/browse/HHH-19417" )
	public void testXmlOverride(ServiceRegistryScope registryScope) {
		final Metadata metadata = MetadataBuildingTestHelper.buildMetadata(
				registryScope.getRegistry(),
				new MappingSources()
						.addManagedClasses( Customer5.class, Order.class, Country.class )
						.addMappingResource( "org/hibernate/orm/test/annotations/fetchprofile/mappings.xml" )
		);
		try ( SessionFactoryImplementor sessionFactory =
					(SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata ) ) {
			assertThat( sessionFactory.containsFetchProfileDefinition( "orders-profile" ) ).isTrue();
		}
	}

	@Test
	@ServiceRegistry
	public void testMissingXmlOverride(ServiceRegistryScope registryScope) {
		try {
			MetadataBuildingTestHelper.buildMetadata(
					registryScope.getRegistry(),
					Customer5.class,
					Order.class,
					Country.class
			);
			fail( "Expecting an exception, but none thrown" );
		}
		catch (MappingException expected) {
		}
	}

	@Test
	@DomainModel(
			annotatedClasses = {Customer.class, Order.class, SupportTickets.class, Country.class},
			annotatedPackageNames = "org.hibernate.orm.test.annotations.fetchprofile"
	)
	@SessionFactory
	public void testPackageConfiguredFetchProfile(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		assertThat( sessionFactory.containsFetchProfileDefinition( "package-profile-1" ) ).isTrue();
		assertThat( sessionFactory.containsFetchProfileDefinition( "package-profile-2" ) ).isTrue();
	}
}
