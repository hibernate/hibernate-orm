/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced.table;

import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.id.IdentifierGeneratorHelper.BasicHolder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = "org/hibernate/orm/test/idgen/enhanced/table/Basic.hbm.xml" )
@SessionFactory
public class BasicTableTest {

	@Test
	public void testNormalBoundary(SessionFactoryScope scope) {
		final EntityPersister persister = scope.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Entity.class.getName());
		assertThat( persister.getGenerator() ).isInstanceOf( TableGenerator.class );
		final TableGenerator generator = ( TableGenerator ) persister.getGenerator();

		scope.inTransaction( (s) -> {
			int count = 5;
			for ( int i = 0; i < count; i++ ) {
				final Entity entity = new Entity( "" + ( i + 1 ) );
				s.persist( entity );
				long expectedId = i + 1;
				assertEquals( expectedId, entity.getId().longValue() );
				assertEquals( expectedId, generator.getTableAccessCount() );
				assertEquals( expectedId, ( (BasicHolder) generator.getOptimizer().getLastSourceValue() ).getActualLongValue() );
			}
		} );
	}

	@AfterEach
	public void cleanTestData(SessionFactoryScope scope) {
		scope.dropData();
	}
}
