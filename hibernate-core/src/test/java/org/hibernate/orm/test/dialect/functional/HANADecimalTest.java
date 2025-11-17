/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the correctness of the parameter hibernate.dialect.hana.treat_double_typed_fields_as_decimal which controls the
 * handling of double types as either {@link BigDecimal} (parameter is set to true) or {@link Double} (default behavior
 * or parameter is set to false)
 *
 * @author Jonathan Bregler
 */
@RequiresDialect(HANADialect.class)
@DomainModel(annotatedClasses = {HANADecimalTest.DecimalEntity.class})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = {@Setting(name = "hibernate.dialect.hana.treat_double_typed_fields_as_decimal", value = "true")})
public class HANADecimalTest {

	private static final String ENTITY_NAME = "DecimalEntity";

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session -> {
			session.doWork( connection -> {
				try ( PreparedStatement ps = connection
						.prepareStatement( "CREATE COLUMN TABLE " + ENTITY_NAME
								+ " (key INTEGER, doubledouble DOUBLE, decimaldecimal DECIMAL(38,15), doubledecimal DECIMAL(38,15), decimaldouble DOUBLE, PRIMARY KEY (key))" ) ) {
					ps.execute();
				}
			} );
		} );
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session -> {
			session.doWork( connection -> {
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
	@JiraKey(value = "HHH-12995")
	public void testDecimalTypeTrue(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			DecimalEntity entity = new DecimalEntity();
			entity.key = 1;
			entity.doubleDouble = 1.19d;
			entity.decimalDecimal = BigDecimal.valueOf( 1.19d );
			entity.doubleDecimal = 1.19d;
			entity.decimalDouble = BigDecimal.valueOf( 1.19d );
			session.persist( entity );

			DecimalEntity entity2 = new DecimalEntity();
			entity2.key = 2;
			entity2.doubleDouble = 0.3d;
			entity2.decimalDecimal = BigDecimal.valueOf( 0.3d );
			entity2.doubleDecimal = 0.3d;
			entity2.decimalDouble = BigDecimal.valueOf( 0.3d );
			session.persist( entity2 );
		} );

		scope.inTransaction(   session -> {
			Query<DecimalEntity> legacyQuery = session.createQuery( "select b from " + ENTITY_NAME + " b order by key asc", DecimalEntity.class );
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
		} );
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
