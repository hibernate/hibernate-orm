/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;

import org.hibernate.generator.Generator;
import org.hibernate.id.GenericGeneratorGeneration;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 * @author Lukasz Antoniak
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = {
		"org/hibernate/orm/test/idgen/enhanced/sequence/Basic.orm.xml",
		"org/hibernate/orm/test/idgen/enhanced/sequence/Dedicated.orm.xml"
})
@SessionFactory
public class BasicSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( GenericGeneratorGeneration.class );

		final GenericGeneratorGeneration genericGenerator = (GenericGeneratorGeneration) persister.getGenerator();
		Generator delegate = genericGenerator.getDelegate();
		assertThat( delegate ).isInstanceOf( SequenceStyleGenerator.class );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) delegate;

		final int count = 5;

		scope.inTransaction( (s) -> {
			for ( int i = 0; i < count; i++ ) {
				final Entity entity = new Entity( "" + ( i + 1 ) );
				s.persist( entity );

				long expectedId = i + 1;

				assertEquals( expectedId, entity.getId().longValue() );
				assertEquals( expectedId, generator.getDatabaseStructure().getTimesAccessed() );
				assertEquals( expectedId, generator.getOptimizer().getLastSourceValue().longValue() );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-6790")
	public void testSequencePerEntity(SessionFactoryScope scope) {

		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor( SpecialEntity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( GenericGeneratorGeneration.class );

		final GenericGeneratorGeneration genericGenerator = (GenericGeneratorGeneration) persister.getGenerator();
		Generator delegate = genericGenerator.getDelegate();
		assertThat( delegate ).isInstanceOf( SequenceStyleGenerator.class );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) delegate;

		assertEquals( "ID_SEQ_BSC_ENTITY" + SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX,
				generator.getDatabaseStructure().getPhysicalName().render() );

		scope.inTransaction( (s) -> {
			SpecialEntity entity1 = new SpecialEntity( "1" );
			s.persist(  entity1 );
			SpecialEntity entity2 = new SpecialEntity( "2" );
			s.persist(  entity2 );

			assertEquals( 1, entity1.getId().intValue() );
			assertEquals( 2, entity2.getId().intValue() );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}
}
