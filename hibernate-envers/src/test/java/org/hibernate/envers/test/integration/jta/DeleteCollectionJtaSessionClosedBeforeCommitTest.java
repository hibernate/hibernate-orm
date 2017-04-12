/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.jta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11580")
public class DeleteCollectionJtaSessionClosedBeforeCommitTest extends BaseEnversJPAFunctionalTestCase {
	private static final int ENTITY_ID = 1;
	private static final int OTHER_ENTITY_ID = 2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {TestEntity.class, OtherTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@Test
	@Priority(10)
	public void initData() throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		EntityManager entityManager = getEntityManager();
		try {
			TestEntity entity = new TestEntity( ENTITY_ID, "Fab" );
			entityManager.persist( entity );

			OtherTestEntity other = new OtherTestEntity( OTHER_ENTITY_ID, "other" );

			entity.addOther( other );
			entityManager.persist( entity );
			entityManager.persist( other );

		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		entityManager = getEntityManager();
		try {
			TestEntity entity = entityManager.find( TestEntity.class, ENTITY_ID );
			OtherTestEntity other = entityManager.find( OtherTestEntity.class, OTHER_ENTITY_ID );
			entityManager.remove( entity );
			entityManager.remove( other );
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals(
				Arrays.asList( 1, 2 ),
				getAuditReader().getRevisions( TestEntity.class, ENTITY_ID )
		);
	}

	@Test
	public void testRevisionHistory() {
		assertEquals(
				new TestEntity( 1, "Fab" ),
				getAuditReader().find( TestEntity.class, ENTITY_ID, 1 )
		);
	}

	@Audited
	@Entity
	@Table(name = "ENTITY")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		@OneToMany
		@JoinTable(name = "LINK_TABLE", joinColumns = @JoinColumn(name = "ENTITY_ID"))
		private List<OtherTestEntity> others = new ArrayList<>();

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void addOther(OtherTestEntity other) {
			this.others.add( other );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			TestEntity that = (TestEntity) o;

			if ( getId() != null ? !getId().equals( that.getId() ) : that.getId() != null ) {
				return false;
			}
			return name != null ? name.equals( that.name ) : that.name == null;
		}

		@Override
		public int hashCode() {
			int result = getId() != null ? getId().hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			return result;
		}
	}

	@Audited
	@Entity
	@Table(name = "O_ENTITY")
	public static class OtherTestEntity {

		@Id
		private Integer id;
		private String name;

		public OtherTestEntity() {
		}

		public OtherTestEntity(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
