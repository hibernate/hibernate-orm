/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.sequence;

import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
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
		"org/hibernate/orm/test/idgen/enhanced/sequence/Basic.hbm.xml",
		"org/hibernate/orm/test/idgen/enhanced/sequence/Dedicated.hbm.xml"
})
@SessionFactory
public class BasicSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( SequenceStyleGenerator.class );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getGenerator();

		final int count = 5;

		scope.inTransaction( (s) -> {
			for ( int i = 0; i < count; i++ ) {
				final Entity entity = new Entity( "" + ( i + 1 ) );
				s.persist( entity );

				long expectedId = i + 1;

				assertEquals( expectedId, entity.getId().longValue() );
				assertEquals( expectedId, generator.getDatabaseStructure().getTimesAccessed() );
				assertEquals( expectedId, ( (BasicHolder) generator.getOptimizer()
						.getLastSourceValue() ).getActualLongValue() );
			}
		} );
	}

	@Test
	@JiraKey(value = "HHH-6790")
	public void testSequencePerEntity(SessionFactoryScope scope) {
		final String overriddenEntityName = "SpecialEntity";

		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(overriddenEntityName);
		assertThat( persister.getGenerator() ).isInstanceOf( SequenceStyleGenerator.class );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getGenerator();
		assertEquals( "ID_SEQ_BSC_ENTITY" + SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX,
				generator.getDatabaseStructure().getPhysicalName().render() );

		scope.inTransaction( (s) -> {
			Entity entity1 = new Entity( "1" );
			s.persist( overriddenEntityName, entity1 );
			Entity entity2 = new Entity( "2" );
			s.persist( overriddenEntityName, entity2 );

			assertEquals( 1, entity1.getId().intValue() );
			assertEquals( 2, entity2.getId().intValue() );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}
}
