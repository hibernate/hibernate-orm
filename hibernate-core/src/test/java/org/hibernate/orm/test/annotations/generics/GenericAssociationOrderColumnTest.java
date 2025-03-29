/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

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
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericAssociationOrderColumnTest.AbstractParent.class,
		GenericAssociationOrderColumnTest.ParentEntity.class,
		GenericAssociationOrderColumnTest.ChildEntity.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16641" )
public class GenericAssociationOrderColumnTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity p1 = new ParentEntity( 1L );
			final ChildEntity c1 = new ChildEntity( 2L, p1 );
			p1.getChildren().add( c1 );
			final ChildEntity c2 = new ChildEntity( 3L, p1 );
			p1.getChildren().add( c2 );
			final ChildEntity c3 = new ChildEntity( 4L, p1 );
			p1.getChildren().add( c3 );
			session.persist( p1 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ChildEntity" ).executeUpdate();
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity parent = session.find( ParentEntity.class, 1L );
			assertThat( parent.getChildren().stream().map( ChildEntity::getId ) ).containsExactly( 2L, 3L, 4L );
		} );
	}

	@MappedSuperclass
	public static abstract class AbstractParent<T extends ChildEntity> {
		@Id
		public Long id;

		@OneToMany( mappedBy = "parent", cascade = CascadeType.ALL )
		@OrderColumn
		public List<T> children = new ArrayList<>();

		public AbstractParent() {
		}

		public AbstractParent(Long id) {
			this.id = id;
		}

		public List<T> getChildren() {
			return children;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		public Long id;

		@ManyToOne
		public ParentEntity parent;

		public ChildEntity() {
		}

		public ChildEntity(Long id, ParentEntity parent) {
			this.id = id;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public ParentEntity getParent() {
			return parent;
		}
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity extends AbstractParent<ChildEntity> {
		public ParentEntity() {
		}

		public ParentEntity(Long id) {
			super( id );
		}
	}
}
