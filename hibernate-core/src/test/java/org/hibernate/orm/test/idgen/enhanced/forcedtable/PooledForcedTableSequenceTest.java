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
					for ( int i = 0; i <= increment; i++ ) {
						final Entity entity = new Entity( "" + ( i + 1 ) );
						s.save( entity );

						long expectedId = i + 1;
						assertEquals( expectedId, entity.getId().longValue() );
						// NOTE : initialization calls table twice
						assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
						assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					}
					// now force a "clock over"
					final Entity entity = new Entity( "" + increment );
					s.save( entity );
					long expectedId = optimizer.getIncrementSize() + 2;
					assertEquals( expectedId, entity.getId().longValue() );
					// initialization (2) + clock over
					assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( increment + 2, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
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
