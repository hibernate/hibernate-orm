/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( xmlMappings = "org/hibernate/orm/test/idgen/enhanced/sequence/Pooled.hbm.xml" )
@SessionFactory
public class PooledSequenceTest {

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
					for ( int i = 0; i <= increment; i++ ) {
						final Entity entity = new Entity( "" + ( i + 1 ) );
						session.save( entity );
						assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization calls seq twice
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization calls seq twice
						assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
						assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
					}
					// now force a "clock over"
					final Entity entity = new Entity( "" + increment );
					session.save( entity );
					assertEquals( 3, generator.getDatabaseStructure().getTimesAccessed() ); // initialization (2) + clock over
					assertEquals( ( increment * 2 ) + 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization (2) + clock over
					assertEquals( increment + 2, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
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
