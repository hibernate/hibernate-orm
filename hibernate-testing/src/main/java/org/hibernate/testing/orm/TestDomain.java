/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.testing.junit5.FailureExpectedExtension;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @asciidoc
 *
 * Used to define the test model ({@link org.hibernate.boot.spi.MetadataImplementor})
 * to be used for testing.
 *
 * Can be used by itself, along with {@link TestDomainAware}, to test the MetadataImplementor.  E.g.
 *
 * [source, JAVA, indent=0]
 * ----
 * @TestDomain( ... )
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
 * 		public void injectTestModel(MetadataImplementor model) {
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
 * @ServiceRegistry( ... )
 * @TestDomain( ... )
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
 * 		public void injectTestModel(MetadataImplementor model) {
 * 		 	this.model = model;
 * 		}
 * }
 * ----
 *
 * It can also be used in conjunction with {@link SessionFactory}
 *
 * @see TestDomainAware
 *
 * @author Steve Ebersole
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ServiceRegistryExtension.class )
@ExtendWith( ServiceRegistryParameterResolver.class )
@ExtendWith( TestDomainExtension.class )
@ExtendWith( TestDomainParameterResolver.class )
public @interface TestDomain {
	StandardDomainModel[] standardModels() default {};
	Class<? extends DomainModelDescriptor>[] modelDescriptorClasses() default {};
	Class[] annotatedClasses() default {};
	String[] annotatedClassNames() default {};
	String[] xmlMappings() default {};
}
