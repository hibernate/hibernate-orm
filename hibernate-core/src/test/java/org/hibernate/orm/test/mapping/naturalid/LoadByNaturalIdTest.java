/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				LoadByNaturalIdTest.Parent.class,
				LoadByNaturalIdTest.Child.class
		}
)
@SessionFactory
@JiraKey("HHH-16968")
public class LoadByNaturalIdTest {

	private static final Long PARENT_ID = 1l;
	private static final String PARENT_NAME = "Luigi";
	private static final Long CHILD_ID = 1l;
	private static final String CHILD_NAME = "Filippo";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( PARENT_ID, PARENT_NAME );
					new Child( CHILD_ID, CHILD_NAME, parent );
					session.persist( parent );
				}
		);
	}

	@Test
	public void testResolveNaturalIdToId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NaturalIdLoader<?> naturalIdLoader = getNaturalIdLoader( Parent.class, session );
					Long parentId = (Long) naturalIdLoader.resolveNaturalIdToId(
							PARENT_NAME,
							session
					);
					assertThat( parentId ).isNotNull();
					assertThat( parentId ).isEqualTo( PARENT_ID );
				}
		);
	}

	@Test
	public void testLoadByNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.byNaturalId( Parent.class ).using( "name", PARENT_NAME ).load();
					assertThat( parent ).isNotNull();
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
					assertThat( parent.getChildren().iterator().next().getName() ).isEqualTo( CHILD_NAME );
				}
		);
	}

	@Test
	public void testResolveIdToNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NaturalIdLoader<?> naturalIdLoader = getNaturalIdLoader(
							Parent.class,
							session
					);
					String naturalId = (String) naturalIdLoader.resolveIdToNaturalId(
							PARENT_ID,
							session
					);
					assertThat( naturalId ).isNotNull();
					assertThat( naturalId ).isEqualTo( PARENT_NAME );
				}
		);
	}


	private static NaturalIdLoader<?> getNaturalIdLoader(Class clazz, SessionImplementor session) {
		return session.getFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( clazz )
				.getNaturalIdLoader();
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		@NaturalId
		private String name;

		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
			parent.children.add( this );
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}
	}
}
