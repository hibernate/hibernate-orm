/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.list;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.sql.SimpleSelect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to operations on a PersistentList
 *
 * @author Steve Ebersole
 */
public class PersistentListTest extends SessionFactoryBasedFunctionalTest {

	@Override
	public String[] getHbmMappingFiles() {
		return new String[] { "collection/list/Mappings.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5732")
	public void testInverseListIndex() {
		// make sure no one changes the mapping
		final PersistentCollectionDescriptor collectionPersister = sessionFactory().getCollectionPersister( ListOwner.class
																													.getName() + ".children" );
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		ListOwner root = new ListOwner( "root" );
		inTransaction(
				session -> {
					ListOwner child1 = new ListOwner( "c1" );
					root.getChildren().add( child1 );
					child1.setParent( root );
					ListOwner child2 = new ListOwner( "c2" );
					root.getChildren().add( child2 );
					child2.setParent( root );

					session.save( root );
				}
		);

		// now, make sure the list-index column gotten written...
		inTransaction(
				session -> {
					session.doWork(
							work -> {
								PreparedStatement preparedStatement = null;
								ResultSet resultSet = null;
								try {
									preparedStatement = work.prepareStatement(
											"select o.NAME, o.LIST_INDEX from LIST_OWNER o where o.NAME <> ?" );
//								SimpleSelect select = new SimpleSelect( getDialect() )
//										.setTableName( "LIST_OWNER" )
//										.addColumn( "NAME" )
//										.addColumn( "LIST_INDEX" )
//										.addCondition( "NAME", "<>", "?" );
//								PreparedStatement preparedStatement = session.getJdbcCoordinator()
//										.getStatementPreparer()
//										.prepareStatement( select.toStatementString() );
									preparedStatement.setString( 1, "root" );
									preparedStatement.execute();
									resultSet = preparedStatement.getResultSet();
									Map<String, Integer> valueMap = new HashMap<>();
									while ( resultSet.next() ) {
										final String name = resultSet.getString( 1 );
										assertFalse( resultSet.wasNull(), "NAME column was null" );
										final int position = resultSet.getInt( 2 );
										assertFalse( resultSet.wasNull(), "LIST_INDEX column was null" );
										valueMap.put( name, position );
									}
									assertEquals( 2, valueMap.size() );

									// c1 should be list index 0
									assertEquals( Integer.valueOf( 0 ), valueMap.get( "c1" ) );
									// c2 should be list index 1
									assertEquals( Integer.valueOf( 1 ), valueMap.get( "c2" ) );
								}
								finally {
									if ( resultSet != null && !resultSet.isClosed() ) {
										resultSet.close();
									}
									if ( preparedStatement != null && !preparedStatement.isClosed() ) {
										preparedStatement.close();
									}
								}
							}
					);
					session.delete( root );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5732")
	public void testInverseListIndex2() {
		// make sure no one changes the mapping
		final PersistentCollectionDescriptor collectionPersister = sessionFactory().getCollectionPersister( Order.class.getName() + ".lineItems" );
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		Order order = new Order( "acme-1" );
		inTransaction( session -> {
			order.addLineItem( "abc", 2 );
			order.addLineItem( "def", 200 );
			order.addLineItem( "ghi", 13 );
			session.save( order );

		} );

		// now, make sure the list-index column gotten written...
		inTransaction(
				session -> {
					session.doWork(
							work -> {
								PreparedStatement preparedStatement = null;
								ResultSet resultSet = null;
								try {
									SimpleSelect select = new SimpleSelect( getDialect() )
											.setTableName( "T_LINE_ITEM" )
											.addColumn( "ORDER_ID" )
											.addColumn( "INDX" )
											.addColumn( "PRD_CODE" );
									preparedStatement = work.prepareStatement(
											"select ORDER_ID, INDX, PRD_CODE from T_LINE_ITEM" );
									preparedStatement.execute();
									resultSet = preparedStatement.getResultSet();
									Map<String, Integer> valueMap = new HashMap<>();
									while ( resultSet.next() ) {
										final int fk = resultSet.getInt( 1 );
										assertFalse( resultSet.wasNull(), "Collection key (FK) column was null" );
										final int indx = resultSet.getInt( 2 );
										assertFalse( resultSet.wasNull(), "List index column was null" );
										final String prodCode = resultSet.getString( 3 );
										assertFalse( resultSet.wasNull(), "Prod code column was null" );
										valueMap.put( prodCode, indx );
									}
									assertEquals( 3, valueMap.size() );
									assertEquals( Integer.valueOf( 0 ), valueMap.get( "abc" ) );
									assertEquals( Integer.valueOf( 1 ), valueMap.get( "def" ) );
									assertEquals( Integer.valueOf( 2 ), valueMap.get( "ghi" ) );
								}
								finally {
									if ( resultSet != null && !resultSet.isClosed() ) {
										resultSet.close();
									}
									if ( preparedStatement != null && !preparedStatement.isClosed() ) {
										preparedStatement.close();
									}
								}
							}
					);
					session.delete( order );
				}
		);
	}

	@Test
	public void testWriteMethodDirtying() {
		ListOwner parent = new ListOwner( "root" );
		ListOwner child = new ListOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		ListOwner otherChild = new ListOwner( "c2" );

		inTransaction(
				session -> {
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
				}
		);
	}
}
