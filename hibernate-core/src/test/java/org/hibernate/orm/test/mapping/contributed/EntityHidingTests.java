/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.contributed;

import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry( javaServices = @JavaService(
		role = AdditionalMappingContributor.class,
		impl = ContributorImpl.class )
)
@ServiceRegistry( settings = @Setting(
		name = AvailableSettings.JPA_METAMODEL_POPULATION,
		value = "ignoreUnsupported"
) )
@DomainModel( annotatedClasses = MainEntity.class )
@SessionFactory
public class EntityHidingTests {
	@Test
	@NotImplementedYet( reason = "Contributed entity hiding is not yet implemented" )
	public void testModel(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();

		final EntityDomainType<?> jpaModelDescriptor = sessionFactory.getJpaMetamodel().entity( "DynamicEntity" );
		assertThat( jpaModelDescriptor, nullValue() );

		final EntityPersister mappingModelDescriptor = sessionFactory.getMappingMetamodel()
				.findEntityDescriptor( "DynamicEntity" );
		assertThat( mappingModelDescriptor, nullValue() );
	}
}
