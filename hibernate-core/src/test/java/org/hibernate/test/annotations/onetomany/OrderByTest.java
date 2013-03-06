/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.onetomany;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Brett Meyer
 */
public class OrderByTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Order.class, OrderItem.class, Box.class, Item.class,
				BankAccount.class, Transaction.class};
	}
		
	@Test
	public void testOrderByOnIdClassProperties() throws Exception {
		Session s = openSession( );
		s.getTransaction().begin();
		Order o = new Order();
		o.setAcademicYear( 2000 );
		o.setSchoolId( "Supelec" );
		o.setSchoolIdSort( 1 );
		s.persist( o );
		OrderItem oi1 = new OrderItem();
		oi1.setAcademicYear( 2000 );
		oi1.setDayName( "Monday" );
		oi1.setSchoolId( "Supelec" );
		oi1.setOrder( o );
		oi1.setDayNo( 23 );
		s.persist( oi1 );
		OrderItem oi2 = new OrderItem();
		oi2.setAcademicYear( 2000 );
		oi2.setDayName( "Tuesday" );
		oi2.setSchoolId( "Supelec" );
		oi2.setOrder( o );
		oi2.setDayNo( 30 );
		s.persist( oi2 );
		s.flush();
		s.clear();

		OrderID oid = new OrderID();
		oid.setAcademicYear( 2000 );
		oid.setSchoolId( "Supelec" );
		o = (Order) s.get( Order.class, oid );
		assertEquals( 30, o.getItemList().get( 0 ).getDayNo().intValue() );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7608" )
	@RequiresDialect({ H2Dialect.class, Oracle8iDialect.class })
	public void testOrderByReferencingFormulaColumn() {
		Session session = openSession();

		// Populating database with test data.
		session.getTransaction().begin();
		Box box1 = new Box( 1 );
		Item item1 = new Item( 1, "1", box1 );
		Item item2 = new Item( 2, "22", box1 );
		Item item3 = new Item( 3, "2", box1 );
		session.persist( box1 );
		session.persist( item1 );
		session.persist( item2 );
		session.persist( item3 );
		session.flush();
		session.refresh( item1 );
		session.refresh( item2 );
		session.refresh( item3 );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		box1 = (Box) session.get( Box.class, box1.getId() );
		Assert.assertEquals( Arrays.asList( item2, item1, item3 ), box1.getItems() );
		session.getTransaction().commit();

		session.clear();

		// Cleanup data.
		session.getTransaction().begin();
		session.delete( item1 );
		session.delete( item2 );
		session.delete( item3 );
		session.delete( box1 );
		session.getTransaction().commit();

		session.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-5732")
	public void testInverseIndex() {
		final CollectionPersister transactionsPersister = sessionFactory().getCollectionPersister(
				BankAccount.class.getName() + ".transactions" );
		assertTrue( transactionsPersister.isInverse() );

		Session s = openSession();
		s.getTransaction().begin();

		BankAccount account = new BankAccount();
		account.addTransaction( "zzzzz" );
		account.addTransaction( "aaaaa" );
		account.addTransaction( "mmmmm" );
		s.save( account );
		s.getTransaction().commit();

		s.close();

		s = openSession();
		s.getTransaction().begin();
		
		try {
			final QueryableCollection queryableCollection = (QueryableCollection) transactionsPersister;
			SimpleSelect select = new SimpleSelect( getDialect() )
					.setTableName( queryableCollection.getTableName() )
					.addColumn( "code" )
					.addColumn( "transactions_index" );
			PreparedStatement preparedStatement = ((SessionImplementor)s).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( select.toStatementString() );
			ResultSet resultSet = preparedStatement.executeQuery();
			Map<Integer, String> valueMap = new HashMap<Integer, String>();
			while ( resultSet.next() ) {
				final String code = resultSet.getString( 1 );
				assertFalse( "code column was null", resultSet.wasNull() );
				final int indx = resultSet.getInt( 2 );
				assertFalse( "List index column was null", resultSet.wasNull() );
				valueMap.put( indx, code );
			}
			assertEquals( 3, valueMap.size() );
			assertEquals( "zzzzz", valueMap.get( 0 ) );
			assertEquals( "aaaaa", valueMap.get( 1 ) );
			assertEquals( "mmmmm", valueMap.get( 2 ) );
		}
		catch ( SQLException e ) {
			fail(e.getMessage());
		}
	}
}
