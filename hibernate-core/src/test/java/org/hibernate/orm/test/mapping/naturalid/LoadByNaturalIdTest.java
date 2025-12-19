/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Timeout;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Cursor must be on simple SELECT for FOR UPDATE")
	void testFindOptions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.find( Parent.class, "Luigi",
					KeyType.NATURAL,
					LockMode.PESSIMISTIC_WRITE,
					Timeout.seconds( 1 ),
					ReadOnlyMode.READ_ONLY );
			session.findMultiple( Parent.class, List.of("Luigi"),
					KeyType.NATURAL,
					LockMode.PESSIMISTIC_WRITE,
					Timeout.seconds( 1 ),
					ReadOnlyMode.READ_ONLY );
		} );
	}

	@Test
	void testFindMultipleOptions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var multiple = session.findMultiple(
					Parent.class,
					List.of( "Luigi", "Filippo" ),
					KeyType.NATURAL,
					SessionCheckMode.ENABLED,
					OrderingMode.UNORDERED,
					RemovalsMode.EXCLUDE
			);
			assertEquals( 1, multiple.size() );
		} );
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Parent.class, "Luigi", KeyType.NATURAL ) );
			var multiple = session.findMultiple(
					Parent.class,
					List.of( "Luigi", "Filippo" ),
					KeyType.NATURAL,
					SessionCheckMode.ENABLED,
					OrderingMode.UNORDERED,
					RemovalsMode.EXCLUDE
			);
			assertEquals( 0, multiple.size() );
		} );
	}

	@Test
	void testFindMultipleOptions2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var multiple = session.findMultiple(
					Parent.class,
					List.of( "Luigi", "Filippo" ),
					KeyType.NATURAL,
					SessionCheckMode.ENABLED,
					RemovalsMode.REPLACE
			);
			assertEquals( 2, multiple.size() );
			assertNotNull( multiple.get(0) );
			assertNull( multiple.get(1) );
		} );
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Parent.class, "Luigi", KeyType.NATURAL ) );
			var multiple = session.findMultiple(
					Parent.class,
					List.of( "Luigi", "Filippo" ),
					KeyType.NATURAL,
					SessionCheckMode.ENABLED,
					RemovalsMode.REPLACE
			);
			assertEquals( 2, multiple.size() );
			assertNull( multiple.get(0) );
			assertNull( multiple.get(1) );
		} );
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
