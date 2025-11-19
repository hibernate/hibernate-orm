/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;
import org.junit.jupiter.api.extension.ExtensionContext;

/// Used to define the [org.hibernate.service.ServiceRegistry] to be used for testing.
/// Produces a [ServiceRegistryScope] which can be injected via [JUnit ParameterResolver][ServiceRegistryParameterResolver]
/// or via [ServiceRegistryScopeAware]; the ParameterResolver should be preferred.
///
/// ```java
/// @ServiceRegistry(settings=@Setting(name=LOG_SQL, value="true"))
/// class SomeTest {
///     @Test
///     void testStuff(ServiceRegistryScope registryScope) {
///         ...
///     }
/// }
/// ```
///
/// @see ServiceRegistryExtension
///
/// @author Steve Ebersole
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME )

@ServiceRegistryFunctionalTesting
public @interface ServiceRegistry {
	Class<? extends ServiceContributor>[] serviceContributors() default {};

	Class<? extends StandardServiceInitiator>[] initiators() default {};

	Service[] services() default {};
	JavaService[] javaServices() default {};

	Setting[] settings() default {};

	SettingProvider[] settingProviders() default {};

	SettingConfiguration[] settingConfigurations() default {};

	ResolvableSetting[] resolvableSettings() default {};

	/**
	 * A Hibernate Service registration
	 */
	@interface Service {
		Class<? extends org.hibernate.service.Service> role();
		Class<? extends org.hibernate.service.Service> impl();
	}

	/**
	 * A Java service loadable via {@link java.util.ServiceLoader}
	 */
	@interface JavaService {
		Class<?> role();
		Class<?>[] impls();
	}

	@interface ResolvableSetting {
		String settingName();
		Class<? extends SettingResolver> resolver();
	}

	interface SettingResolver {
		Object resolve(StandardServiceRegistryBuilder registryBuilder, ExtensionContext junitContext);
	}

}
