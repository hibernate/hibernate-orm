/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import java.util.List;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.Test;


/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13614")
@Jpa(
		annotatedClasses = {
				Person.class
		},
		settingProviders = {
				@SettingProvider(
						settingName = EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER,
						provider = DtoIntegratorProviderInstanceSettingProvider.class )
		}
)
public class IntegrationProviderSettingByObjectTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					List<PersonDto> dtos = entityManager.createQuery(
							"select new PersonDto(id, name) " +
									"from Person", PersonDto.class )
							.getResultList();
				}
		);
	}
}
