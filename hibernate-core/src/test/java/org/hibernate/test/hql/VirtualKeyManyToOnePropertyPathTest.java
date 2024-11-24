/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

/**
 * @author Christian Beikov
 */
public class VirtualKeyManyToOnePropertyPathTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Item.class, OrderItem.class};
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15051")
	public void tstPropertyPathVirtualIdOfKeyManyToOneProducesNoJoin() {
		doInHibernate( this::sessionFactory, session -> {
			sqlStatementInterceptor.clear();
			session.createQuery( "SELECT o.item.id1 FROM OrderItem o", Long.class ).getResultList();
			sqlStatementInterceptor.assertExecutedCount( 1 );
			assertFalse( sqlStatementInterceptor.getSqlQueries().get( 0 ).contains( " join " ) );
		} );
	}

	@Entity(name = "Item")
	public static class Item implements Serializable {
		@Id
		Long id1;
		@Id
		Long id2;

		public Item() {
		}

	}

	@Entity(name = "OrderItem")
	public static class OrderItem implements Serializable {
		@Id
		long id;
		@Id
		@ManyToOne
		Item item;

		public OrderItem() {
		}
	}
}
