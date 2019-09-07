/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.*;
import javax.persistence.*;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ConstructorResultDtoTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
			Person.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			List<PersonDto> dtos = entityManager.createQuery(
                "select new PersonDto(id, name) " +
                "from Person", PersonDto.class)
            .getResultList();
		});
	}

    @Override
    protected void addMappings(Map settings) {
        settings.put(EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, DtoIntegratorProvider.class.getName());
    }

    public static class DtoIntegratorProvider implements IntegratorProvider {
		@Override
		public List<Integrator> getIntegrators() {
			return Collections.singletonList(
					new Integrator() {
						@Override
						public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
							metadata.getImports().put("PersonDto", PersonDto.class.getName());
						}

						@Override
						public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

						}
					}
			);
		}
	}

    @Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private int age;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	public static class PersonDto {

		private Long id;

		private String name;

		public PersonDto(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
