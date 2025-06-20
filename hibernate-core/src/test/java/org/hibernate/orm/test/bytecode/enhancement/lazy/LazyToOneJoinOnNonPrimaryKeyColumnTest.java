/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@DomainModel(
		annotatedClasses = {
			LazyToOneJoinOnNonPrimaryKeyColumnTest.EntityA.class, LazyToOneJoinOnNonPrimaryKeyColumnTest.EntityB.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-17075")
public class LazyToOneJoinOnNonPrimaryKeyColumnTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			entityA.setMyUniqueKey( "someValue" );
			s.persist( entityA );
			EntityB entityB = new EntityB( 2 );
			entityB.setMyAssociation( entityA );
			s.persist( entityB );

			EntityB entityB2 = new EntityB( 3 );
			s.persist( entityB2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLazyLoading(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityB entityB = s.get( EntityB.class, 2 );
			// Trigger lazy loading
			assertThat( entityB.getMyAssociation() )
					.isNotNull() // When the bug isn't fixed, this assertion fails
					.returns( 1, EntityA::getId );

			entityB = s.get( EntityB.class, 3 );
			assertThat( entityB.getMyAssociation() )
					.isNull();
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {

		@Id
		private Integer id;

		@Column(unique = true)
		private String myUniqueKey;

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

		public String getMyUniqueKey() {
			return myUniqueKey;
		}

		public void setMyUniqueKey(String myUniqueKey) {
			this.myUniqueKey = myUniqueKey;
		}
	}

	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "myForeignKey", referencedColumnName = "myUniqueKey")
		private EntityA myAssociation;

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

		public EntityA getMyAssociation() {
			return myAssociation;
		}

		public void setMyAssociation(EntityA myAssociation) {
			this.myAssociation = myAssociation;
		}
	}
}
