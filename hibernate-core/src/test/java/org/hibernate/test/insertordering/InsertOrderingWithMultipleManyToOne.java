/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11996")
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingWithMultipleManyToOne
		extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( false, false );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Parent.class,
			ChildA.class,
			ChildB.class,
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		settings.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void testBatching() throws SQLException {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent();
			session.persist(parent);

			ChildA childA = new ChildA();
			childA.setParent(parent);
			session.persist(childA);

			ChildB childB = new ChildB();
			childB.setParent(parent);
			session.persist(childB);

			connectionProvider.clear();
		} );

		assertEquals( 3, connectionProvider.getPreparedStatements().size() );
		/*PreparedStatement addressPreparedStatement = connectionProvider.getPreparedStatement(
				"insert into Address (ID) values (?)" );
		verify( addressPreparedStatement, times( 2 ) ).addBatch();
		verify( addressPreparedStatement, times( 1 ) ).executeBatch();
		PreparedStatement personPreparedStatement = connectionProvider.getPreparedStatement(
				"insert into Person (ID) values (?)" );
		verify( personPreparedStatement, times( 4 ) ).addBatch();
		verify( personPreparedStatement, times( 1 ) ).executeBatch();*/
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ChildA")
	public static class ChildA {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "ChildB")
	public static class ChildB {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
