/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.dirtiness;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true)
public class DirtinessWithEntityHierarchyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SuperEntity.class, ChildEntity.class };
	}

	@Test
	@JiraKey("HHH-16688")
	public void testDynamicUpdateWithClassHierarchy() {
		try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			ChildEntity entity = new ChildEntity();
			entity.setId( 1 );
			entity.setaSuper( "aSuper before" );
			entity.setbSuper( "bSuper before" );
			entity.setaChild( "aChild before" );
			entity.setbChild( "bChild before" );
			s.persist( entity );
			tx.commit();
		}

		try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			ChildEntity entity = session.find( ChildEntity.class, 1 );
			entity.setaSuper( "aSuper after" );
			entity.setbSuper( "bSuper after" );
			entity.setaChild( "aChild after" );
			entity.setbChild( "bChild after" );
			s.merge( entity );
			tx.commit();
		}

		try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			ChildEntity entity = session.find( ChildEntity.class, 1 );
			Assert.assertEquals( "aSuper after", entity.getaSuper() );
			Assert.assertEquals( "bSuper after", entity.getbSuper() );
			Assert.assertEquals( "aChild after", entity.getaChild() );
			Assert.assertEquals( "bChild after", entity.getbChild() );
			tx.commit();
		}
	}

	@Entity(name = "SuperEntity")
	public abstract static class SuperEntity {
		@Id
		private Integer id;
		private String aSuper;
		private String bSuper;

		public SuperEntity() {
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getaSuper() {
			return aSuper;
		}

		public void setaSuper(String aSuper) {
			this.aSuper = aSuper;
		}

		public String getbSuper() {
			return bSuper;
		}

		public void setbSuper(String bSuper) {
			this.bSuper = bSuper;
		}
	}

	@Entity(name = "ChildEntity")
	@DynamicUpdate
	public static class ChildEntity extends SuperEntity {
		private String aChild;
		private String bChild;

		public ChildEntity() {
		}

		public String getaChild() {
			return aChild;
		}

		public void setaChild(String aChild) {
			this.aChild = aChild;
		}

		public String getbChild() {
			return bChild;
		}

		public void setbChild(String bChild) {
			this.bChild = bChild;
		}
	}

}
