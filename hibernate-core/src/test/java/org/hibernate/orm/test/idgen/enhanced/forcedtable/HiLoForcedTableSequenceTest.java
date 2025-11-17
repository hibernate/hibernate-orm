/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.forcedtable;

import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/idgen/enhanced/forcedtable/HiLo.hbm.xml")
@SessionFactory
public class HiLoForcedTableSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( SequenceStyleGenerator.class );
		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getGenerator();
		assertThat( generator.getDatabaseStructure() ).isInstanceOf( TableStructure.class );
		assertThat( generator.getOptimizer() ).isInstanceOf( HiLoOptimizer.class );

		final HiLoOptimizer optimizer = (HiLoOptimizer) generator.getOptimizer();
		int increment = optimizer.getIncrementSize();

		scope.inTransaction( (s) -> {
			for ( int i = 0; i < increment; i++ ) {
				final Entity entity = new Entity( "" + ( i + 1 ) );
				s.persist( entity );

				long expectedId = i + 1;
				assertThat( entity.getId().longValue() ).isEqualTo( expectedId );
				assertThat( ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ).isEqualTo( 1L );
				assertThat( ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() ).isEqualTo( i + 1L );
				assertThat( ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() ).isEqualTo( increment + 1L );
			}

			// now force a "clock over"
			final Entity entity = new Entity( "" + increment );
			s.persist( entity );

			long expectedId = optimizer.getIncrementSize() + 1;
			assertThat( entity.getId() ).isEqualTo( expectedId );
			// initialization + clock-over
			assertThat( ( (BasicHolder) optimizer.getLastSourceValue() ).getActualLongValue() ).isEqualTo( 2L );
			assertThat( ( (BasicHolder) optimizer.getLastValue() ).getActualLongValue() ).isEqualTo( increment + 1L );
			assertThat( ( (BasicHolder) optimizer.getHiValue() ).getActualLongValue() ).isEqualTo( ( increment * 2L ) + 1L );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}
}
