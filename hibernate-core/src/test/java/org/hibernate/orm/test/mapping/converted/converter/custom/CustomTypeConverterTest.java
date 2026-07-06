/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class CustomTypeConverterTest {

	@Test
	@BootstrapServiceRegistry( javaServices = @JavaService(
			role = TypeContributor.class,
			impl = PayloadWrapperTypeContributorImpl.class
	) )
	@SuppressWarnings("JUnitMalformedDeclaration")
	public void testConverterAppliedScopedContributions(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = (MetadataImplementor) MetadataBuildingTestHelper.buildMetadata(
				registryScope.getRegistry(),
				PayloadWrapperConverter.class,
				MyEntity.class
		);
		final TypeConfiguration bootTypeConfiguration = metadata.getTypeConfiguration();
		performAssertions( metadata, bootTypeConfiguration );
	}

	protected void performAssertions(
			MetadataImplementor metadata,
			TypeConfiguration bootTypeConfiguration) {
		try ( final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
			assertThat(
					sessionFactory.getMappingMetamodel().getTypeConfiguration(),
					sameInstance( bootTypeConfiguration )
			);

			assertThat(
					bootTypeConfiguration.getJavaTypeRegistry().resolveDescriptor( PayloadWrapper.class ),
					sameInstance( PayloadWrapperJavaType.INSTANCE )
			);

			assertThat(
					bootTypeConfiguration.getJdbcTypeRegistry().getDescriptor( PayloadWrapperJdbcType.INSTANCE.getJdbcTypeCode() ),
					sameInstance( PayloadWrapperJdbcType.INSTANCE )
			);

			final EntityPersister entityPersister = sessionFactory.getMappingMetamodel().getEntityDescriptor( MyEntity.class );
			entityPersister.getPropertyType( "customType" );
		}
	}

	public static class PayloadWrapperTypeContributorImpl implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( PayloadWrapperJavaType.INSTANCE );
			typeContributions.contributeJdbcType( PayloadWrapperJdbcType.INSTANCE );
		}
	}
}
