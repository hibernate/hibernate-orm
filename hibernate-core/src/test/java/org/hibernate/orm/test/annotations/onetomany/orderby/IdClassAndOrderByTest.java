/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany.orderby;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.source.spi.AttributePath;

import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Jpa(
		annotatedClasses = {
				ECompany.class,
				Department.class
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
						provider = IdClassAndOrderByTest.PhysicalNamingStrategyProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
						provider = IdClassAndOrderByTest.ImplicitNamingStrategyProvider.class
				),

		}
)
@JiraKey(value = "HHH-16009")
public class IdClassAndOrderByTest {

	public static final String COMPANY_NAME = "Foo Company";

	public static class PhysicalNamingStrategyProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return PhysicalNamingStrategySnakeCaseImpl.class.getName();
		}
	}

	public static class ImplicitNamingStrategyProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return CustomImplicitNamingStrategy.class.getName();
		}
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ECompany company = new ECompany();
					company.setName( COMPANY_NAME );
					entityManager.persist( company );

					Department department = new Department();
					department.setCompany( company );
					department.setDepartmentCode( "1234567" );
					department.setName( "Foo Department" );
					entityManager.persist( department );
				}
		);

	}

	@Test
	public void testSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery(
									"SELECT c FROM Company c LEFT JOIN FETCH c.departments WHERE c.name = :name" )
							.setParameter( "name", COMPANY_NAME ).getResultList();
				}
		);
	}


	public static class CustomImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
		public CustomImplicitNamingStrategy() {
		}

		@Override
		@Nonnull
		public LogicalName determineAssociationTableName(@Nonnull AssociationTableNamingInput source, @Nonnull ImplicitNamingContext context) {
			String var10000 = ((NamedTableNamingInput) source.owningTable()).names().physicalName().getText();
			String name = var10000 + "_" + AttributePath.parse( source.attributePath() ).getProperty();
			return context.implicitName( name );
		}
	}
}
