package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(inlineDirtyChecking = true)
@JiraKey("HHH-16688")
public class DirtyTrackingDynamicUpdateTest extends BaseCoreFunctionalTestCase {

	public static final int ID = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {

					TestEntity testEntity = new TestEntity( ID );
					testEntity.setaSuper( "aSuper before" );
					testEntity.setbSuper( "bSuper before" );
					testEntity.setaChild( "aChild before" );
					testEntity.setbChild( "bChild before" );
					session.persist( testEntity );
				}
		);
	}

	@Test
	public void testDynamicUpdate() {

		String aSuperNewValue = "aSuper after";
		String bSuperNewValue = "bSuper after";
		String aChildNewValue = "aChild after";
		String bChildNewValue = "bChild after";

		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					entity.setaSuper( aSuperNewValue );
					entity.setbSuper( bSuperNewValue );
					entity.setaChild( aChildNewValue );
					entity.setbChild( bChildNewValue );
					session.merge( entity );
				}
		);

		inTransaction(
				session -> {
					TestEntity entity = session.find( TestEntity.class, ID );
					assertThat( entity.getaSuper() ).isEqualTo( aSuperNewValue );
					assertThat( entity.getbSuper() ).isEqualTo( bSuperNewValue );
					assertThat( entity.getaChild() ).isEqualTo( aChildNewValue );
					assertThat( entity.getbChild() ).isEqualTo( bChildNewValue );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;
		private String aSuper;
		private String bSuper;
		private String aChild;
		private String bChild;

		public TestEntity() {
		}

		public TestEntity(Integer id) {
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
