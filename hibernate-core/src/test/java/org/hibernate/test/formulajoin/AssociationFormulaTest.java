/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.formulajoin;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-9952")
public class AssociationFormulaTest extends BaseCoreFunctionalTestCase {

	public AssociationFormulaTest() {
		super();
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@Override
	public String[] getMappings() {
		return new String[] { "formulajoin/Mapping.hbm.xml" };
	}

	@Before
	public void fillDb() {

		Entity entity = new Entity();
		entity.setId( new Id( "test", 1 ) );
		entity.setOther( new OtherEntity() );
		entity.getOther().setId( new Id( "test", 2 ) );

		Entity otherNull = new Entity();
		otherNull.setId( new Id( "null", 3 ) );

		inTransaction(
				session -> {
					session.merge( entity );
					session.merge( otherNull );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Entity" ).executeUpdate();
					session.createQuery( "delete from OtherEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testJoin() {

		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "select e from Entity e inner join e.other o" )
							.uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 1, loaded.getId().getId() );
					assertEquals( 2, loaded.getOther().getId().getId() );
					assertFalse( Hibernate.isInitialized( loaded.getOther() ) );
					Hibernate.initialize( loaded.getOther() );
				}
		);
	}

	@Test
	public void testJoinFetch() {
		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "select e from Entity e inner join fetch e.other o" )
							.uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 1, loaded.getId().getId() );
					assertTrue( Hibernate.isInitialized( loaded.getOther() ) );
					assertEquals( 2, loaded.getOther().getId().getId() );
				}
		);
	}

	@Test
	public void testSelectFullNull() {
		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.other is null" )
							.uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 3, loaded.getId().getId() );
					assertNull( loaded.getOther() );

				}
		);
	}

	@Test
	public void testSelectPartialNull() {
		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.other.id.id is null" )
							.uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 3, loaded.getId().getId() );
					assertNull( loaded.getOther() );
				}
		);
	}

	@Test
	public void testSelectFull() {
		inTransaction(
				session -> {
					OtherEntity other = new OtherEntity();
					other.setId( new Id( "test", 2 ) );
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.other = :other" )
							.setParameter( "other", other )
							.uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 1, loaded.getId().getId() );
					assertNotNull( loaded.getOther() );
					assertEquals( 2, loaded.getOther().getId().getId() );
				}
		);
	}

	@Test
	public void testUpdateFromExisting() {
		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 1" ).uniqueResult();
					assertNotNull( "loaded", loaded );
					assertNotNull( loaded.getOther() );
					loaded.setOther( new OtherEntity() );
					loaded.getOther().setId( new Id( "test", 3 ) );
				}
		);
	}

	@Test
	public void testUpdateFromNull() {
		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 3" ).uniqueResult();
					assertNotNull( "loaded", loaded );
					assertNull( loaded.getOther() );
					loaded.setOther( new OtherEntity() );
					loaded.getOther().setId( new Id( "test", 3 ) );
				}
		);
	}

	@Test
	@Ignore("multi-column updates don't work!")
	public void testUpdateHql() {
		inTransaction(
				session -> {
					OtherEntity other = new OtherEntity();
					other.setId( new Id( "null", 4 ) );
					assertEquals(
							"execute",
							1,
							session.createQuery( "update Entity e set e.other = :other where e.id.id = 3" )
									.setParameter( "other", other )
									.executeUpdate()
					);
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 3" ).uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 4, loaded.getOther().getId().getId() );
				}
		);
	}

	@Test
	@Ignore("multi-column updates don't work!")
	public void testUpdateHqlNull() {
		inTransaction(
				session -> {
					assertEquals(
							"execute",
							1,
							session.createQuery( "update Entity e set e.other = null where e.id.id = 1" )
									.executeUpdate()
					);
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 1" ).uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 4, loaded.getOther().getId().getId() );
				}
		);
	}

	@Test
	public void testDeleteHql() {
		inTransaction(
				session -> {
					OtherEntity other = new OtherEntity();
					other.setId( new Id( "test", 2 ) );
					assertEquals(
							"execute",
							1,
							session.createQuery( "delete Entity e where e.other = :other" )
									.setParameter( "other", other )
									.executeUpdate()
					);
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 1" ).uniqueResult();
					assertNull( "loaded", loaded );
				}
		);
	}

	@Test
	public void testPersist() {
		inTransaction(
				session -> {
					Entity entity = new Entity();
					entity.setId( new Id( "new", 5 ) );
					entity.setOther( new OtherEntity() );
					entity.getOther().setId( new Id( "new", 6 ) );
					session.persist( entity );
				}
		);

		inTransaction(
				session -> {
					Entity loaded = (Entity) session.createQuery( "from Entity e where e.id.id = 5" ).uniqueResult();
					assertNotNull( "loaded", loaded );
					assertEquals( 6, loaded.getOther().getId().getId() );
				}
		);
	}

}
