package org.hibernate.test.annotations.various;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Subselect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@TestForIssue(jiraKey = "HHH-15520")
public class OneOneGeneratedValueTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class,
				EntityB.class
		};
	}

	@Test
	public void testIt() {
		inTransaction(
				session -> {
					EntityA entityA = new EntityA( 1l );
					session.persist( entityA );
				}
		);
		inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1l );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getB() ).isEqualTo( 5l );
				}
		);
	}

	@Entity(name = "EntityA")
	@Table(name = "TABLE_A")
	public static class EntityA {

		@Id
		private Long id;

		private String name;

		@Generated(GenerationTime.INSERT)
		@OneToOne(mappedBy = "a")
		private EntityB b;

		public EntityA() {
		}

		public EntityA(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public EntityB getB() {
			return b;
		}
	}

	@Entity(name = "EntityB")
	@Subselect("SELECT 5 as b, a.id AS AId FROM TABLE_A a")
	public static class EntityB {

		private Long aId;

		private EntityA a;

		private Long b;

		@Id
		public Long getAId() {
			return aId;
		}

		public void setAId(Long aId) {
			this.aId = aId;
		}

		@OneToOne
		@PrimaryKeyJoinColumn
		public EntityA getA() {
			return a;
		}

		public void setA(EntityA a) {
			this.a = a;
		}

		public Long getB() {
			return b;
		}

		public void setB(Long b) {
			this.b = b;
		}


	}
}
