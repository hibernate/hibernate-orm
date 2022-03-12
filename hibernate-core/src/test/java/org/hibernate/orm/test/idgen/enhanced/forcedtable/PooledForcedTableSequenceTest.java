/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.enhanced.forcedtable;

import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/idgen/enhanced/forcedtable/Pooled.hbm.xml"
)
@SessionFactory
public class PooledForcedTableSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
        EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getIdentifierGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getIdentifierGenerator();
		assertThat( generator.getDatabaseStructure(), instanceOf( TableStructure.class ) );
		assertThat( generator.getOptimizer(), instanceOf( PooledOptimizer.class ) );

		final PooledOptimizer optimizer = (PooledOptimizer) generator.getOptimizer();
		final int increment = optimizer.getIncrementSize();

		scope.inTransaction(
				(s) -> {
					// The value that we get from the callback is the high value (PooledOptimizer by default)
					// When first increment is initialValue, we can only generate one id from it -> id 1
					Entity entity = new Entity( "1" );
					s.save( entity );

					long expectedId = 1;
					assertEquals( expectedId, entity.getId().longValue() );
					assertEquals( 1, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
					assertEquals( 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );

					// now start a full range of values, callback give us hiValue 11
					// id : 2,3,4...,11
					for ( int i = 0; i < increment; i++ ) {
						entity = new Entity( "" + ( i + 2 ) );
						s.save( entity );

						expectedId = i + 2;
						assertEquals( expectedId, entity.getId().longValue() );
						assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
						assertEquals( i + 2, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					}

					// now force a "clock over" -> id 12. (increment * 2 + initialValue) - 9 is the current formula of PooledOptimizer
					entity = new Entity( "" + ( ( increment * 2 + 1 )  - 9 )  );
					s.save( entity );

					expectedId = optimizer.getIncrementSize() + 2;
					assertEquals( expectedId, entity.getId().longValue() );
					assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( increment * 2 + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( (increment * 2 + 1 )  - 9, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
					assertEquals( increment * 2 + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.createQuery( "delete Entity" ).executeUpdate()
		);
	}
}
