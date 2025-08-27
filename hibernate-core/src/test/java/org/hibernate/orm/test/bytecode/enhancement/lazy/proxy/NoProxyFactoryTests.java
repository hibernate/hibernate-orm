/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.internal.EntityRepresentationStrategyPojoStandard;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cases where we do not want a {@linkplain org.hibernate.proxy.ProxyFactory}
 *
 * @author Steve Ebersole
 */
@RunWith(BytecodeEnhancerRunner.class )
@EnhancementOptions(lazyLoading = true)
public class NoProxyFactoryTests extends BaseNonConfigCoreFunctionalTestCase {
	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, EntityRepresentationStrategyPojoStandard.class.getName() )
	);

	/**
	 * See org.hibernate.internal.CoreMessageLogger#unableToCreateProxyFactory
	 */
	private final Triggerable triggerable = logInspection.watchForLogMessages( "HHH000305" );


	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		sources.addAnnotatedClasses( SimpleEntity.class );
	}

	@Test
	public void testNoInheritance() {
		assertThat( triggerable.wasTriggered() )
				.describedAs( "Warning was logged" )
				.isFalse();

		final MappingMetamodelImplementor mappingMetamodel = sessionFactory().getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( SimpleEntity.class );
		assertThat( entityDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ).isTrue();
		assertThat( entityDescriptor.getRepresentationStrategy().getProxyFactory() ).isNull();

		inTransaction( (session) -> {
			final SimpleEntity reference = session.getReference( SimpleEntity.class, 1 );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
			assertThat( reference ).isNotInstanceOf( HibernateProxy.class );
		} );
	}

	@Entity(name="SimpleEntity")
	@Table(name="SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;

		public final Integer getId() {
			return id;
		}

		public final void setId(Integer id) {
			this.id = id;
		}

		public final String getName() {
			return name;
		}

		public final void setName(String name) {
			this.name = name;
		}
	}
}
