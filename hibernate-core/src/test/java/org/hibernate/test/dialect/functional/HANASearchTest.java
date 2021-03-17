/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.HANACloudColumnStoreDialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests the correctness of the SAP HANA fulltext-search functions.
 * 
 * @author Jonathan Bregler
 */
@RequiresDialect(value = { HANAColumnStoreDialect.class })
@SkipForDialect(value = HANACloudColumnStoreDialect.class)
public class HANASearchTest extends BaseCoreFunctionalTestCase {

	private static final String ENTITY_NAME = "SearchEntity";

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection.prepareStatement( "CREATE COLUMN TABLE " + ENTITY_NAME
						+ " (key INTEGER, t TEXT, c NVARCHAR(255), PRIMARY KEY (key))" ) ) {
					ps.execute();
				}
				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE FULLTEXT INDEX FTI ON " + ENTITY_NAME + " (c)" ) ) {
					ps.execute();
				}
			} );
		} );
	}

	@Override
	protected void cleanupTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection.prepareStatement( "DROP TABLE " + ENTITY_NAME + " CASCADE" ) ) {
					ps.execute();
				}
				catch (Exception e) {
					// Ignore
				}
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13021")
	public void testTextType() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			SearchEntity entity = new SearchEntity();
			entity.key = Integer.valueOf( 1 );
			entity.t = "TEST TEXT";
			entity.c = "TEST STRING";

			s.persist( entity );

			s.flush();

			Query<Object[]> legacyQuery = s.createQuery( "select b, snippets(t), highlighted(t), score() from "
					+ ENTITY_NAME + " b where contains(b.t, 'text') = contains_rhs()", Object[].class );

			Object[] result = legacyQuery.getSingleResult();

			SearchEntity retrievedEntity = (SearchEntity) result[0];

			assertEquals( 4, result.length );

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertEquals( "TEST TEXT", retrievedEntity.t );
			assertEquals( "TEST STRING", retrievedEntity.c );

			assertEquals( "TEST <b>TEXT</b>", result[1] );
			assertEquals( "TEST <b>TEXT</b>", result[2] );
			assertEquals( 0.75d, result[3] );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13021")
	public void testTextTypeFalse() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			SearchEntity entity = new SearchEntity();
			entity.key = Integer.valueOf( 1 );
			entity.t = "TEST TEXT";
			entity.c = "TEST STRING";

			s.persist( entity );

			s.flush();

			Query<Object[]> legacyQuery = s.createQuery( "select b, snippets(t), highlighted(t), score() from " + ENTITY_NAME
					+ " b where not_contains(b.t, 'string') = contains_rhs()", Object[].class );

			Object[] result = legacyQuery.getSingleResult();

			SearchEntity retrievedEntity = (SearchEntity) result[0];

			assertEquals( 4, result.length );

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertEquals( "TEST TEXT", retrievedEntity.t );
			assertEquals( "TEST STRING", retrievedEntity.c );

			assertEquals( "TEST TEXT", result[1] );
			assertEquals( "TEST TEXT", result[2] );
			assertEquals( 1d, result[3] );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13021")
	public void testCharType() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			SearchEntity entity = new SearchEntity();
			entity.key = Integer.valueOf( 1 );
			entity.t = "TEST TEXT";
			entity.c = "TEST STRING";

			s.persist( entity );

			s.getTransaction().commit();
			s.beginTransaction();

			Query<Object[]> legacyQuery = s.createQuery( "select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
					+ " b where contains(b.c, 'string') = contains_rhs()", Object[].class );

			Object[] result = legacyQuery.getSingleResult();

			SearchEntity retrievedEntity = (SearchEntity) result[0];

			assertEquals( 4, result.length );

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertEquals( "TEST TEXT", retrievedEntity.t );
			assertEquals( "TEST STRING", retrievedEntity.c );

			assertEquals( "TEST <b>STRING</b>", result[1] );
			assertEquals( "TEST <b>STRING</b>", result[2] );
			assertEquals( 0.75d, result[3] );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13021")
	public void testCharTypeComplexQuery() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			SearchEntity entity = new SearchEntity();
			entity.key = Integer.valueOf( 1 );
			entity.t = "TEST TEXT";
			entity.c = "TEST STRING";

			s.persist( entity );

			s.flush();

			s.getTransaction().commit();
			s.beginTransaction();

			Query<Object[]> legacyQuery = s.createQuery(
					"select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
							+ " b where contains(b.c, 'string') = contains_rhs() and key=1 and score() > 0.5",
					Object[].class );

			Object[] result = legacyQuery.getSingleResult();

			SearchEntity retrievedEntity = (SearchEntity) result[0];

			assertEquals( 4, result.length );

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertEquals( "TEST TEXT", retrievedEntity.t );
			assertEquals( "TEST STRING", retrievedEntity.c );

			assertEquals( "TEST <b>STRING</b>", result[1] );
			assertEquals( "TEST <b>STRING</b>", result[2] );
			assertEquals( 0.75d, result[3] );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13021")
	public void testFuzzy() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			SearchEntity entity = new SearchEntity();
			entity.key = Integer.valueOf( 1 );
			entity.t = "TEST TEXT";
			entity.c = "TEST STRING";

			s.persist( entity );

			s.flush();

			s.getTransaction().commit();
			s.beginTransaction();

			Query<Object[]> legacyQuery = s.createQuery( "select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
					+ " b where contains(b.c, 'string', FUZZY(0.7)) = contains_rhs()", Object[].class );

			Object[] result = legacyQuery.getSingleResult();

			SearchEntity retrievedEntity = (SearchEntity) result[0];

			assertEquals( 4, result.length );

			assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
			assertEquals( "TEST TEXT", retrievedEntity.t );
			assertEquals( "TEST STRING", retrievedEntity.c );

			assertEquals( "TEST <b>STRING</b>", result[1] );
			assertEquals( "TEST <b>STRING</b>", result[2] );
			assertEquals( 0.75d, result[3] );
		} );
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[]{ SearchEntity.class };
	}

	@Entity(name = ENTITY_NAME)
	public static class SearchEntity {

		@Id
		public Integer key;

		public String t;

		public String c;
	}

}
