package org.hibernate.orm.test.bytecode;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true, lazyLoading = true, extendedEnhancement = true)
@JiraKey( "HHH-16542" )
public class GetAndIsMethodTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				EntityA.class,
				EntityB.class
		};
	}

	@Test
	public void testIt(){

	}

	@Entity(name = "EntityA")
	@Table(name = "A_TABLE")
	public static class EntityA  {

		@Id
		protected String id;

		protected String name;

		public EntityA() {
		}

		public EntityA(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@OneToOne
		protected EntityB entityB;

		public EntityB getEntityB() {
			return entityB;
		}

		public boolean isEntityB() {
			return getEntityB() != null;
		}

		public String getAId() {
			return id;
		}

		public String getAName() {
			return name;
		}

	}

	@Entity(name = "EntityB")
	@Table(name = "B_TABLE")
	public static class EntityB {

		@Id
		protected String id;

		protected String name;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
