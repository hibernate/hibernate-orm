/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Tests the correctness of the parameter hibernate.dialect.hana.treat_double_typed_fields_as_decimal which controls the
 * handling of double types as either {@link BigDecimal} (parameter is set to true) or {@link Double} (default behavior
 * or parameter is set to false)
 * 
 * @author Jonathan Bregler
 */
@RequiresDialect(value = { AbstractHANADialect.class })
public class HANADecimalTest extends BaseCoreFunctionalTestCase {

	private static final String ENTITY_NAME = "DecimalEntity";

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, localSession -> {
			localSession.doWork( connection -> {
				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE COLUMN TABLE " + ENTITY_NAME
								+ " (key INTEGER, doubledouble DOUBLE, decimaldecimal DECIMAL(38,15), doubledecimal DECIMAL(38,15), decimaldouble DOUBLE, PRIMARY KEY (key))" ) ) {
					ps.execute();
				}
			} );
		} );
	}

	@Override
	protected void cleanupTest() throws Exception {
		doInHibernate( this::sessionFactory, localSession -> {
			localSession.doWork( connection -> {
				try ( PreparedStatement ps = connection.prepareStatement( "DROP TABLE " + ENTITY_NAME ) ) {
					ps.execute();
				}
				catch (Exception e) {
					// Ignore
				}
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12995")
	public void testDecimalTypeFalse() throws Exception {
		rebuildSessionFactory( configuration -> {
			configuration.setProperty( "hibernate.dialect.hana.treat_double_typed_fields_as_decimal", Boolean.FALSE.toString() );
		} );

		Session s = openSession();
		s.beginTransaction();

		DecimalEntity entity = new DecimalEntity();
		entity.key = Integer.valueOf( 1 );
		entity.doubleDouble = 1.19d;
		entity.decimalDecimal = BigDecimal.valueOf( 1.19d );
		entity.doubleDecimal = 1.19d;
		entity.decimalDouble = BigDecimal.valueOf( 1.19d );
		
		s.persist( entity );

		DecimalEntity entity2 = new DecimalEntity();
		entity2.key = Integer.valueOf( 2 );
		entity2.doubleDouble = 0.3d;
		entity2.decimalDecimal = BigDecimal.valueOf( 0.3d );
		entity2.doubleDecimal = 0.3d;
		entity2.decimalDouble = BigDecimal.valueOf( 0.3d );

		s.persist( entity2 );

		s.flush();

		s.getTransaction().commit();

		s.clear();

		Query<DecimalEntity> legacyQuery = s.createQuery( "select b from " + ENTITY_NAME + " b order by key asc", DecimalEntity.class );

		List<DecimalEntity> retrievedEntities = legacyQuery.getResultList();

		assertEquals(2, retrievedEntities.size());

		DecimalEntity retrievedEntity = retrievedEntities.get( 0 );
		assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
		assertEquals( 1.19d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "1.190000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 1.189999999999999d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "1.19" ), retrievedEntity.decimalDouble );
		
		retrievedEntity = retrievedEntities.get( 1 );
		assertEquals( Integer.valueOf( 2 ), retrievedEntity.key );
		assertEquals( 0.3d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "0.300000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 0.299999999999999d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "0.3" ), retrievedEntity.decimalDouble );

	}

	@Test
	@TestForIssue(jiraKey = "HHH-12995")
	public void testDecimalTypeDefault() throws Exception {
		rebuildSessionFactory();

		Session s = openSession();
		s.beginTransaction();

		DecimalEntity entity = new DecimalEntity();
		entity.key = Integer.valueOf( 1 );
		entity.doubleDouble = 1.19d;
		entity.decimalDecimal = BigDecimal.valueOf( 1.19d );
		entity.doubleDecimal = 1.19d;
		entity.decimalDouble = BigDecimal.valueOf( 1.19d );

		s.persist( entity );

		DecimalEntity entity2 = new DecimalEntity();
		entity2.key = Integer.valueOf( 2 );
		entity2.doubleDouble = 0.3d;
		entity2.decimalDecimal = BigDecimal.valueOf( 0.3d );
		entity2.doubleDecimal = 0.3d;
		entity2.decimalDouble = BigDecimal.valueOf( 0.3d );

		s.persist( entity2 );

		s.flush();

		s.getTransaction().commit();

		s.clear();

		Query<DecimalEntity> legacyQuery = s.createQuery( "select b from " + ENTITY_NAME + " b order by key asc", DecimalEntity.class );

		List<DecimalEntity> retrievedEntities = legacyQuery.getResultList();

		assertEquals(2, retrievedEntities.size());

		DecimalEntity retrievedEntity = retrievedEntities.get( 0 );
		assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
		assertEquals( 1.19d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "1.190000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 1.189999999999999d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "1.19" ), retrievedEntity.decimalDouble );
		
		retrievedEntity = retrievedEntities.get( 1 );
		assertEquals( Integer.valueOf( 2 ), retrievedEntity.key );
		assertEquals( 0.3d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "0.300000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 0.299999999999999d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "0.3" ), retrievedEntity.decimalDouble );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12995")
	public void testDecimalTypeTrue() throws Exception {
		rebuildSessionFactory( configuration -> {
			configuration.setProperty( "hibernate.dialect.hana.treat_double_typed_fields_as_decimal", Boolean.TRUE.toString() );
		} );

		Session s = openSession();
		s.beginTransaction();

		DecimalEntity entity = new DecimalEntity();
		entity.key = Integer.valueOf( 1 );
		entity.doubleDouble = 1.19d;
		entity.decimalDecimal = BigDecimal.valueOf( 1.19d );
		entity.doubleDecimal = 1.19d;
		entity.decimalDouble = BigDecimal.valueOf( 1.19d );

		s.persist( entity );
		
		DecimalEntity entity2 = new DecimalEntity();
		entity2.key = Integer.valueOf( 2 );
		entity2.doubleDouble = 0.3d;
		entity2.decimalDecimal = BigDecimal.valueOf( 0.3d );
		entity2.doubleDecimal = 0.3d;
		entity2.decimalDouble = BigDecimal.valueOf( 0.3d );

		s.persist( entity2 );

		s.flush();

		s.getTransaction().commit();

		s.clear();

		Query<DecimalEntity> legacyQuery = s.createQuery( "select b from " + ENTITY_NAME + " b order by key asc", DecimalEntity.class );

		List<DecimalEntity> retrievedEntities = legacyQuery.getResultList();

		assertEquals(2, retrievedEntities.size());

		DecimalEntity retrievedEntity = retrievedEntities.get( 0 );
		assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
		assertEquals( 1.19d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "1.190000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 1.19d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "1.19" ), retrievedEntity.decimalDouble );
		
		retrievedEntity = retrievedEntities.get( 1 );
		assertEquals( Integer.valueOf( 2 ), retrievedEntity.key );
		assertEquals( 0.3d, retrievedEntity.doubleDouble, 0 );
		assertEquals( new BigDecimal( "0.300000000000000" ), retrievedEntity.decimalDecimal );
		assertEquals( 0.3d, retrievedEntity.doubleDecimal, 0 );
		assertEquals( new BigDecimal( "0.3" ), retrievedEntity.decimalDouble );
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[]{
				DecimalEntity.class
		};
	}

	@Entity(name = ENTITY_NAME)
	public static class DecimalEntity {

		@Id
		public Integer key;

		public double doubleDouble;

		public BigDecimal decimalDecimal;

		public double doubleDecimal;

		public BigDecimal decimalDouble;
	}

}
