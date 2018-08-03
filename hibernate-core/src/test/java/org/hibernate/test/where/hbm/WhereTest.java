/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.where.hbm;

import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.test.where.hbm.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Max Rydahl Andersen
 */
public class WhereTest extends BaseCoreFunctionalTestCase {
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
					s.createQuery( "update File f set f.parent = null" ).executeUpdate();
					s.createQuery( "delete File f" ).executeUpdate();
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
					assertEquals( parent.getChildren().size(), 1 );
				}
		);
	}

	@Test
	public void testCriteria() {
		inTransaction(
				s -> {
					File parent = (File) s.createCriteria( File.class )
							.setFetchMode( "children", FetchMode.JOIN )
							.add( Restrictions.isNull( "parent" ) )
							.uniqueResult();
					assertEquals( parent.getChildren().size(), 1 );
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

					File parent = (File) ( (Object[]) query.list().get( 0 ) )[0];
					// @Where should not be applied
					assertEquals( parent.getChildren().size(), 2 );
				}
		);
	}
}

