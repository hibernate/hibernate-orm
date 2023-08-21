/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.boot.spi.MetadataContributor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService;
import org.hibernate.testing.boot.ExtraJavaServicesClassLoaderService.JavaServiceDescriptor;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import org.jboss.jandex.IndexView;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class CustomTypeConverterTest extends BaseUnitTestCase {
	@Test
	public void testConverterAppliedScopedRegistration() {
		final List<JavaServiceDescriptor<?>> services = List.of(
				new JavaServiceDescriptor<>(
						MetadataContributor.class,
						PayloadWrapperMetadataContributor.class
				)
		);
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().enableAutoClose()
				.applyClassLoaderService( new ExtraJavaServicesClassLoaderService( services ) )
				.build();
		try (final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( PayloadWrapperConverter.class )
					.addAnnotatedClass( MyEntity.class );
			final MetadataBuilderImplementor metadataBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();

			// now the new scoped way
			final TypeConfiguration bootTypeConfiguration = metadataBuilder.getBootstrapContext().getTypeConfiguration();

			performAssertions( metadataBuilder, bootTypeConfiguration );
		}
	}

	protected void performAssertions(
			MetadataBuilderImplementor metadataBuilder,
			TypeConfiguration bootTypeConfiguration) {
		try ( final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadataBuilder.build().buildSessionFactory()) {
			assertThat(
					sessionFactory.getMetamodel().getTypeConfiguration(),
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

	@Test
	public void testConverterAppliedScopedContributions() {
		final List<JavaServiceDescriptor<?>> services = List.of(
				new JavaServiceDescriptor<>( TypeContributor.class, PayloadWrapperTypeContributorImpl.class )
		);

		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().enableAutoClose()
				.applyClassLoaderService( new ExtraJavaServicesClassLoaderService( services ) )
				.build();
		try ( final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.build() ) {
			final MetadataSources metadataSources = new MetadataSources( ssr )
					.addAnnotatedClass( PayloadWrapperConverter.class )
					.addAnnotatedClass( MyEntity.class );
			final MetadataBuilderImplementor metadataBuilder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();

			// now the new scoped way
			final TypeConfiguration bootTypeConfiguration = metadataBuilder.getBootstrapContext().getTypeConfiguration();

			performAssertions( metadataBuilder, bootTypeConfiguration );
		}
	}

	public static class PayloadWrapperMetadataContributor implements MetadataContributor {
		@Override
		public void contribute(InFlightMetadataCollector metadataCollector, IndexView jandexIndex) {
			final TypeConfiguration typeConfiguration = metadataCollector.getTypeConfiguration();
			typeConfiguration.getJavaTypeRegistry()
					.addDescriptor( PayloadWrapperJavaType.INSTANCE );
			typeConfiguration.getJdbcTypeRegistry()
					.addDescriptor( PayloadWrapperJdbcType.INSTANCE );
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
