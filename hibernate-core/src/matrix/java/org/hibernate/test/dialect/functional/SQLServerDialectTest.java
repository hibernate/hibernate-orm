/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express
  * copyright attribution statements applied by the authors.
  * All third-party contributions are distributed under license by
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to
  * use, modify, copy, or redistribute it subject to the terms and
  * conditions of the GNU Lesser General Public License, as published
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this distribution; if not, write to:
  *
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */
package org.hibernate.test.dialect.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Guenther Demetz
 */
public class SQLServerDialectTest extends BaseCoreFunctionalTestCase {
	
	@TestForIssue(jiraKey = "HHH-7198")
    @Test
    @RequiresDialect(value = {SQLServer2005Dialect.class})
    public void testMaxResultsSqlServerWithCaseSensitiveCollation() throws Exception {
        
        Session s = openSession();
        Connection conn = ((SessionFactoryImpl) s.getSessionFactory()).getConnectionProvider().getConnection();
        String databaseName = conn.getCatalog();
        conn.close();
        s.createSQLQuery("ALTER DATABASE " + databaseName + " COLLATE Latin1_General_CS_AS").executeUpdate();

        Transaction tx = s.beginTransaction();

        for (int i=1; i <= 20;i++) { 
	        Product kit = new Product();
	        kit.id = i;
	        kit.description = "Kit" + i;
	        s.persist(kit);
        }
        s.flush();
        s.clear();

        List list = s.createQuery("from Product where description like 'Kit%'").setFirstResult(2).setMaxResults(2).list(); 
        // without patch this query produces following sql (Note that the tablename as well as the like condition have turned into lowercase)"
        // WITH query AS (select product0_.id as id0_, product0_.description as descript2_0_, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ from product product0_ where product0_.description like 'kit%') SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?
        
        
        // this leads to following exception:
//        org.hibernate.exception.SQLGrammarException: Invalid object name 'product'.
//    	at org.hibernate.exception.internal.SQLStateConversionDelegate.convert(SQLStateConversionDelegate.java:122)
//    	at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:49)
//    	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:125)
//    	at org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:110)
//    	at org.hibernate.engine.jdbc.internal.proxy.AbstractStatementProxyHandler.continueInvocation(AbstractStatementProxyHandler.java:130)
//    	at org.hibernate.engine.jdbc.internal.proxy.AbstractProxyHandler.invoke(AbstractProxyHandler.java:81)
//    	at $Proxy18.executeQuery(Unknown Source)
//    	at org.hibernate.loader.Loader.getResultSet(Loader.java:1953)
//    	...
//    Caused by: com.microsoft.sqlserver.jdbc.SQLServerException: Invalid object name 'product'.
//    	at com.microsoft.sqlserver.jdbc.SQLServerException.makeFromDatabaseError(SQLServerException.java:197)
//    	at com.microsoft.sqlserver.jdbc.SQLServerStatement.getNextResult(SQLServerStatement.java:1493)
//    	at com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement.doExecutePreparedStatement(SQLServerPreparedStatement.java:390)
//    	at com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement$PrepStmtExecCmd.doExecute(SQLServerPreparedStatement.java:340)
//    	at com.microsoft.sqlserver.jdbc.TDSCommand.execute(IOBuffer.java:4575)
//    	at com.microsoft.sqlserver.jdbc.SQLServerConnection.executeCommand(SQLServerConnection.java:1400)
//    	at com.microsoft.sqlserver.jdbc.SQLServerStatement.executeCommand(SQLServerStatement.java:179)
//    	at com.microsoft.sqlserver.jdbc.SQLServerStatement.executeStatement(SQLServerStatement.java:154)
//    	at com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement.executeQuery(SQLServerPreparedStatement.java:283)
//    	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//    	at sun.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
//    	at sun.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
//    	at java.lang.reflect.Method.invoke(Unknown Source)
//    	at org.hibernate.engine.jdbc.internal.proxy.AbstractStatementProxyHandler.continueInvocation(AbstractStatementProxyHandler.java:122)
//    	... 44 more


        
        assertEquals(2,list.size());
        tx.rollback();
        s.close();
    }
	
	
	@TestForIssue(jiraKey = "HHH-3961")
	@Test
	@RequiresDialect(value = { SQLServer2005Dialect.class })
	public void testLockNowaitSqlServer() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		final Product kit = new Product();
		kit.id = 4000;
		kit.description="m";
		s.persist(kit);
		s.getTransaction().commit();
		final Transaction tx = s.beginTransaction();
		kit.description="change!";
		s.flush(); // creates write lock on kit until we end the transaction
		
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				tx.commit();
			}});
		
		
		Session s2 = openSession();
		
		Transaction tx2 = s2.beginTransaction();
		//s2.createSQLQuery("SET LOCK_TIMEOUT 5000;Select @@LOCK_TIMEOUT;").uniqueResult(); strangely this is useless
		
		Product kit2= (Product) s2.byId(Product.class).load(kit.id);
		LockOptions opt = new LockOptions(LockMode.UPGRADE_NOWAIT);
		opt.setTimeOut(0); // seems useless
		long start = System.currentTimeMillis();
		t.start();
		try {
			s2.buildLockRequest(opt).lock(kit2);
		}
		catch (SQLGrammarException e) {
			// OK
		}
		long end = System.currentTimeMillis();
		t.join();
		long differenceInMillisecs =  end-start;
		assertTrue("Lock NoWait blocked for " + differenceInMillisecs + " ms, this is definitely to much for Nowait", differenceInMillisecs < 2000);
		
		s2.getTransaction().rollback();
		s.getTransaction().begin();
		s.delete(kit);
		s.getTransaction().commit();

	
	}

	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[] {
				Product.class
		};
	}

}
