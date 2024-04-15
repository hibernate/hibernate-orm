/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Jan Schatteman
 */
@TestForIssue( jiraKey = "HHH-15257" )
public class NonWhereQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				NonWhereQueryTest.TestUser.class
		};
	}

	@Before
	public void prepareTestData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					TestUser user = new TestUser();
					user.setLoggedIn( true );
					entityManager.persist( user );
				}
		);
	}

	@After
	public void cleanupTestData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.createQuery("delete from TestUser").executeUpdate();
				}
		);
	}

	@Test
	public void testNonWhereQueryOnJoinInheritedTable() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					int i = entityManager.createQuery( "update TestUser x set x.loggedIn = false" ).executeUpdate();
					Assert.assertEquals(1, i);
				}
		);
	}

	@Entity(name = "TestUser")
	public static class TestUser extends AbstractEntity {

		@Column
		private Boolean loggedIn;

		public TestUser() {
			super();
		}

		public boolean isLoggedIn() {
			if (this.loggedIn == null) {
				return false;
			}
			return this.loggedIn;
		}

		public void setLoggedIn(boolean loggedIn) {
			this.loggedIn = loggedIn;
		}
	}

	@Entity(name = "AbstractEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractEntity implements Comparable<AbstractEntity> {

		private final UUID uuid;

		@Id
		@GeneratedValue
		private int id;

		public int getId() {
			return this.id;
		}

		public UUID getUuid() {
			return this.uuid;
		}

		public AbstractEntity() {
			super();
			this.uuid = UUID.randomUUID();
		}

		@Override
		public boolean equals(Object obj) {
			int usedId = this.getId();
			if (usedId > 0) {
				return (obj instanceof AbstractEntity) && (usedId == ((AbstractEntity) obj).getId());
			}
			return super.equals(obj);
		}

		@Override
		public int compareTo(AbstractEntity o) {
			return Integer.compare(this.getId(), o.getId());
		}

		@Override
		public int hashCode() {
			final int usedId = this.getId();
			if (usedId > 0) {
				return Objects.hash( this.getClass().toString(), usedId);
			}
			return super.hashCode();
		}
	}

}
