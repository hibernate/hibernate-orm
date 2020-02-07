/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.integrationprovider;

import java.util.List;
import java.util.Properties;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHHH-13853")
public class IntegrationProviderSettingByClassUsingPropertiesTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<PersonDto> dtos = entityManager.createQuery(
					"select new PersonDto(id, name) " +
					"from Person", PersonDto.class )
			.getResultList();
		} );
	}

	protected PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ) {
			@Override
			public Properties getProperties() {
				Properties properties = new Properties();
				properties.put(
						EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER,
						DtoIntegratorProvider.class.getName()
				);
				return properties;
			}
		};
	}
}
