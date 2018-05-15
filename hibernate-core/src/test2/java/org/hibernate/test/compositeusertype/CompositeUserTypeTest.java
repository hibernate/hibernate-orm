/**
 *
 */
package org.hibernate.test.compositeusertype;

import java.util.Arrays;
import java.util.HashSet;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
		final Unit unit1 = Percent.INSTANCE;
		final Unit unit2 = new Currency( "EUR" );
		final Unit unit3 = new Currency( "USD" );

		final Integer id = 1;

		doInHibernate( this::sessionFactory, session -> {
			TestEntity entity = new TestEntity();
			entity.setId( id );

			session.persist( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			TestEntity entity = session.find( TestEntity.class, id );
			assertNotNull( "Expected an entity to be returned", entity );
			assertTrue( "Expected no units", entity.getUnits().isEmpty() );

			entity.getUnits().add( unit1 );
			entity.getUnits().add( unit2 );
			entity.getUnits().add( unit3 );
		} );

		doInHibernate( this::sessionFactory, session -> {
			TestEntity entity = session.get( TestEntity.class, id );
			assertNotNull( "Expected an entity to be returned", entity );
			assertEquals(
					"Unexpected units",
					new HashSet<>( Arrays.asList( unit1, unit2, unit3 ) ),
					entity.getUnits()
			);

			entity.getUnits().remove( unit2 );
		} );

		doInHibernate( this::sessionFactory, session -> {
			TestEntity entity = session.get( TestEntity.class, id );
			assertNotNull( "Expected an entity to be returned", entity );
			assertEquals( "Unexpected units", new HashSet<>( Arrays.asList( unit1, unit3 ) ), entity.getUnits() );
		} );
	}
}
