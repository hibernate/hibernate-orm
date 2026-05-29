/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * Tests that final accessors do not prevent a {@link ProxyFactory} after bytecode enhancement.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				FinalAccessorProxyFactoryTests.SimpleEntity.class,
				FinalAccessorProxyFactoryTests.SpecializedEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class FinalAccessorProxyFactoryTests {
	@RegisterExtension
	public LoggerInspectionExtension logInspection =
			LoggerInspectionExtension.builder().setLogger( CORE_LOGGER ).build();

	/**
	 * See org.hibernate.internal.CoreMessageLogger#unableToCreateProxyFactory
	 */
	private final Triggerable triggerable = logInspection.watchForLogMessages( "HHH000305" );

	@Test
	public void finalAccessorsAreMadeProxyable(SessionFactoryScope scope) {
		assertThat( triggerable.wasTriggered() )
				.describedAs( "Warning was logged" )
				.isFalse();

		final var mappingMetamodel = scope.getSessionFactory().getMappingMetamodel();
		final var entityDescriptor = mappingMetamodel.findEntityDescriptor( SimpleEntity.class );
		assertThat( entityDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() ).isTrue();
		assertThat( entityDescriptor.getRepresentationStrategy().getProxyFactory() ).isNotNull();

		scope.inTransaction( session -> {
			final SimpleEntity reference = session.getReference( SimpleEntity.class, 1 );
			assertThat( isInitialized( reference ) ).isFalse();
			assertThat( reference ).isInstanceOf( HibernateProxy.class );
		} );
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
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

	@Entity(name = "SpecializedEntity")
	public static class SpecializedEntity extends SimpleEntity {
		private String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
