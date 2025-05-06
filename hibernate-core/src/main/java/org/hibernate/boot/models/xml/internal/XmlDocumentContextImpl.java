/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.model.source.internal.OverriddenMappingDefaults;
import org.hibernate.boot.models.xml.spi.XmlDocument;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

/**
 * @author Steve Ebersole
 */
public class XmlDocumentContextImpl implements XmlDocumentContext {
	private final XmlDocument xmlDocument;
	private final EffectiveMappingDefaults effectiveDefaults;
	private final ModelsContext modelBuildingContext;
	private final BootstrapContext bootstrapContext;

	public XmlDocumentContextImpl(
			XmlDocument xmlDocument,
			RootMappingDefaults mappingDefaults,
			ModelsContext modelBuildingContext,
			BootstrapContext bootstrapContext) {
		this.xmlDocument = xmlDocument;
		this.effectiveDefaults = buildEffectiveDefaults( xmlDocument, mappingDefaults );
		this.modelBuildingContext = modelBuildingContext;
		this.bootstrapContext = bootstrapContext;
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
	public ModelsContext getModelBuildingContext() {
		return modelBuildingContext;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
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
