/**
 * 
 */
package org.hibernate.test.compositeusertype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class CompositeUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "compositeusertype/TestEntity.hbm.xml" };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9186")
	public void testRemovalWithNullableFields() {
		Session session = openSession();

		final Unit unit1 = Percent.INSTANCE;
		final Unit unit2 = new Currency( "EUR" );
		final Unit unit3 = new Currency( "USD" );

		TestEntity entity = new TestEntity();
		entity.setId( 1 );
		Transaction tx = session.beginTransaction();
		session.persist( entity );
		tx.commit();
		session.close();

		session = openSession();
		entity = (TestEntity) session.get( TestEntity.class, entity.getId() );
		assertNotNull( "Expected an entity to be returned", entity );
		assertTrue( "Expected no units", entity.getUnits().isEmpty() );
		tx = session.beginTransaction();
		entity.getUnits().add( unit1 );
		entity.getUnits().add( unit2 );
		entity.getUnits().add( unit3 );
		tx.commit();
		session.close();

		session = openSession();
		entity = (TestEntity) session.get( TestEntity.class, entity.getId() );
		assertNotNull( "Expected an entity to be returned", entity );
		assertEquals( "Unexpected units", buildSet( unit1, unit2, unit3 ), entity.getUnits() );
		tx = session.beginTransaction();
		entity.getUnits().remove( unit2 );
		tx.commit();
		session.close();

		session = openSession();
		entity = (TestEntity) session.get( TestEntity.class, entity.getId() );
		assertNotNull( "Expected an entity to be returned", entity );
		assertEquals( "Unexpected units", buildSet( unit1, unit3 ), entity.getUnits() );

	}

	private Set<Unit> buildSet(final Unit... units) {
		final Set<Unit> result = new HashSet<Unit>();
		for ( final Unit unit : units ) {
			result.add( unit );
		}
		return result;
	}

}
