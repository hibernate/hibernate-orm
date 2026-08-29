/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.model.internal.BinderHelper;
import org.hibernate.boot.model.internal.GeneratorAnnotationHelper;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PackageInfoCacheTests {
	private static final String PACKAGE_NAME = "example.model";
	private static final String PACKAGE_INFO_NAME = PACKAGE_NAME + ".package-info";

	@Test
	@ServiceRegistry
	void cachesPresentPackageInfoAcrossAnnotationAndGeneratorLookups(ServiceRegistryScope registryScope) {
		final var classDetailsRegistry = mock( ClassDetailsRegistry.class );
		final var packageInfoDetails = mock( ClassDetails.class );
		final var softDelete = mock( SoftDelete.class );
		final var nationalized = mock( Nationalized.class );
		final var context = createContext( classDetailsRegistry, registryScope.getRegistry() );
		when( classDetailsRegistry.resolveClassDetails( PACKAGE_INFO_NAME ) ).thenReturn( packageInfoDetails );
		when( packageInfoDetails.getAnnotationUsage( SoftDelete.class, context.modelsContext ) ).thenReturn( softDelete );
		when( packageInfoDetails.getAnnotationUsage( Nationalized.class, context.modelsContext ) )
				.thenReturn( nationalized );

		assertThat( BinderHelper.extractFromPackage( SoftDelete.class, context.entityDetails, context.buildingContext ) )
				.isSameAs( softDelete );
		assertThat( BinderHelper.extractFromPackage( Nationalized.class, context.entityDetails, context.buildingContext ) )
				.isSameAs( nationalized );
		assertThat( GeneratorAnnotationHelper.locatePackageInfoDetails( context.entityDetails, context.buildingContext ) )
				.isSameAs( packageInfoDetails );
		verify( classDetailsRegistry ).resolveClassDetails( PACKAGE_INFO_NAME );
	}

	@Test
	@ServiceRegistry
	void cachesMissingPackageInfoAcrossAnnotationAndGeneratorLookups(ServiceRegistryScope registryScope) {
		final var classDetailsRegistry = mock( ClassDetailsRegistry.class );
		final var context = createContext( classDetailsRegistry, registryScope.getRegistry() );
		when( classDetailsRegistry.resolveClassDetails( PACKAGE_INFO_NAME ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThat( BinderHelper.extractFromPackage( SoftDelete.class, context.entityDetails, context.buildingContext ) )
				.isNull();
		assertThat( BinderHelper.extractFromPackage( Nationalized.class, context.entityDetails, context.buildingContext ) )
				.isNull();
		assertThat( GeneratorAnnotationHelper.locatePackageInfoDetails( context.entityDetails, context.buildingContext ) )
				.isNull();
		verify( classDetailsRegistry ).resolveClassDetails( PACKAGE_INFO_NAME );
	}

	@Test
	@ServiceRegistry
	void keepsPackageInfoCachesIsolatedBetweenMetadataBootstraps(ServiceRegistryScope registryScope) {
		final var firstRegistry = mock( ClassDetailsRegistry.class );
		final var secondRegistry = mock( ClassDetailsRegistry.class );
		final var firstPackageInfo = mock( ClassDetails.class );
		final var secondPackageInfo = mock( ClassDetails.class );
		final var firstContext = createContext( firstRegistry, registryScope.getRegistry() );
		final var secondContext = createContext( secondRegistry, registryScope.getRegistry() );
		when( firstRegistry.resolveClassDetails( PACKAGE_INFO_NAME ) ).thenReturn( firstPackageInfo );
		when( secondRegistry.resolveClassDetails( PACKAGE_INFO_NAME ) ).thenReturn( secondPackageInfo );

		assertThat( firstContext.metadataCollector.getPackageInfoClassDetails( PACKAGE_NAME ) )
				.containsSame( firstPackageInfo );
		assertThat( firstContext.metadataCollector.getPackageInfoClassDetails( PACKAGE_NAME ) )
				.containsSame( firstPackageInfo );
		assertThat( secondContext.metadataCollector.getPackageInfoClassDetails( PACKAGE_NAME ) )
				.containsSame( secondPackageInfo );
		assertThat( secondContext.metadataCollector.getPackageInfoClassDetails( PACKAGE_NAME ) )
				.containsSame( secondPackageInfo );
		verify( firstRegistry, times( 1 ) ).resolveClassDetails( PACKAGE_INFO_NAME );
		verify( secondRegistry, times( 1 ) ).resolveClassDetails( PACKAGE_INFO_NAME );
	}

	private static TestContext createContext(
			ClassDetailsRegistry classDetailsRegistry,
			StandardServiceRegistry serviceRegistry) {
		final var modelsContext = mock( ModelsContext.class );
		final var bootstrapContext = mock( BootstrapContext.class );
		final var buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		buildingOptions.setBootstrapContext( bootstrapContext );
		when( modelsContext.getClassDetailsRegistry() ).thenReturn( classDetailsRegistry );
		when( bootstrapContext.getModelsContext() ).thenReturn( modelsContext );
		when( bootstrapContext.getSqlFunctions() ).thenReturn( Map.of() );
		when( bootstrapContext.getAuxiliaryDatabaseObjectList() ).thenReturn( List.of() );
		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingOptions );
		final var buildingContext = mock( MetadataBuildingContext.class );
		when( buildingContext.getBootstrapContext() ).thenReturn( bootstrapContext );
		when( buildingContext.getMetadataCollector() ).thenReturn( metadataCollector );
		final var entityDetails = mock( ClassDetails.class );
		when( entityDetails.getName() ).thenReturn( PACKAGE_NAME + ".Entity" );
		return new TestContext( modelsContext, metadataCollector, buildingContext, entityDetails );
	}

	private record TestContext(
			ModelsContext modelsContext,
			InFlightMetadataCollectorImpl metadataCollector,
			MetadataBuildingContext buildingContext,
			ClassDetails entityDetails) {
	}
}
