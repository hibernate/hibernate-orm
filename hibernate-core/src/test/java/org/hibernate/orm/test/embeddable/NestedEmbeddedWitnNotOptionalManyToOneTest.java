/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				NestedEmbeddedWitnNotOptionalManyToOneTest.TestEntity.class,
				NestedEmbeddedWitnNotOptionalManyToOneTest.ChildEntity.class
		}

)
@SessionFactory
public class NestedEmbeddedWitnNotOptionalManyToOneTest {

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ChildEntity child = new ChildEntity( 2, "child" );
					SecondEmbeddable secondEmbeddable = new SecondEmbeddable( child );
					FirstEmbeddable embedded = new FirstEmbeddable();
					embedded.addEntry( secondEmbeddable );
					TestEntity testEntity = new TestEntity( 1, "test", embedded );
					session.persist( child );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.get( TestEntity.class, 1 );
					assertNotNull( testEntity );
					FirstEmbeddable embeddedAttribute = testEntity.getEmbeddedAttribute();
					assertNotNull( embeddedAttribute );
					Set<SecondEmbeddable> entries = embeddedAttribute.getEntries();
					assertThat( entries.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Integer id;

		private String name;

		@Embedded
		private FirstEmbeddable embeddedAttribute;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, FirstEmbeddable embeddedAttribute) {
			this.id = id;
			this.name = name;
			this.embeddedAttribute = embeddedAttribute;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public FirstEmbeddable getEmbeddedAttribute() {
			return embeddedAttribute;
		}
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		private Integer id;

		private String name;

		public ChildEntity() {
		}

		public ChildEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}


	@Embeddable
	public static class FirstEmbeddable {

		@ElementCollection
		private Set<SecondEmbeddable> entries = new HashSet<>();


		public void addEntry(SecondEmbeddable entry) {
			this.entries.add( entry );
		}

		public Set<SecondEmbeddable> getEntries() {
			return entries;
		}
	}

	@Embeddable
	public static class SecondEmbeddable {

		@ManyToOne(optional = false, cascade = CascadeType.ALL)
		private ChildEntity child;

		public SecondEmbeddable() {
		}

		public SecondEmbeddable(ChildEntity child) {
			this.child = child;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

}
