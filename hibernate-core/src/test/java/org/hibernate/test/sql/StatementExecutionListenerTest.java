package org.hibernate.test.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.jdbc.SQLStatementExecutionListener;
import org.hibernate.testing.jdbc.SQLStatementExecutionListener.StatementExecution;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;


public class StatementExecutionListenerTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementExecutionListener sqlStatementInterceptor;

	@Override
	protected void addSettings(Map settings) {
		sqlStatementInterceptor = new SQLStatementExecutionListener( settings );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Test
	public void testStatementExecuted() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		long beforeExecution = System.nanoTime();
		MyEntity entity = new MyEntity();
		s.persist( entity );
		tx.commit();
		s.close();
		long afterExecution = System.nanoTime();

		List<StatementExecution> statementExecutions = sqlStatementInterceptor.getStatementExecutions();

		assertFalse( statementExecutions.isEmpty() );

		for ( StatementExecution statementExecution : statementExecutions ) {
			long startTimeNanos = statementExecution.getStartTimeNanos();
			assertTrue( startTimeNanos >= beforeExecution );
			assertTrue( startTimeNanos <= afterExecution );
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

}
