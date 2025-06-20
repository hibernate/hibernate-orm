/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.forcedtable;


import org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import org.hibernate.id.enhanced.NoopOptimizer;
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
		xmlMappings = "org/hibernate/orm/test/idgen/enhanced/forcedtable/Basic.hbm.xml"
)
@SessionFactory
public class BasicForcedTableSequenceTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator(), instanceOf( SequenceStyleGenerator.class ) );

		final SequenceStyleGenerator generator = (SequenceStyleGenerator) persister.getGenerator();
		assertThat( generator.getDatabaseStructure(), instanceOf( TableStructure.class ) );
		assertThat( generator.getOptimizer(), instanceOf( NoopOptimizer.class ) );

		scope.inTransaction(
				(session) -> {
					int count = 5;

					for ( int i = 0; i < count; i++ ) {
						final Entity entity = new Entity( "" + ( i + 1 ) );
						session.persist( entity );
						long expectedId = i + 1;
						assertEquals( expectedId, entity.getId().longValue() );
						assertEquals( expectedId, generator.getDatabaseStructure().getTimesAccessed() );
						assertEquals( expectedId, ( (BasicHolder) generator.getOptimizer().getLastSourceValue() ).getActualLongValue() );
					}
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
