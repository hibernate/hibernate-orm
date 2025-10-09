/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;


import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = "org/hibernate/orm/test/idgen/enhanced/sequence/HiLo.hbm.xml" )
@SessionFactory
public class HiLoSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( SequenceStyleGenerator.class );
		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getGenerator();
		assertThat( generator.getOptimizer() ).isInstanceOf( HiLoOptimizer.class );
		final HiLoOptimizer optimizer = (HiLoOptimizer) generator.getOptimizer();
		final int increment = optimizer.getIncrementSize();

		scope.inTransaction( (s) -> {
			for ( int i = 0; i < increment; i++ ) {
				final Entity entity = new Entity( "" + ( i + 1 ) );
				s.persist( entity );

				// initialization
				assertEquals( 1, generator.getDatabaseStructure().getTimesAccessed() );
				// initialization
				assertEquals( 1, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() );
				assertEquals( i + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
				assertEquals( increment + 1, ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() );
			}

			// now force a "clock over"
			final Entity entity = new Entity( "" + increment );
			s.persist( entity );
			assertEquals( 2, generator.getDatabaseStructure().getTimesAccessed() ); // initialization
			assertEquals( 2, ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ); // initialization
			assertEquals( increment + 1, ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() );
			assertEquals( ( increment * 2L ) + 1, ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() );
		} );
	}

	@AfterEach
	public void cleanTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
