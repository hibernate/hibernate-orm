/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.sequence;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import static org.hibernate.testing.junit4.ExtraAssertions.assertClassAssignability;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class PooledSequenceTest extends BaseCoreFunctionalTestCase {

	private static final long INITIAL_VALUE = 1;

	@Override
	public String[] getMappings() {
		return new String[] { "idgen/enhanced/sequence/Pooled.hbm.xml" };
	}

	@Test
	public void testNormalBoundary() {
		EntityPersister persister = sessionFactory().getEntityPersister( Entity.class.getName() );
		assertClassAssignability( SequenceStyleGenerator.class, persister.getIdentifierGenerator().getClass() );
		SequenceStyleGenerator generator = ( SequenceStyleGenerator ) persister.getIdentifierGenerator();
		assertClassAssignability( PooledOptimizer.class, generator.getOptimizer().getClass() );
		PooledOptimizer optimizer = (PooledOptimizer) generator.getOptimizer();

		int increment = optimizer.getIncrementSize();

		TransactionUtil.doInHibernate(
				this::sessionFactory,
				s -> {
					// The value that we get from the callback is the high value (PooledOptimizer by default)
					// When first increment is initialValue, we can only generate one id from it -> id 1
					Entity entity = new Entity( "" + INITIAL_VALUE );
					s.save( entity );

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
						s.save( entity );

						expectedId = i + INITIAL_VALUE;
						assertEquals( expectedId, entity.getId().longValue() );
						assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
						assertEquals( expectedId, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
					}

					// now force a "clock over"
					expectedId++;
					entity = new Entity( "" + expectedId  );
					s.save( entity );

					assertEquals( expectedId, entity.getId().longValue() );
					assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() );
					assertEquals( increment * 2L + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					assertEquals( expectedId, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );

					s.createQuery( "delete Entity" ).executeUpdate();
				}
		);
	}
}
