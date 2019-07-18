/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.annotations.onetomany;

import java.util.Iterator;

import org.hibernate.NullPrecedence;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServer2008Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inSession;

/**
 * @author Andrea Boriero
 */
public class OerderByNullsFirstLastTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-465")
	@RequiresDialect(value = { H2Dialect.class, MySQLDialect.class, SQLServer2008Dialect.class },
			comment = "By default H2 places NULL values first, so testing 'NULLS LAST' expression. " +
					"For MySQL and SQL Server 2008 testing overridden Dialect#renderOrderByElement(String, String, String, NullPrecedence) method. " +
					"MySQL and SQL Server 2008 does not support NULLS FIRST / LAST syntax at the moment, so transforming the expression to 'CASE WHEN ...'.")
	public void testCriteriaNullsFirstLast() {
		inSession( session -> {
			try{
				// Populating database with test data.
				session.getTransaction().begin();
				Zoo zoo1 = new Zoo( null );
				Zoo zoo2 = new Zoo( "Warsaw ZOO" );
				session.persist( zoo1 );
				session.persist( zoo2 );
				session.getTransaction().commit();

				session.clear();

				session.getTransaction().begin();

				Criteria criteria = session.createCriteria( Zoo.class );
				criteria.addOrder( org.hibernate.criterion.Order.asc( "name" ).nulls( NullPrecedence.LAST ) );
				Iterator<Zoo> iterator = (Iterator<Zoo>) criteria.list().iterator();

				Assert.assertEquals( zoo2.getName(), iterator.next().getName() );
				Assert.assertNull( iterator.next().getName() );
				session.getTransaction().commit();

				session.clear();

				// Cleanup data.
				session.getTransaction().begin();
				session.delete( zoo1 );
				session.delete( zoo2 );
				session.getTransaction().commit();
			}
			catch (Exception e){
				if(session.getTransaction().isActive()){
					session.getTransaction().rollback();
				}
				throw e;
			}
		} );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Order.class, OrderItem.class, Zoo.class, Tiger.class,
				Monkey.class, Visitor.class, Box.class, Item.class,
				BankAccount.class, Transaction.class,
				Comment.class, Forum.class, Post.class, User.class,
				Asset.class, Computer.class, Employee.class,
				A.class, B.class, C.class
		};
	}
}
