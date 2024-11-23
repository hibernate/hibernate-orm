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

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.cache.spi.access.AccessType;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.SharedCacheMode;

/**
 * @asciidoc
 *
 * Used to define the test model ({@link org.hibernate.boot.spi.MetadataImplementor})
 * to be used for testing.
 *
 * Can be used by itself, along with {@link DomainModelScopeAware}, to test the MetadataImplementor.  E.g.
 *
 * [source, JAVA, indent=0]
 * ----
 * @TestDomain ( ... )
 * class MyTest implements TestDomainAware {
 *
 * 		@Test
 * 		public void doTheTest() {
 * 		    // use the injected MetadataImplementor
 * 		}
 *
 * 		private MetadataImplementor model;
 *
 * 		@Override
 * 		public void injectTestModelScope(MetadataImplementor model) {
 * 		 	this.model = model;
 * 		}
 * }
 * ----
 *
 *
 * Can optionally be used with {@link ServiceRegistry} to define the ServiceRegistry used to
 * build the MetadataImplementor (passed to
 * {@link org.hibernate.boot.MetadataSources#MetadataSources(org.hibernate.service.ServiceRegistry)}).
 *
 * [source, JAVA, indent=0]
 * ----
 * @ServiceRegistry ( ... )
 * @TestDomain ( ... )
 * class MyTest implements TestDomainAware {
 *
 * 		@Test
 * 		public void doTheTest() {
 * 		    // use the injected MetadataImplementor
 * 		}
 *
 * 		private MetadataImplementor model;
 *
 * 		@Override
 * 		public void injectTestModelScope(MetadataImplementor model) {
 * 		 	this.model = model;
 * 		}
 * }
 * ----
 *
 * It can also be used in conjunction with {@link SessionFactory}
 *
 * @see DomainModelScopeAware
 *
 * @author Steve Ebersole
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

@TestInstance( TestInstance.Lifecycle.PER_CLASS )

@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ServiceRegistryExtension.class )
@ExtendWith( ServiceRegistryParameterResolver.class )

@ExtendWith( DomainModelExtension.class )
@ExtendWith( DomainModelParameterResolver.class )
public @interface DomainModel {
	StandardDomainModel[] standardModels() default {};
	Class<? extends DomainModelDescriptor>[] modelDescriptorClasses() default {};
	Class[] annotatedClasses() default {};
	String[] annotatedClassNames() default {};
	String[] annotatedPackageNames() default {};
	String[] xmlMappings() default {};

	SharedCacheMode sharedCacheMode() default SharedCacheMode.ENABLE_SELECTIVE;

	boolean overrideCacheStrategy() default true;
	String concurrencyStrategy() default "";

	AccessType accessType() default AccessType.READ_WRITE;

	Class<? extends TypeContributor>[] typeContributors() default {};
}
