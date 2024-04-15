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

import org.hibernate.jpa.spi.JpaCompliance;

import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;



/**
 * @author Steve Ebersole
 */
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )

@TestInstance( TestInstance.Lifecycle.PER_CLASS )

@ExtendWith( EntityManagerFactoryExtension.class )
@ExtendWith( EntityManagerFactoryParameterResolver.class )

@ExtendWith( FailureExpectedExtension.class )
public @interface Jpa {
	String persistenceUnitName() default "test-pu";

	/**
	 * Used to mimic container integration
	 */
	Setting[] integrationSettings() default {};

	// todo : multiple persistence units?

	/**
	 * Persistence unit properties
	 */
	Setting[] properties() default {};

	SettingProvider[] settingProviders() default {};

	boolean generateStatistics() default false;
	boolean exportSchema() default true;

	PersistenceUnitTransactionType transactionType() default PersistenceUnitTransactionType.RESOURCE_LOCAL;
	SharedCacheMode sharedCacheMode() default SharedCacheMode.UNSPECIFIED;
	ValidationMode validationMode() default ValidationMode.NONE;

	/**
	 * @see JpaCompliance#isJpaQueryComplianceEnabled()
	 */
	boolean queryComplianceEnabled() default false;

	/**
	 * @see JpaCompliance#isJpaTransactionComplianceEnabled()
	 */
	boolean transactionComplianceEnabled() default false;

	/**
	 * @see JpaCompliance#isJpaClosedComplianceEnabled()
	 */
	boolean closedComplianceEnabled() default false;

	/**
	 * @see JpaCompliance#isJpaProxyComplianceEnabled()
	 */
	boolean proxyComplianceEnabled() default false;

	/**
	 * @see JpaCompliance#isJpaCacheComplianceEnabled()
	 */
	boolean cacheComplianceEnabled() default false;

	/**
	 * @see JpaCompliance#isGlobalGeneratorScopeEnabled()
	 */
	boolean generatorScopeComplianceEnabled() default false;

	boolean excludeUnlistedClasses() default false;

	StandardDomainModel[] standardModels() default {};
	Class<? extends DomainModelDescriptor>[] modelDescriptorClasses() default {};
	Class[] annotatedClasses() default {};
	String[] annotatedClassNames() default {};
	String[] annotatedPackageNames() default {};
	String[] xmlMappings() default {};
}
