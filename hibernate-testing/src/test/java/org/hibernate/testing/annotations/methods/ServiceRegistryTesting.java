/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceContributor;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(javaServices = @BootstrapServiceRegistry.JavaService(
		role = ServiceContributor.class,
		impl = ServiceRegistryTesting.ServiceContributor1.class
))
@ServiceRegistry( settings = @Setting( name = "setting", value = "class-level"))
public class ServiceRegistryTesting {

	@Test
	public void testClassLevel(ServiceRegistryScope scope) {
		scope.withService( ConfigurationService.class, (configurationService) -> {
			assertThat( configurationService.getSettings().get( "setting" ) ).isEqualTo( "class-level" );
			assertThat( configurationService.getSettings().get( "contributed" ) ).isEqualTo( "contributed-1" );
		} );
	}

	@Test
	@BootstrapServiceRegistry(javaServices = @BootstrapServiceRegistry.JavaService(
			role = ServiceContributor.class,
			impl = ServiceRegistryTesting.ServiceContributor2.class
	))
	@ServiceRegistry( settings = @Setting( name = "setting", value = "method-level"))
	public void testMethodLevel(ServiceRegistryScope scope) {
		scope.withService( ConfigurationService.class, (configurationService) -> {
			assertThat( configurationService.getSettings().get( "setting" ) ).isEqualTo( "method-level" );
			assertThat( configurationService.getSettings().get( "contributed" ) ).isEqualTo( "contributed-2" );
		} );
	}

	@Test
	@ServiceRegistry( settings = @Setting( name = "setting", value = "mixed"))
	public void testMethodLevelImplicitBootstrap(ServiceRegistryScope scope) {
		scope.withService( ConfigurationService.class, (configurationService) -> {
			assertThat( configurationService.getSettings().get( "setting" ) ).isEqualTo( "mixed" );
			assertThat( configurationService.getSettings().get( "contributed" ) ).isEqualTo( "contributed-1" );
		} );

	}

	@Test
	@BootstrapServiceRegistry(javaServices = @BootstrapServiceRegistry.JavaService(
			role = ServiceContributor.class,
			impl = ServiceRegistryTesting.ServiceContributor2.class
	))
	public void testMethodLevelImplicitStandard(ServiceRegistryScope scope) {
		scope.withService( ConfigurationService.class, (configurationService) -> {
			assertThat( configurationService.getSettings().get( "setting" ) ).isEqualTo( "class-level" );
			assertThat( configurationService.getSettings().get( "contributed" ) ).isEqualTo( "contributed-2" );
		} );
	}

	public static class ServiceContributor1 implements ServiceContributor {
		@Override
		public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
			serviceRegistryBuilder.getSettings().put( "contributed", "contributed-1" );
		}
	}

	public static class ServiceContributor2 implements ServiceContributor {
		@Override
		public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
			serviceRegistryBuilder.getSettings().put( "contributed", "contributed-2" );
		}
	}
}
