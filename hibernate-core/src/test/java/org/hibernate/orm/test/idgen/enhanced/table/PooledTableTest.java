/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.table;

import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = "org/hibernate/orm/test/idgen/enhanced/table/Pooled.hbm.xml" )
@SessionFactory
public class PooledTableTest {
	private static final long INITIAL_VALUE = 1;

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( TableGenerator.class );
		final TableGenerator generator = (TableGenerator) persister.getGenerator();
		assertThat( generator.getOptimizer() ).isInstanceOf( PooledOptimizer.class );
		final PooledOptimizer optimizer = (PooledOptimizer) generator.getOptimizer();
		final int increment = optimizer.getIncrementSize();

		scope.inTransaction( (s) -> {
			// The value that we get from the callback is the high value (PooledOptimizer by default)
			// When first increment is initialValue, we can only generate one id from it -> id 1
			Entity entity = new Entity( "" + INITIAL_VALUE );
			s.persist( entity );

			long expectedId = INITIAL_VALUE;
			assertEquals( expectedId, entity.getId().longValue() );
			assertEquals( 1, generator.getTableAccessCount() );
			assertEquals( INITIAL_VALUE, optimizer.getLastSourceValue().longValue() );
			assertEquals( INITIAL_VALUE, optimizer.getLastValue() );
			assertEquals( INITIAL_VALUE, optimizer.getLastSourceValue().longValue() );

			// now start a full range of values, callback give us hiValue 11
			// id : 2,3,4...,11
			for ( int i = 1; i <= increment; i++ ) {
				entity = new Entity( "" + ( i + INITIAL_VALUE  ) );
				s.persist( entity );

				expectedId = i + INITIAL_VALUE;
				assertEquals( expectedId, entity.getId().longValue() );
				assertEquals( 2, generator.getTableAccessCount() );
				assertEquals( increment + 1, optimizer.getLastSourceValue().longValue() );
				assertEquals( expectedId, optimizer.getLastValue() );
				assertEquals( increment + 1, optimizer.getLastSourceValue().longValue() );
			}

			// now force a "clock over"
			expectedId++;
			entity = new Entity( "" + expectedId );
			s.persist( entity );

			assertEquals( expectedId, entity.getId().longValue() );
			assertEquals( 3, generator.getTableAccessCount() );
			assertEquals( increment * 2L + 1, optimizer.getLastSourceValue().longValue() );
			assertEquals( expectedId, optimizer.getLastValue() );
		} );
	}

	@AfterEach
	public void cleanTestData(SessionFactoryScope scope) {
		scope.dropData();
	}
}
