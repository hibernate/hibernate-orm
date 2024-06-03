/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				LazyOneToOneMultiAssociationTest.EntityA.class, LazyOneToOneMultiAssociationTest.EntityB.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-16108")
public class LazyOneToOneMultiAssociationTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			s.persist( entityA );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "delete entityb" ).executeUpdate();
			s.createMutationQuery( "delete entitya" ).executeUpdate();
		} );
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, 1 );
			EntityB entityB = new EntityB( 2 );
			entityA.setMappedAssociation1( entityB );
			entityB.setAssociation1( entityA );
			s.persist( entityB );
			// Without a fix, this ends up failing during the flush:
			// java.lang.NullPointerException: Cannot invoke "org.hibernate.metamodel.mapping.SelectableMapping.isFormula()" because "selectable" is null
			//	at org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard.processSet(UpdateCoordinatorStandard.java:665)
		} );

		scope.inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, 1 );
			assertThat( entityA ).isNotNull();

			assertFalse( Hibernate.isPropertyInitialized( entityA, "mappedAssociation1" ) );
			EntityB entityB = entityA.getMappedAssociation1();

			assertThat( entityB ).isNotNull();
			assertThat( entityB.getAssociation1() ).isEqualTo( entityA );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {

		@Id
		private Integer id;

		@OneToOne
		private EntityA selfAssociation;

		@OneToOne(mappedBy = "selfAssociation")
		private EntityA mappedSelfAssociation;

		@OneToOne(mappedBy = "association1", fetch = FetchType.LAZY)
		private EntityB mappedAssociation1;

		@OneToOne(mappedBy = "association2", fetch = FetchType.LAZY)
		private EntityB mappedAssociation2;

		protected EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getSelfAssociation() {
			return selfAssociation;
		}

		public void setSelfAssociation(EntityA selfAssociation) {
			this.selfAssociation = selfAssociation;
		}

		public EntityA getMappedSelfAssociation() {
			return mappedSelfAssociation;
		}

		public void setMappedSelfAssociation(EntityA mappedSelfAssociation) {
			this.mappedSelfAssociation = mappedSelfAssociation;
		}

		public EntityB getMappedAssociation1() {
			return mappedAssociation1;
		}

		public void setMappedAssociation1(EntityB mappedAssociation1) {
			this.mappedAssociation1 = mappedAssociation1;
		}

		public EntityB getMappedAssociation2() {
			return mappedAssociation2;
		}

		public void setMappedAssociation2(EntityB mappedAssociation2) {
			this.mappedAssociation2 = mappedAssociation2;
		}

	}

	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		@OneToOne
		private EntityA association1;

		@OneToOne
		private EntityA association2;

		protected EntityB() {
		}

		public EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getAssociation1() {
			return association1;
		}

		public void setAssociation1(EntityA association1) {
			this.association1 = association1;
		}

		public EntityA getAssociation2() {
			return association2;
		}

		public void setAssociation2(EntityA association2) {
			this.association2 = association2;
		}

	}
}
