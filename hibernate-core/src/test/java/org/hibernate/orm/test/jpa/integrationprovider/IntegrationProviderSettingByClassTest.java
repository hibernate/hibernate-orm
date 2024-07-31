/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import java.util.List;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13614")
@Jpa(
		annotatedClasses = {
				Person.class
		},
		integrationSettings = { @Setting(name = EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, value = "org.hibernate.orm.test.jpa.integrationprovider.DtoIntegratorProvider") }
)
public class IntegrationProviderSettingByClassTest {

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
