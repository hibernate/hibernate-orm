/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/bootstrap/binding/hbm/cid/nonaggregated/dynamic/DynamicCompositeIdBasic.xml")
@SessionFactory
public class DynamicCompositeIdBasicTests {
	@Test
	void testBinding(SessionFactoryScope factoryScope) {
		final EntityPersister entityDescriptor = factoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( "DynamicCompositeIdBasic" );

		assertThat( entityDescriptor.getNumberOfAttributeMappings(), is( 1 ) );

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		assertThat( identifierMapping, instanceOf( NonAggregatedIdentifierMapping.class ) );
		final NonAggregatedIdentifierMapping cid = (NonAggregatedIdentifierMapping) identifierMapping;
		assertThat( cid.getEmbeddableTypeDescriptor().getNumberOfAttributeMappings(), is( 2 ) );

		final AttributeMapping key1 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key1" );
		assertThat( key1, notNullValue() );

		final AttributeMapping key2 = cid.getEmbeddableTypeDescriptor().findAttributeMapping( "key2" );
		assertThat( key2, notNullValue() );

		final AttributeMapping attr1 = entityDescriptor.findAttributeMapping( "attr1" );
		assertThat( attr1, notNullValue() );	}

	@Test
	public void testFullQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "from DynamicCompositeIdBasic e where e.id.key1 = 1" ).list()
		);
	}

	@Test
	public void testEmbeddedQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "from DynamicCompositeIdBasic e where e.key1 = 1" ).list()
		);
	}
}
