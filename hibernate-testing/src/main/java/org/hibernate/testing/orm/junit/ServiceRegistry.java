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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceContributor;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @asciidoc
 *
 * Used to define the ServiceRegistry to be used for testing.  Can be used alone:
 *
 * [source, java, indent=0]
 * ----
 * @ServiceRegistry ( ... )
 * class MyTest extends ServiceRegistryAware {
 * 		@Test
 * 		public void doTheTest() {
 *		    // use the injected registry...
 *
 *		    ...
 * 		}
 *
 * 		private StandardServiceRegistry registry;
 *
 * 		@Override
 * 		public void injectServiceRegistryScope(StandardServiceRegistry registry) {
 * 			this.registry = registry;
 * 		}
 * }
 * ----
 *
 * It can also be used as the basis for building a
 * {@link org.hibernate.boot.spi.MetadataImplementor} via {@link DomainModel}
 * or {@link SessionFactoryImplementor} via {@link SessionFactory},
 * with or without {@link ServiceRegistryScopeAware}.  E.g.
 *
 * [source, java, indent=0]
 * ----
 * @ServiceRegistry ( ... )
 * @TestDomain ( ... )
 * class MyTest ... {
 * }
 * ----
 *
 * Here, the managed ServiceRegistry is used to create the
 * {@link org.hibernate.boot.spi.MetadataImplementor}
 *
 * @see ServiceRegistryScopeAware
 *
 * @author Steve Ebersole
 */
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
