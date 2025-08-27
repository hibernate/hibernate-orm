/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.List;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.ComparisonRestriction;
import org.hibernate.sql.SimpleSelect;

import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to operations on a PersistentList
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/collection/list/Mappings.xml")
@SessionFactory
public class PersistentListTest {

	@Test
	void checkListIndexBase(DomainModelScope modelScope) {
		final PersistentClass listOwnerBinding = modelScope.getEntityBinding( ListOwner.class );
		final List childrenMapping = (List) listOwnerBinding.getProperty( "children" ).getValue();
		assertThat( childrenMapping.getBaseIndex() ).isEqualTo( 1 );

		final PersistentClass orderBinding = modelScope.getEntityBinding( Order.class );
		final List lineItemsMapping = (List) orderBinding.getProperty( "lineItems" ).getValue();
		assertThat( lineItemsMapping.getBaseIndex() ).isEqualTo( 1 );
	}

	@Test
	@JiraKey(value = "HHH-5732")
	public void testInverseListIndex(SessionFactoryScope scope) {
		// make sure no one changes the mapping
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final CollectionPersister collectionPersister = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor(ListOwner.class.getName() + ".children");
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		ListOwner root = new ListOwner( "root" );
		scope.inTransaction(
				session -> {
					ListOwner child1 = new ListOwner( "c1" );
					root.getChildren().add( child1 );
					child1.setParent( root );
					ListOwner child2 = new ListOwner( "c2" );
					root.getChildren().add( child2 );
					child2.setParent( root );

					session.persist( root );
				}
		);


		// now, make sure the list-index column gotten written...
		scope.inTransaction(
				session2 -> {
					session2.doWork(
							connection -> {
								SimpleSelect select = new SimpleSelect( sessionFactory )
										.setTableName( collectionPersister.getTableName() )
										.addColumn( "name" )
										.addColumn( "list_index" )
										.addRestriction( "name", ComparisonRestriction.Operator.NE, "?" );
								final String sql = select.toStatementString();
								PreparedStatement preparedStatement = session2.getJdbcCoordinator()										.getStatementPreparer()
										.prepareStatement( sql );
								preparedStatement.setString( 1, "root" );
								ResultSet resultSet = session2.getJdbcCoordinator()
										.getResultSetReturn()
										.extract( preparedStatement, sql );
								Map<String, Integer> valueMap = new HashMap<String, Integer>();
								while ( resultSet.next() ) {
									final String name = resultSet.getString( 1 );
									assertFalse( "`name` column was null", resultSet.wasNull() );
									final int position = resultSet.getInt( 2 );
									assertFalse( "`list_index` column was null", resultSet.wasNull() );
									valueMap.put( name, position );
								}
								assertEquals( 2, valueMap.size() );
								// account for list-index-base = 1
								// c1 should be list index 1 (0+1)
								assertEquals( Integer.valueOf( 1 ), valueMap.get( "c1" ) );
								// c2 should be list index 2 (1+1)_
								assertEquals( Integer.valueOf( 2 ), valueMap.get( "c2" ) );
							}
					);
					session2.remove( root );

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5732")
	public void testInverseListIndex2(SessionFactoryScope scope) {
		// make sure no one changes the mapping
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final CollectionPersister collectionPersister = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor(Order.class.getName() + ".lineItems");
		assertTrue( collectionPersister.isInverse() );

		// do some creations...
		Order order = new Order( "acme-1" );
		scope.inTransaction(
				session -> {
					order.addLineItem( "abc", 2 );
					order.addLineItem( "def", 200 );
					order.addLineItem( "ghi", 13 );
					session.persist( order );
				}
		);

		// now, make sure the list-index column gotten written...
		scope.inTransaction(
				session2 -> {
					session2.doWork(
							connection -> {
								SimpleSelect select = new SimpleSelect( sessionFactory )
										.setTableName( collectionPersister.getTableName() )
										.addColumn( "order_fk" )
										.addColumn( "list_index" )
										.addColumn( "prod_code" );
								final String sql = select.toStatementString();
								PreparedStatement preparedStatement = ( (SessionImplementor) session2 ).getJdbcCoordinator()
										.getStatementPreparer()
										.prepareStatement( sql );
								ResultSet resultSet = session2.getJdbcCoordinator()
										.getResultSetReturn()
										.extract( preparedStatement, sql );
								Map<String, Integer> valueMap = new HashMap<>();
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
								// account for list-index-base = 1
								assertEquals( Integer.valueOf( 1 ), valueMap.get( "abc" ) );
								assertEquals( Integer.valueOf( 2 ), valueMap.get( "def" ) );
								assertEquals( Integer.valueOf( 3 ), valueMap.get( "ghi" ) );
							}
					);
					session2.remove( order );
				}
		);
	}

	@Test
	public void testWriteMethodDirtying(SessionFactoryScope scope) {
		ListOwner parent = new ListOwner( "root" );
		ListOwner child = new ListOwner( "c1" );
		parent.getChildren().add( child );
		child.setParent( parent );
		ListOwner otherChild = new ListOwner( "c2" );

		scope.inTransaction(
				session -> {
					session.persist( parent );
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
					session.remove( child );
					assertTrue( children.isDirty() );

					session.flush();

					children.clear();
					assertFalse( children.isDirty() );

					session.remove( parent );
				}
		);
	}
}
