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

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.cache.spi.access.AccessType;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.SharedCacheMode;

/// Used to define the [domain model][org.hibernate.boot.spi.MetadataImplementor] to be used for testing.
/// Produces a [DomainModelScope] which can be injected via [JUnit ParameterResolver][DomainModelParameterResolver]
/// or via [DomainModelScopeAware]; the ParameterResolver should be preferred.
///
/// ```java
/// @DomainModel(annotatedClasses=SomeEntity.class)
/// class SomeTest {
///     @Test
///     void testStuff(DomainModelScope modelScope) {
///         ...
///     }
/// }
/// ```
///
/// @see DomainModelExtension
/// @see DomainModelScope
/// @see DomainModelScopeAware
/// @see DomainModelProducer
///
/// @author Steve Ebersole
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
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
	ExtraQueryImport[] extraQueryImports() default {};
	Class<?>[] extraQueryImportClasses() default {};

	SharedCacheMode sharedCacheMode() default SharedCacheMode.ENABLE_SELECTIVE;

	boolean overrideCacheStrategy() default true;
	String concurrencyStrategy() default "";

	AccessType accessType() default AccessType.READ_WRITE;

	Class<? extends TypeContributor>[] typeContributors() default {};

	@interface ExtraQueryImport {
		String name();
		Class<?> importedClass();
	}
}
