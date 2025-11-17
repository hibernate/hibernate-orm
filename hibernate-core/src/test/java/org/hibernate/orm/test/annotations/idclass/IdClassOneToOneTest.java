/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclass;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		IdClassOneToOneTest.CompositeId.class,
		IdClassOneToOneTest.ParentEntity.class,
		IdClassOneToOneTest.ChildEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17205" )
public class IdClassOneToOneTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new ParentEntity( "1", "one" ) );
			session.persist( new ChildEntity( "1", "one" ) );
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
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity result = session.createQuery(
					"select p from ParentEntity p where p.first = :first",
					ParentEntity.class
			).setParameter( "first", "1" ).getSingleResult();
			assertThat( result.getChildEntity() ).isNotNull();
			assertThat( result.getChildEntity().getFirst() ).isEqualTo( "1" );
			assertThat( result.getChildEntity().getSecond() ).isEqualTo( "one" );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity result = session.find( ParentEntity.class, new CompositeId( "1", "one" ) );
			assertThat( result.getChildEntity() ).isNotNull();
			assertThat( result.getChildEntity().getFirst() ).isEqualTo( "1" );
			assertThat( result.getChildEntity().getSecond() ).isEqualTo( "one" );
		} );
	}

	public static class CompositeId implements Serializable {
		private String first;
		private String second;

		public CompositeId() {
		}

		public CompositeId(String first, String second) {
			this.first = first;
			this.second = second;
		}
	}

	@Entity( name = "ParentEntity" )
	@IdClass( CompositeId.class )
	public static class ParentEntity {
		@Id
		@Column( name = "col1" )
		private String first;

		@Id
		@Column( name = "col2" )
		private String second;

		@OneToOne( mappedBy = "parentEntity" )
		private ChildEntity childEntity;

		public ParentEntity() {
		}

		public ParentEntity(String first, String second) {
			this.first = first;
			this.second = second;
		}

		public ChildEntity getChildEntity() {
			return childEntity;
		}
	}

	@Entity( name = "ChildEntity" )
	@IdClass( CompositeId.class )
	public static class ChildEntity {
		@Id
		@Column( name = "col1" )
		private String first;

		@Id
		@Column( name = "col2" )
		private String second;

		@OneToOne
		@JoinColumn( name = "col1", referencedColumnName = "col1" )
		@JoinColumn( name = "col2", referencedColumnName = "col2" )
		private ParentEntity parentEntity;

		public ChildEntity() {
		}

		public ChildEntity(String first, String second) {
			this.first = first;
			this.second = second;
		}

		public String getFirst() {
			return first;
		}

		public String getSecond() {
			return second;
		}

		public ParentEntity getParentEntity() {
			return parentEntity;
		}
	}
}
