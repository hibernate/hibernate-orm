package org.hibernate.jpa.test.query;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.hql.internal.ast.ErrorTracker;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class UpdateBySimpleNamedQueryTest extends BaseEntityManagerFunctionalTestCase {

	private static Triggerable triggerable;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { TestUser.class };
	}

	@ClassRule
	public static LoggerInspectionRule logInspection = new LoggerInspectionRule(Logger.getMessageLogger(CoreMessageLogger.class, ErrorTracker.class.getName()));

	@BeforeClass
	public static void beforeClass() {
		triggerable = logInspection.watchForLogMessages("<AST>:0:0: unexpected end of subtree");
	}

	@Before
	public void setUp() throws Exception {
		doInJPA(this::entityManagerFactory, entityManager -> {
			TestUser field = new TestUser();
			field.setOnline(true);
			entityManager.persist(field);
		});
	}

	@After
	public void tearDown() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createQuery("delete from TestUser")
					.executeUpdate();
		});
	}

	@TestForIssue("HHH-15257")
	@Test
	public void testNamedQueryOnInheritedTable() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createNamedQuery(TestUser.RESET_ONLINE);
			try {
				int i = query.executeUpdate();
				Assert.assertEquals(1, i);
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		});
		Assert.assertTrue("\"unexpected end of subtree\" errors appeared", triggerable.triggerMessages()
				.isEmpty());
	}

	@Entity(name = "TestUser")
	@NamedQueries({
			//
			@NamedQuery(name = TestUser.RESET_ONLINE, query = "update TestUser x set x.online = false"),
			//
	})
	public static class TestUser extends AbstractEntity {

		private static final long serialVersionUID = 2021_03_01_01L;
		public static final String RESET_ONLINE = "TestUser.resetOnline";

		@Column()
		private Boolean online;

		public TestUser() {
			super();
		}

		public boolean isOnline() {
			if (this.online == null) {
				return false;
			}
			return this.online;
		}

		public void setOnline(boolean online) {
			this.online = online;
		}

	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractEntity implements Serializable, Comparable<AbstractEntity> {

		private static final long serialVersionUID = 2021_07_21_03L;

		private final UUID uuid;

		@Id
		@GenericGenerator(name = "native_generator", strategy = "native", parameters = { @Parameter(name = "sequence_name", value = "hibernate_sequence") })
		@GeneratedValue(generator = "native_generator")
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
				return Objects.hash(this.getClass()
						.toString(), usedId);
			}
			return super.hashCode();
		}

	}
}

