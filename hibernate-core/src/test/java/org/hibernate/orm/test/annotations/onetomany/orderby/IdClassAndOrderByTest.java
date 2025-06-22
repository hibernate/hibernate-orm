/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany.orderby;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
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
		public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
			String var10000 = source.getOwningPhysicalTableName();
			String name = var10000 + "_" + source.getAssociationOwningAttributePath().getProperty();
			return this.toIdentifier( name, source.getBuildingContext() );
		}
	}
}
