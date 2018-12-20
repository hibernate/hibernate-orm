/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Used to define the ServiceRegistry to be used for testing.  Can be used alone:
 *
 * [source, JAVA, indent=0]
 * ----
 * @ServiceRegistry( ... )
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
 * or {@link org.hibernate.engine.spi.SessionFactoryImplementor} via {@link SessionFactory},
 * with or without {@link ServiceRegistryScopeAware}.  E.g.
 *
 * [source, JAVA, indent=0]
 * ----
 * @ServiceRegistry( ... )
 * @TestDomain( ... )
 * class MyTest ... {
 * }
 * ----
 *
 * Here, the managed ServiceRegistry is used to create the
 * {@link org.hibernate.boot.spi.MetadataImplementor} via
 * {@link org.hibernate.boot.MetadataSources#MetadataSources(org.hibernate.service.ServiceRegistry)}
 *
 * @see ServiceRegistryScopeAware
 *
 * @author Steve Ebersole
 */
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )

@ServiceRegistryFunctionalTesting
//@TestInstance( TestInstance.Lifecycle.PER_CLASS )
//
//@ExtendWith( FailureExpectedExtension.class )
//@ExtendWith( ServiceRegistryExtension.class )
//@ExtendWith( ServiceRegistryParameterResolver.class )
public @interface ServiceRegistry {
	Class<? extends ServiceContributor>[] serviceContributors() default {};

	Class<? extends StandardServiceInitiator>[] initiators() default {};

	Service[] services() default {};

	Setting[] settings() default {};

	@interface Service {
		Class<? extends org.hibernate.service.Service> role();
		Class<? extends org.hibernate.service.Service> impl();
	}

	@interface Setting {
		String name();
		String value();
	}
}
