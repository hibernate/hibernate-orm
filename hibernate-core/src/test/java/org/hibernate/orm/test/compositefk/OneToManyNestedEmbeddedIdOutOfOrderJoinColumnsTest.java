/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		OneToManyNestedEmbeddedIdOutOfOrderJoinColumnsTest.ChildEntity.class,
		OneToManyNestedEmbeddedIdOutOfOrderJoinColumnsTest.ParentEntity.class
} )
@SessionFactory
public class OneToManyNestedEmbeddedIdOutOfOrderJoinColumnsTest {
	@Test
	void testOutOfOrderNestedEmbeddedIdReference(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final ParentEntity parent = new ParentEntity(
					new ParentEntityId( 1, new NestedEmbeddable( "parent" ) ),
					List.of( new ChildEntity( 1 ), new ChildEntity( 2 ) )
			);
			session.persist( parent );
		} );

		scope.inTransaction( (session) -> {
			final ParentEntity parent = session.find(
					ParentEntity.class,
					new ParentEntityId( 1, new NestedEmbeddable( "parent" ) )
			);
			assertThat( parent.children ).hasSize( 2 );
		} );
	}

	@Entity( name = "NestedOutOfOrderChild" )
	@Table( name = "nested_out_of_order_child" )
	public static class ChildEntity {
		@Id
		private Integer id;

		protected ChildEntity() {
		}

		private ChildEntity(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "NestedOutOfOrderParent" )
	@Table( name = "nested_out_of_order_parent" )
	public static class ParentEntity {
		@EmbeddedId
		private ParentEntityId id;

		@OneToMany( cascade = CascadeType.ALL )
		@JoinColumns( {
				@JoinColumn( name = "parent_name", referencedColumnName = "name", nullable = false ),
				@JoinColumn( name = "parent_id", referencedColumnName = "id", nullable = false )
		} )
		private List<ChildEntity> children;

		protected ParentEntity() {
		}

		private ParentEntity(ParentEntityId id, List<ChildEntity> children) {
			this.id = id;
			this.children = children;
		}
	}

	@Embeddable
	public record ParentEntityId(Integer id, NestedEmbeddable nested) implements Serializable {
	}

	@Embeddable
	public record NestedEmbeddable(String name) implements Serializable {
	}
}
