/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.mapping;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				DefaultCascadeTest.Parent.class,
				DefaultCascadeTest.Child.class
		},
		//	using 'xmlMappings = { "org/hibernate/orm/test/jpa/mapping/orm.xml" }' also works
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.ORM_XML_FILES,
						provider = DefaultCascadeTest.EJB3DDMappingProvider.class )
		}
)
public class DefaultCascadeTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Child" ).executeUpdate();
					entityManager.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCascadePersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent();
					Child child = new Child();
					child.parent = parent;

					entityManager.persist( child );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity(name = "Child")
	@Table(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;
	}

	public static class EJB3DDMappingProvider implements SettingProvider.Provider<List<String>> {
		@Override
		public List<String> getSetting() {
			return Arrays.asList( "org/hibernate/orm/test/jpa/mapping/orm.xml" );
		}
	}
}
