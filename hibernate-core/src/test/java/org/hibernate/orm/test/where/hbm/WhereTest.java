/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import java.util.HashSet;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Max Rydahl Andersen
 */
public class WhereTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "where/hbm/File.hbm.xml" };
	}

	@Before
	public void createTestData() {
		inTransaction(
				s -> {
					// `parent` has no parent
					// `deleted parent` has no parent
					// `child` has `parent` for parent
					// `deleted child` has `parent` for parent
					File parent = new File("parent", null);
					s.persist( parent );
					s.persist( new File("child", parent) );
					File deletedChild = new File("deleted child", parent);
					deletedChild.setDeleted(true);
					s.persist( deletedChild );
					File deletedParent = new File("deleted parent", null);
					deletedParent.setDeleted(true);
					s.persist( deletedParent );
				}
		);
	}

	@After
	public void removeTestData() {
		inTransaction(
				s -> {
					s.createNativeQuery( "update T_FILE set parent = null" ).executeUpdate();
					s.createNativeQuery( "delete from T_FILE" ).executeUpdate();
				}
		);
	}

	@Test
	public void testHql() {
		inTransaction(
				s -> {
					File parent = s.createQuery("from File f where f.id = 4", File.class )
							.uniqueResult();

					assertNull( parent );
				}
		);
	}

	@Test
	public void testHqlWithFetch() {
		inTransaction(
				s -> {
					final List<File> files = s.createQuery(
							"from File f left join fetch f.children where f.parent is null",
							File.class
					).list();
					final HashSet<File> filesSet = new HashSet<>( files );
					assertEquals( 1, filesSet.size() );
					File parent = files.get( 0 );
					assertEquals( 1, parent.getChildren().size() );
				}
		);
	}

	@Test
	public void testCriteria() {
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<File> criteria = criteriaBuilder.createQuery( File.class );
					Root<File> root = criteria.from( File.class );
					root.fetch( "children", JoinType.LEFT );
					criteria.where( criteriaBuilder.isNull( root.get("parent") ));
					File parent = s.createQuery( criteria ).uniqueResult();
					assertEquals( parent.getChildren().size(), 1 );
					assertEquals(  1, parent.getChildren().size() );
				}
		);
	}

	@Test
	public void testNativeQuery() {
		inTransaction(
				s -> {
					final NativeQuery query = s.createNativeQuery(
							"select {f.*}, {c.*} from T_FILE f left join T_FILE c on f.id = c.parent where f.parent is null" )
							.addEntity( "f", File.class );
					query.addFetch( "c", "f", "children" );

					File parent = (File) query.list().get( 0 );
					// @Where should not be applied
					assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
					assertEquals( 2, parent.getChildren().size() );
				}
		);
	}
}
