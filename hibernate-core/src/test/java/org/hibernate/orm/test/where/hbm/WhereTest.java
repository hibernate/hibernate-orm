/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.hbm;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Max Rydahl Andersen
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "hbm/where/File.hbm.xml")
@SessionFactory
public class WhereTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		// `parent` has no parent
		// `deleted parent` has no parent
		// `child` has `parent` for parent
		// `deleted child` has `parent` for parent
		factoryScope.inTransaction(s -> {
			File parent = new File("parent", null);
			s.persist( parent );
			s.persist( new File("child", parent) );
			File deletedChild = new File("deleted child", parent);
			deletedChild.setDeleted(true);
			s.persist( deletedChild );
			File deletedParent = new File("deleted parent", null);
			deletedParent.setDeleted(true);
			s.persist( deletedParent );
		} );
	}

	@AfterEach
	public void removeTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			var parent = s.createQuery("from File f where f.id = 4", File.class ).uniqueResult();
			assertNull( parent );
		} );
	}

	@Test
	public void testHqlWithFetch(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			final List<File> files = s.createQuery(
					"from File f left join fetch f.children where f.parent is null",
					File.class
			).list();
			assertEquals( 1, files.size() );
			File parent = files.get( 0 );
			assertEquals( 1, parent.getChildren().size() );
		} );
	}

	@Test
	public void testCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<File> criteria = criteriaBuilder.createQuery( File.class );
			Root<File> root = criteria.from( File.class );
			root.fetch( "children", JoinType.LEFT );
			criteria.where( criteriaBuilder.isNull( root.get("parent") ));
			File parent = s.createQuery( criteria ).uniqueResult();
			assertEquals( parent.getChildren().size(), 1 );
			assertEquals(  1, parent.getChildren().size() );
		} );
	}

	@Test
	public void testNativeQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			final NativeQuery query = s.createNativeQuery(
					"select {f.*}, {c.*} from T_FILE f left join T_FILE c on f.id = c.parent where f.parent is null" )
					.addEntity( "f", File.class );
			query.addFetch( "c", "f", "children" );

			File parent = (File) query.list().get( 0 );
			// @Where should not be applied
			assertTrue( Hibernate.isInitialized( parent.getChildren() ) );
			assertEquals( 2, parent.getChildren().size() );
		} );
	}
}
