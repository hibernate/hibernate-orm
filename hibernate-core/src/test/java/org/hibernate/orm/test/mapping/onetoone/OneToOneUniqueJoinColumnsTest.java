/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OneToOneUniqueJoinColumnsTest.BaseEntity.class,
		OneToOneUniqueJoinColumnsTest.EntityA.class,
		OneToOneUniqueJoinColumnsTest.EntityB.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18390" )
public class OneToOneUniqueJoinColumnsTest {
	@Test
	public void testFindBothEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.createSelectionQuery( "from EntityA", EntityA.class ).getSingleResult();
			assertThat( entityA ).isNotNull().extracting( BaseEntity::getName ).isEqualTo( "entity_a" );
			assertThat( entityA.getEntityB() ).isNotNull().extracting( BaseEntity::getName ).isEqualTo( "entity_b" );
		} );
	}

	@Test
	public void testFindBothEntitiesInReverse(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB entityB = session.createSelectionQuery( "from EntityB", EntityB.class ).getSingleResult();
			assertThat( entityB ).isNotNull().extracting( BaseEntity::getName ).isEqualTo( "entity_b" );
			assertThat( entityB.getEntityA() ).isNotNull().extracting( BaseEntity::getName ).isEqualTo( "entity_a" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA();
			entityA.setName( "entity_a" );
			entityA.setColumnAEntityA( 10L );
			entityA.setColumnBEntityA( 11L );
			session.persist( entityA );

			final EntityB entityB = new EntityB();
			entityB.setName( "entity_b" );
			entityB.setColumnAEntityB( 10L );
			entityB.setColumnBEntityB( 11L );
			session.persist( entityB );
		} );
	}

	@MappedSuperclass
	static class BaseEntity {
		@Id
		@GeneratedValue
		public Long id;

		public String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "EntityA" )
	@Table( name = "EntityA", uniqueConstraints = @UniqueConstraint( columnNames = {
			"columnAEntityA",
			"columnBEntityA"
	} ) )
	static class EntityA extends BaseEntity {
		@Column( name = "columnAEntityA" )
		public Long columnAEntityA;

		@Column( name = "columnBEntityA" )
		public Long columnBEntityA;

		@OneToOne( mappedBy = "entityA" )
		public EntityB entityB;

		public Long getColumnAEntityA() {
			return columnAEntityA;
		}

		public void setColumnAEntityA(Long columnAEntityA) {
			this.columnAEntityA = columnAEntityA;
		}

		public Long getColumnBEntityA() {
			return columnBEntityA;
		}

		public void setColumnBEntityA(Long columnBEntityA) {
			this.columnBEntityA = columnBEntityA;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}
	}

	@Entity( name = "EntityB" )
	@Table( name = "EntityB", uniqueConstraints = @UniqueConstraint( columnNames = {
			"columnAEntityB",
			"columnBEntityB"
	} ) )
	static class EntityB extends BaseEntity {
		@Column( name = "columnAEntityB" )
		public Long columnAEntityB;
		@Column( name = "columnBEntityB" )
		public Long columnBEntityB;

		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumns( {
				@JoinColumn( name = "columnAEntityB", referencedColumnName = "columnAEntityA", insertable = false, updatable = false ),
				@JoinColumn( name = "columnBEntityB", referencedColumnName = "columnBEntityA", insertable = false, updatable = false )
		} )
		public EntityA entityA;

		public Long getColumnAEntityB() {
			return columnAEntityB;
		}

		public void setColumnAEntityB(Long columnAEntityB) {
			this.columnAEntityB = columnAEntityB;
		}

		public Long getColumnBEntityB() {
			return columnBEntityB;
		}

		public void setColumnBEntityB(Long columnBEntityB) {
			this.columnBEntityB = columnBEntityB;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}
	}
}
