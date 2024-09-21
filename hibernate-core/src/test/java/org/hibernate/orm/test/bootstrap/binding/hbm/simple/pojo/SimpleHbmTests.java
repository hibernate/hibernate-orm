/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.simple.pojo;

import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SimpleHbmTests {
	@Test
	public void testBinding(ServiceRegistryScope scope) {
		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/bootstrap/binding/hbm/simple/pojo/SimpleEntity.hbm.xml" )
				.buildMetadata()
				.buildSessionFactory();

		final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( SimpleEntity.class );

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		assertThat( identifierMapping, instanceOf( BasicEntityIdentifierMapping.class ) );
		final BasicEntityIdentifierMapping bid = (BasicEntityIdentifierMapping) identifierMapping;
		assertThat( bid.getFetchableName(), is( "id" ) );
		assertThat( bid.getPartName(), is( EntityIdentifierMapping.ID_ROLE_NAME ) );

		assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 1 ) );
		assertThat( entityDescriptor.getNumberOfDeclaredAttributeMappings(), is( 1 ) );
		final AttributeMapping nameAttr = entityDescriptor.findAttributeMapping( "name" );
		assertThat( nameAttr, notNullValue() );
	}
}
