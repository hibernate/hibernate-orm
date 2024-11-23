package org.hibernate.test.annotations.onetomany;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceException;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "HHH-15091")
public class OneToManyJoinColumnsUniquenessTest extends BaseCoreFunctionalTestCase {
	private static final SQLStatementInspector statementInspector = new SQLStatementInspector();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class,
				EntityB.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.STATEMENT_INSPECTOR, statementInspector );
	}

	@Test
	public void testInsertWithNullAssociationThrowPersistenceException() {
		statementInspector.clear();

		inTransaction(
				session -> {
					try {
						EntityB entityB = new EntityB( 1l );
						session.persist( entityB );
						fail("PersistenceException expected");
					}
					catch (PersistenceException e) {
						//expected
					}
					// check that no insert statement has bees executed
					statementInspector.assertExecutedCount( 0 );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@EmbeddedId
		private PK id;

		@OneToMany(mappedBy = "entityA", fetch = FetchType.LAZY)
		private Set<EntityB> entityBs;

		public EntityA() {
		}
	}

	@Embeddable
	public static class PK implements Serializable {
		@Column(name = "id_1")
		private String id1;
		@Column(name = "id_2")
		private String id2;

		public PK() {
		}

		public PK(String id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns(value = {
				@JoinColumn(name = "b_to_a_1", referencedColumnName = "id_1", nullable = false)
				,
				@JoinColumn(name = "b_to_a_2", referencedColumnName = "id_2", nullable = false)
		}
		)
		private EntityA entityA;

		public EntityB() {
		}

		public EntityB(Long id) {
			this.id = id;
		}
	}
}
