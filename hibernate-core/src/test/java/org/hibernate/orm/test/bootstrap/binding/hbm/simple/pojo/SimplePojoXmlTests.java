/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.simple.pojo;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
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
@DomainModel(xmlMappings = "org/hibernate/orm/test/bootstrap/binding/hbm/simple/pojo/SimpleEntity.xml")
@SessionFactory
public class SimplePojoXmlTests {
	@Test
	public void testBinding(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

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

	@Test
	void testUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from SimpleEntity" ).list();
			session.createQuery( "select e from SimpleEntity e" ).list();
			session.createQuery( "select e from SimpleEntity e where e.name = 'abc'" ).list();
		} );
	}
}
