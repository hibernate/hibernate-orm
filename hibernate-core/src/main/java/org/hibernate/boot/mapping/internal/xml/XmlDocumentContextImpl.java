/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml;

import org.hibernate.boot.mapping.internal.context.RootMappingDefaults;
import org.hibernate.boot.model.source.internal.OverriddenMappingDefaults;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class XmlDocumentContextImpl implements XmlDocumentContext {
	private final XmlDocument xmlDocument;
	private final EffectiveMappingDefaults effectiveDefaults;
	private final ModelsContext modelBuildingContext;
	private final ClassLoaderService classLoaderService;
	private final TypeConfiguration typeConfiguration;
	private final JdbcServices jdbcServices;

	public XmlDocumentContextImpl(
			XmlDocument xmlDocument,
			RootMappingDefaults mappingDefaults,
			ModelsContext modelBuildingContext,
			MetadataBuildingContext metadataBuildingContext) {
		this.xmlDocument = xmlDocument;
		this.effectiveDefaults = buildEffectiveDefaults( xmlDocument, mappingDefaults );
		this.modelBuildingContext = modelBuildingContext;
		this.classLoaderService = metadataBuildingContext.getClassLoaderService();
		this.typeConfiguration = metadataBuildingContext.getTypeConfiguration();
		this.jdbcServices = metadataBuildingContext.getJdbcServices();
	}

	@Override
	public XmlDocument getXmlDocument() {
		return xmlDocument;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
		return effectiveDefaults;
	}

	@Override
	public ModelsContext getModelsContext() {
		return modelBuildingContext;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	private static EffectiveMappingDefaults buildEffectiveDefaults(
			XmlDocument xmlDocument,
			RootMappingDefaults mappingDefaults) {
		final XmlDocument.Defaults documentDefaults = xmlDocument.getDefaults();
		final OverriddenMappingDefaults.Builder builder = new OverriddenMappingDefaults.Builder( mappingDefaults );

		if ( StringHelper.isNotEmpty( documentDefaults.getCatalog() ) ) {
			builder.setImplicitCatalogName( documentDefaults.getCatalog() );
		}

		if ( StringHelper.isNotEmpty( documentDefaults.getSchema() ) ) {
			builder.setImplicitSchemaName( documentDefaults.getSchema() );
		}

		if ( documentDefaults.isAutoImport() ) {
			builder.setAutoImportEnabled( true );
		}

		if ( StringHelper.isNotEmpty( documentDefaults.getPackage() ) ) {
			builder.setImplicitPackageName( documentDefaults.getPackage() );
		}

		if ( documentDefaults.getAccessType() != null ) {
			builder.setImplicitPropertyAccessType( documentDefaults.getAccessType() );
		}

		if ( StringHelper.isNotEmpty( documentDefaults.getAccessorStrategy() ) ) {
			builder.setImplicitPropertyAccessorName( documentDefaults.getAccessorStrategy() );
		}

		if ( documentDefaults.isLazinessImplied() ) {
			builder.setEntitiesImplicitlyLazy( true );
			builder.setPluralAttributesImplicitlyLazy( true );
		}

		return builder.build();
	}
}
