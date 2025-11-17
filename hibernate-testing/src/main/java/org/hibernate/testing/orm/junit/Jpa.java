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

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.DomainModelDescriptor;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;



/**
 * @author Steve Ebersole
 */
@Inherited
@Target( {ElementType.TYPE, ElementType.METHOD} )
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

	SettingProvider[] integrationSettingProviders() default {};

	// todo : multiple persistence units?

	/**
	 * Persistence unit properties
	 */
	Setting[] properties() default {};

	SettingProvider[] settingProviders() default {};

	SettingConfiguration[] settingConfigurations() default {};

	boolean generateStatistics() default false;
	boolean exportSchema() default true;

	PersistenceUnitTransactionType transactionType() default PersistenceUnitTransactionType.RESOURCE_LOCAL;
	SharedCacheMode sharedCacheMode() default SharedCacheMode.UNSPECIFIED;
	ValidationMode validationMode() default ValidationMode.NONE;

	boolean excludeUnlistedClasses() default false;

	StandardDomainModel[] standardModels() default {};
	Class<? extends DomainModelDescriptor>[] modelDescriptorClasses() default {};
	Class[] annotatedClasses() default {};
	String[] annotatedClassNames() default {};
	String[] annotatedPackageNames() default {};
	String[] xmlMappings() default {};

	/**
	 * Shortcut for adding {@code @Setting( name = AvailableSettings.STATEMENT_INSPECTOR, value = "org.hibernate.testing.jdbc.SQLStatementInspector"}
	 * to the integration settings.
	 * Note: if the statement inspector is also explicitly specified as a setting, it will be overridden by the shortcut
	 * @see SQLStatementInspector
	 */
	boolean useCollectingStatementInspector() default false;
}
