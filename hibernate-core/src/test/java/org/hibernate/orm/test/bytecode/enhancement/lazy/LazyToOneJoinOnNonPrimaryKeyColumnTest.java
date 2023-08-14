/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-17075")
public class LazyToOneJoinOnNonPrimaryKeyColumnTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	@Before
	public void prepare() {
		inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			entityA.setMyUniqueKey( "someValue" );
			s.persist( entityA );
			EntityB entityB = new EntityB( 2 );
			entityB.setMyAssociation( entityA );
			s.persist( entityB );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( s -> {
			s.createMutationQuery( "delete entityb" ).executeUpdate();
			s.createMutationQuery( "delete entitya" ).executeUpdate();
		} );
	}

	@Test
	public void testLazyLoading() {
		inTransaction( s -> {
			EntityB entityB = s.get( EntityB.class, 2 );
			// Trigger lazy loading
			assertThat( entityB.getMyAssociation() )
					.isNotNull() // When the bug isn't fixed, this assertion fails
					.returns( 1, EntityA::getId );
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
