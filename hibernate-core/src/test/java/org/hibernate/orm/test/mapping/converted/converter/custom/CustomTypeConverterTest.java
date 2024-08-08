/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( PayloadWrapperConverter.class )
				.addAnnotatedClass( MyEntity.class );
		final MetadataBuilderImplementor metadataBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();
		final TypeConfiguration bootTypeConfiguration = metadataBuilder.getBootstrapContext().getTypeConfiguration();
		performAssertions( metadataBuilder, bootTypeConfiguration );
	}

	protected void performAssertions(
			MetadataBuilderImplementor metadataBuilder,
			TypeConfiguration bootTypeConfiguration) {
		try ( final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadataBuilder.build().buildSessionFactory()) {
			assertThat(
					sessionFactory.getMappingMetamodel().getTypeConfiguration(),
					sameInstance( bootTypeConfiguration )
			);

			assertThat(
					bootTypeConfiguration.getJavaTypeRegistry().getDescriptor( PayloadWrapper.class ),
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
