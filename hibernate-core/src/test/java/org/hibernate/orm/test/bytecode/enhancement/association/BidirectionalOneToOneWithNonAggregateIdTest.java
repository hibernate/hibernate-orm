/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.LazyGroup;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.FetchMode.SELECT;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				BidirectionalOneToOneWithNonAggregateIdTest.Entity1.class,
				BidirectionalOneToOneWithNonAggregateIdTest.Entity2.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-17519")
public class BidirectionalOneToOneWithNonAggregateIdTest  {

	static final int ENTITY_ID = 1;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Entity1 e1 = new Entity1( ENTITY_ID );
					Entity2 e2 = new Entity2();

					e1.setChild( e2 );

					e2.setCaseNumber( "TEST" );
					e2.setParent( e1 );

					session.persist( e1 );
				}
		);
	}


	@Test
	public void testRemovingChild(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Entity1 e1 = session.byId( Entity1.class ).load( ENTITY_ID );
					Entity2 child = e1.getChild();
					assertNotNull( child );
				}
		);
	}

	@Entity(name = "Entity1")
	@Table(name = "entity1")
	@DynamicInsert
	@DynamicUpdate
	public static class Entity1 {
		@Id
		protected int id;

		@Version
		@Column(name = "lock_version", nullable = false)
		protected int lockVersion;

		@OneToOne(fetch = LAZY, cascade = ALL, orphanRemoval = true, mappedBy = "parent")
		@LazyGroup("group2")
		@Fetch(SELECT)
		protected Entity2 child;

		public Entity1() {
		}

		public Entity1(int id) {
			this.id = id;
		}

		public Entity2 getChild() {
			return child;
		}

		public void setChild(Entity2 child) {
			this.child = child;
		}
	}

	@Entity(name = "Entity2")
	@Table(name = "entity2")
	@DynamicInsert
	@DynamicUpdate
	public static class Entity2 {
		@Id
		@OneToOne(fetch = LAZY, optional = false)
		@LazyGroup("owner")
		@JoinColumn(name = "parentid", nullable = false, updatable = false)
		@Fetch(SELECT)
		protected Entity1 parent;

		@Column(name = "case_number")
		protected String caseNumber;

		public void setParent(Entity1 parent) {
			this.parent = parent;
		}

		public void setCaseNumber(String caseNumber) {
			this.caseNumber = caseNumber;
		}
	}

}
