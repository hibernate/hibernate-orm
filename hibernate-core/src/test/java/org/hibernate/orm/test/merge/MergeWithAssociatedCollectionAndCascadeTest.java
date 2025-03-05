/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MergeWithAssociatedCollectionAndCascadeTest.SomeChildEntity.class,
		MergeWithAssociatedCollectionAndCascadeTest.SomeEntity.class,
		MergeWithAssociatedCollectionAndCascadeTest.CompositePk.class,
		MergeWithAssociatedCollectionAndCascadeTest.CompositePkEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17193" )
public class MergeWithAssociatedCollectionAndCascadeTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SomeEntity someEntity = new SomeEntity( 1L );
			final SomeChildEntity child1 = new SomeChildEntity( 1L, someEntity );
			final SomeChildEntity child2 = new SomeChildEntity( 2L, someEntity );
			session.persist( child1 );
			session.persist( child2 );
			session.persist( someEntity );
			session.persist( new CompositePkEntity( new CompositePk( someEntity, "value" ), "initial-value" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from CompositePkEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SomeChildEntity" ).executeUpdate();
			session.createMutationQuery( "delete from SomeEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testCompositePkEntityMerge(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SomeEntity someEntity = session.find( SomeEntity.class, 1L );
			final CompositePkEntity compositePkEntity = new CompositePkEntity(
					new CompositePk( someEntity, "value" ),
					"new-value"
			);
			session.merge( compositePkEntity );
		} );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from CompositePkEntity",
				CompositePkEntity.class
		).getSingleResult().getSomeValue() ).isEqualTo( "new-value" ) );
	}

	@Entity( name = "SomeChildEntity" )
	public static class SomeChildEntity {
		@Id
		private Long id;

		@ManyToOne
		@JoinColumn( name = "some_entity" )
		private SomeEntity someEntity;

		public SomeChildEntity() {
		}

		public SomeChildEntity(Long id, SomeEntity someEntity) {
			this.id = id;
			this.someEntity = someEntity;
		}
	}

	@Entity( name = "SomeEntity" )
	public static class SomeEntity {
		@Id
		private Long id;

		@OneToMany( mappedBy = "someEntity", cascade = CascadeType.ALL )
		private List<SomeChildEntity> childEntities = new ArrayList<>();

		public SomeEntity() {
		}

		public SomeEntity(Long id) {
			this.id = id;
		}

		public List<SomeChildEntity> getChildEntities() {
			return childEntities;
		}
	}

	@Embeddable
	public static class CompositePk implements Serializable {
		@ManyToOne
		@JoinColumn( name = "some_entity_id" )
		private SomeEntity someEntity;

		@Column( name = "some_pk_value" )
		private String somePkValue;

		public CompositePk() {
		}

		public CompositePk(SomeEntity someEntity, String somePkValue) {
			this.someEntity = someEntity;
			this.somePkValue = somePkValue;
		}
	}

	@Entity( name = "CompositePkEntity" )
	public static class CompositePkEntity {
		@EmbeddedId
		private CompositePk id;

		@Column( name = "some_value" )
		private String someValue;

		public CompositePkEntity() {
		}

		public CompositePkEntity(CompositePk id, String someValue) {
			this.id = id;
			this.someValue = someValue;
		}

		public String getSomeValue() {
			return someValue;
		}
	}
}
