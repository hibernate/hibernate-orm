/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.annotations.fetchprofile.mappedby.Address;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey(value = "HHH-14071")
@BaseUnitTest
public class MappedByFetchProfileUnitTest {

	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testFetchProfileConfigured() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer6.class );
		config.addAnnotatedClass( Address.class );
		try (SessionFactoryImplementor sessionImpl = (SessionFactoryImplementor) config
				.buildSessionFactory( serviceRegistry )) {

			assertThat( sessionImpl.containsFetchProfileDefinition( "address-with-customer" ) )
					.describedAs( "fetch profile not parsed properly" )
					.isTrue();
			assertThat( sessionImpl.containsFetchProfileDefinition( "customer-with-address" ) )
					.describedAs( "fetch profile not parsed properly" )
					.isTrue();
		}
	}

	@Test
	public void testPackageConfiguredFetchProfile() {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Customer6.class );
		config.addAnnotatedClass( Address.class );
		config.addPackage( Address.class.getPackage().getName() );
		try (SessionFactoryImplementor sessionImpl = (SessionFactoryImplementor) config
				.buildSessionFactory( serviceRegistry )) {

			assertThat( sessionImpl.containsFetchProfileDefinition( "mappedBy-package-profile-1" ) )
					.describedAs( "fetch profile not parsed properly" )
					.isTrue();
			assertThat( sessionImpl.containsFetchProfileDefinition( "mappedBy-package-profile-2" ) )
					.describedAs( "fetch profile not parsed properly" )
					.isTrue();
		}
	}

}
