/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11580")
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class DeleteCollectionJtaSessionClosedBeforeCommitTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private static final int ENTITY_ID = 1;
	private static final int OTHER_ENTITY_ID = 2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {TestEntity.class, OtherTestEntity.class};
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() throws Exception {
		// Transaction 1
		inJtaTransaction(
				entityManager -> {
					final TestEntity entity = new TestEntity( ENTITY_ID, "Fab" );
					entityManager.persist( entity );

					final OtherTestEntity other = new OtherTestEntity( OTHER_ENTITY_ID, "other" );
					entity.addOther( other );

					entityManager.persist( entity );
					entityManager.persist( other );
				}
		);

		// Transaction 2
		inJtaTransaction(
				entityManager -> {
					final TestEntity entity = entityManager.find( TestEntity.class, ENTITY_ID );
					final OtherTestEntity other = entityManager.find( OtherTestEntity.class, OTHER_ENTITY_ID );
					entityManager.remove( entity );
					entityManager.remove( other );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( TestEntity.class, ENTITY_ID ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		assertThat( getAuditReader().find( TestEntity.class, ENTITY_ID, 1 ), equalTo( new TestEntity( 1, "Fab" ) ) );
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
