/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
public class EntityWithSetOfComponentsEagerlyFetchedTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Test
	public void testOperations() {
		final TestEntity entity = new TestEntity( 1 );

		entity.getSetOfComponents().add( new Component( 5 ) );

		inTransaction( session -> session.save( entity ) );

		inTransaction(
				session -> {
					final Integer value = session.createQuery( "select e.id from TestEntity e", Integer.class )
							.uniqueResult();
					assert value == 1;
				}
		);

		inTransaction(
				session -> {
					final TestEntity loaded = session.get( TestEntity.class, 1 );
					assert loaded != null;
					Set<Component> setOfEagerComponents = loaded.getSetOfComponents();
					assertTrue(
							Hibernate.isInitialized( setOfEagerComponents ),
							"The eager collection has not been initialized"
					);
					assertThat( setOfEagerComponents.size(), is( 1 ) );
				}
		);

		inTransaction(
				session -> {
					final List<TestEntity> list = session.byMultipleIds( TestEntity.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final TestEntity loaded = list.get( 0 );
					assert loaded != null;
					Set<Component> setOfEagerComponents = loaded.getSetOfComponents();
					assertTrue(
							Hibernate.isInitialized( setOfEagerComponents ),
							"The eager collection has not been initialized"
					);
					assertThat( setOfEagerComponents.size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<Component> setOfComponents = new HashSet<>();

		public TestEntity() {
		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public Set<Component> getSetOfComponents() {
			return setOfComponents;
		}

		public void setSetOfComponents(Set<Component> setOfComponents) {
			this.setOfComponents = setOfComponents;
		}
	}

	@Embeddable
	public static class Component {
		private Integer integerField;

		public Component() {
		}

		public Component(Integer integerField) {
			this.integerField = integerField;
		}
	}
}
