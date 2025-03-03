/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = OneToOneJoinTableSelfReferenceTest.Person.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18399" )
public class OneToOneJoinTableSelfReferenceTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person parent = new Person();
			parent.setId( 2L );

			final Person child = new Person();
			child.setId( 1L );
			parent.setChild( child );

			session.persist( child );
			session.persist( parent );
		} );
		scope.inTransaction( session -> {
			final Person parent = session.find( Person.class, 2L );
			assertThat( parent.getChild() ).isNotNull().extracting( Person::getId ).isEqualTo( 1L );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "Person" )
	static class Person {
		@Id
		private Long id;

		@OneToOne( fetch = FetchType.LAZY )
		@JoinTable(
				name = "Parent_Child",
				joinColumns = @JoinColumn( name = "parent_id", referencedColumnName = "id" ),
				inverseJoinColumns = @JoinColumn( name = "child_id", referencedColumnName = "id" )
		)
		private Person child;

		@OneToOne( mappedBy = "child", fetch = FetchType.LAZY )
		private Person parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Person getChild() {
			return child;
		}

		public void setChild(Person child) {
			this.child = child;
		}

		public Person getParent() {
			return parent;
		}
	}
}
