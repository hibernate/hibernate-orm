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
package org.hibernate.test.collection.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests related to operations on a PersistentList
 *
 * @author Steve Ebersole
 */
public class PersistentListTest extends BaseCoreFunctionalTestCase {
	
	@Override
	public String[] getMappings() {
		return new String[] { "collection/list/Mappings.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5732"  )
	public void testInverseListIndex() {
		// make sure no one changes the mapping
		final CollectionPersister collectionPersister = sessionFactory().getCollectionPersister( ListOwner.class.getName() + ".children" );
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		Session session = openSession();
		session.beginTransaction();

		ListOwner root = new ListOwner( "root" );
		ListOwner child1 = new ListOwner( "c1" );
		root.getChildren().add( child1 );
		child1.setParent( root );
		ListOwner child2 = new ListOwner( "c2" );
		root.getChildren().add( child2 );
		child2.setParent( root );

		session.save( root );
		session.getTransaction().commit();
		session.close();

		// now, make sure the list-index column gotten written...
		final Session session2 = openSession();
		session2.beginTransaction();
		session2.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						final QueryableCollection queryableCollection = (QueryableCollection) collectionPersister;
						SimpleSelect select = new SimpleSelect( getDialect() )
								.setTableName( queryableCollection.getTableName() )
								.addColumn( "NAME" )
								.addColumn( "LIST_INDEX" )
								.addCondition( "NAME", "<>", "?" );
						PreparedStatement preparedStatement = ((SessionImplementor)session2).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( select.toStatementString() );
						preparedStatement.setString( 1, "root" );
						ResultSet resultSet = ((SessionImplementor)session2).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( preparedStatement );
						Map<String, Integer> valueMap = new HashMap<String, Integer>();
						while ( resultSet.next() ) {
							final String name = resultSet.getString( 1 );
							assertFalse( "NAME column was null", resultSet.wasNull() );
							final int position = resultSet.getInt( 2 );
							assertFalse( "LIST_INDEX column was null", resultSet.wasNull() );
							valueMap.put( name, position );
						}
						assertEquals( 2, valueMap.size() );

						// c1 should be list index 0
						assertEquals( Integer.valueOf( 0 ), valueMap.get( "c1" ) );
						// c2 should be list index 1
						assertEquals( Integer.valueOf( 1 ), valueMap.get( "c2" ) );
					}
				}
		);
		session2.delete( root );
		session2.getTransaction().commit();
		session2.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5732"  )
	public void testInverseListIndex2() {
		// make sure no one changes the mapping
		final CollectionPersister collectionPersister = sessionFactory().getCollectionPersister( Order.class.getName() + ".lineItems" );
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		Session session = openSession();
		session.beginTransaction();

		Order order = new Order( "acme-1" );
		order.addLineItem( "abc", 2 );
		order.addLineItem( "def", 200 );
		order.addLineItem( "ghi", 13 );
		session.save( order );
		session.getTransaction().commit();
		session.close();

		// now, make sure the list-index column gotten written...
		final Session session2 = openSession();
		session2.beginTransaction();
		session2.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						final QueryableCollection queryableCollection = (QueryableCollection) collectionPersister;
						SimpleSelect select = new SimpleSelect( getDialect() )
								.setTableName( queryableCollection.getTableName() )
								.addColumn( "ORDER_ID" )
								.addColumn( "INDX" )
								.addColumn( "PRD_CODE" );
						PreparedStatement preparedStatement = ((SessionImplementor)session2).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( select.toStatementString() );
						ResultSet resultSet = ((SessionImplementor)session2).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( preparedStatement );
						Map<String, Integer> valueMap = new HashMap<String, Integer>();
						while ( resultSet.next() ) {
							final int fk = resultSet.getInt( 1 );
							assertFalse( "Collection key (FK) column was null", resultSet.wasNull() );
							final int indx = resultSet.getInt( 2 );
							assertFalse( "List index column was null", resultSet.wasNull() );
							final String prodCode = resultSet.getString( 3 );
							assertFalse( "Prod code column was null", resultSet.wasNull() );
							valueMap.put( prodCode, indx );
						}
						assertEquals( 3, valueMap.size() );
						assertEquals( Integer.valueOf( 0 ), valueMap.get( "abc" ) );
						assertEquals( Integer.valueOf( 1 ), valueMap.get( "def" ) );
						assertEquals( Integer.valueOf( 2 ), valueMap.get( "ghi" ) );
					}
				}
		);
		session2.delete( order );
		session2.getTransaction().commit();
		session2.close();
	}

	@Test
	public void testWriteMethodDirtying() {
		ListOwner parent = new ListOwner( "root" );
		ListOwner child = new ListOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		ListOwner otherChild = new ListOwner( "c2" );

		Session session = openSession();
		session.beginTransaction();
		session.save( parent );
		session.flush();
		// at this point, the list on parent has now been replaced with a PersistentList...
		PersistentList children = (PersistentList) parent.getChildren();

		assertFalse( children.remove( otherChild ) );
		assertFalse( children.isDirty() );

		ArrayList otherCollection = new ArrayList();
		otherCollection.add( child );
		assertFalse( children.retainAll( otherCollection ) );
		assertFalse( children.isDirty() );

		otherCollection = new ArrayList();
		otherCollection.add( otherChild );
		assertFalse( children.removeAll( otherCollection ) );
		assertFalse( children.isDirty() );

		children.clear();
		session.delete( child );
		assertTrue( children.isDirty() );

		session.flush();

		children.clear();
		assertFalse( children.isDirty() );

		session.delete( parent );
		session.getTransaction().commit();
		session.close();
	}
}
