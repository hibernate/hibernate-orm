/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;

import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel( xmlMappings = "org/hibernate/orm/test/idgen/enhanced/sequence/Pooled.hbm.xml" )
@SessionFactory
public class PooledSequenceTest {

	private static final long INITIAL_VALUE = 1;

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertThat( generator.getOptimizer(), instanceOf( PooledOptimizer.class ) );

		final PooledOptimizer optimizer = (PooledOptimizer) generator.getOptimizer();
		final int increment = optimizer.getIncrementSize();

		scope.inTransaction(
				(session) -> {
					// The value that we get from the callback is the high value (PooledOptimizer by default)
					// When first increment is initialValue, we can only generate one id from it -> id 1
					Entity entity = new Entity( "" + INITIAL_VALUE );
					session.persist( entity );

					long expectedId = INITIAL_VALUE;
					assertEquals( expectedId, entity.getId().longValue() );
					assertEquals( 1, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( INITIAL_VALUE, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( INITIAL_VALUE, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
					assertEquals( INITIAL_VALUE, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );

					// now start a full range of values, callback give us hiValue 11
					// id : 2,3,4...,11
					for ( int i = 1; i <= increment; i++ ) {
						entity = new Entity( "" + ( i + INITIAL_VALUE ) );
						session.persist( entity );

						expectedId = i + INITIAL_VALUE;
						assertEquals( expectedId, entity.getId().longValue() );
						assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization calls seq twice
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization calls seq twice
						assertEquals( expectedId, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					}

					// now force a "clock over"
					expectedId++;
					entity = new Entity( "" + expectedId  );
					session.persist( entity );

					assertEquals( expectedId, entity.getId().longValue() );
					assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( increment * 2L + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( expectedId, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
				}
		);
	}

	@AfterEach
	public void cleanTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Entity" ).executeUpdate()
		);
	}
}
