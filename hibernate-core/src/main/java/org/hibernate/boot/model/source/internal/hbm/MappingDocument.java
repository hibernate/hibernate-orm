/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Set;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAuxiliaryDatabaseObjectType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmClassRenameType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdentifierGeneratorDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeDefinitionType;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.internal.TypeDefinitionRegistryStandardImpl;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.source.internal.OverriddenMappingDefaults;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.query.HbmResultSetMappingDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;

import org.jboss.logging.Logger;

import static org.hibernate.boot.model.source.internal.hbm.Helper.collectToolingHints;

/**
 * Aggregates together information about a mapping document.
 *
 * @author Steve Ebersole
 */
public class MappingDocument implements HbmLocalMetadataBuildingContext, MetadataSourceProcessor {
	private static final Logger LOG = Logger.getLogger( MappingDocument.class );

	private final JaxbHbmHibernateMapping documentRoot;
	private final Origin origin;
	private final MetadataBuildingContext rootBuildingContext;
	private final EffectiveMappingDefaults mappingDefaults;

	private final ToolingHintContext toolingHintContext;

	private final TypeDefinitionRegistry typeDefinitionRegistry;

	private final String contributor;

	public MappingDocument(
			String contributor,
			JaxbHbmHibernateMapping documentRoot,
			Origin origin,
			MetadataBuildingContext rootBuildingContext) {
		this.contributor = contributor;
		this.documentRoot = documentRoot;
		this.origin = origin;
		this.rootBuildingContext = rootBuildingContext;

		// todo : allow for a split in default-lazy for singular/plural

		mappingDefaults =
				new OverriddenMappingDefaults.Builder( rootBuildingContext.getEffectiveDefaults() )
						.setImplicitSchemaName( documentRoot.getSchema() )
						.setImplicitCatalogName( documentRoot.getCatalog() )
						.setImplicitPackageName( documentRoot.getPackage() )
						.setImplicitPropertyAccessorName( documentRoot.getDefaultAccess() )
//						.setImplicitCascadeStyleName( documentRoot.getDefaultCascade() )
						.setEntitiesImplicitlyLazy( documentRoot.isDefaultLazy() )
						.setAutoImportEnabled( documentRoot.isAutoImport() )
						.setPluralAttributesImplicitlyLazy( documentRoot.isDefaultLazy() )
						.build();

		toolingHintContext = collectToolingHints( null, documentRoot );

		typeDefinitionRegistry =
				new TypeDefinitionRegistryStandardImpl( rootBuildingContext.getTypeDefinitionRegistry() );
	}

	public JaxbHbmHibernateMapping getDocumentRoot() {
		return documentRoot;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}


	@Override
	public String determineEntityName(EntityInfo entityElement) {
		return determineEntityName( entityElement.getEntityName(), entityElement.getName() );
	}

	private static String qualifyIfNeeded(String name, String implicitPackageName) {
		if ( name == null ) {
			return null;
		}
		if ( name.indexOf( '.' ) < 0 && implicitPackageName != null ) {
			return implicitPackageName + '.' + name;
		}
		else {
			return name;
		}
	}

	@Override
	public String determineEntityName(String entityName, String clazz) {
		return entityName != null
				? entityName
				: qualifyIfNeeded( clazz, mappingDefaults.getDefaultPackageName() );
	}

	@Override
	public String qualifyClassName(String name) {
		return qualifyIfNeeded( name, mappingDefaults.getDefaultPackageName() );
	}

	@Override
	public PersistentClass findEntityBinding(String entityName, String clazz) {
		return getMetadataCollector().getEntityBinding( determineEntityName( entityName, clazz ) );
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return rootBuildingContext.getBootstrapContext();
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return rootBuildingContext.getBuildingOptions();
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return rootBuildingContext.getMetadataCollector();
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return rootBuildingContext.getObjectNameNormalizer();
	}

	@Override
	public TypeDefinitionRegistry getTypeDefinitionRegistry() {
		return typeDefinitionRegistry;
	}

	@Override
	public String getCurrentContributorName() {
		return contributor;
	}

	@Override
	public void prepare() {
		// nothing to do here
	}

	@Override
	public void processTypeDefinitions() {
		for ( var typeDef : documentRoot.getTypedef() ) {
			TypeDefinitionBinder.processTypeDefinition( this, typeDef );
		}
	}

	@Override
	public void processQueryRenames() {
		for ( var renameBinding : documentRoot.getImport() ) {
			final String name = qualifyClassName( renameBinding.getClazz() );
			final String rename = renameBinding.getRename() == null
					? StringHelper.unqualify( name )
					: renameBinding.getRename();
			getMetadataCollector().addImport( rename, name );
			LOG.tracef( "Import (query rename): %s -> %s", rename, name );
		}
	}

	@Override
	public void processFilterDefinitions() {
		for ( var filterDefinitionBinding : documentRoot.getFilterDef() ) {
			FilterDefinitionBinder.processFilterDefinition( this, filterDefinitionBinding );
		}
	}

	@Override
	public void processFetchProfiles() {
		for ( var fetchProfileBinding : documentRoot.getFetchProfile() ) {
			FetchProfileBinder.processFetchProfile( this, fetchProfileBinding );
		}
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
		for ( var auxDbObjectBinding : documentRoot.getDatabaseObject() ) {
			AuxiliaryDatabaseObjectBinder.processAuxiliaryDatabaseObject( this, auxDbObjectBinding );
		}
	}

	@Override
	public void processNamedQueries() {
		for ( var namedQuery : documentRoot.getQuery() ) {
			NamedQueryBinder.processNamedQuery( this, namedQuery );
		}
		for ( var namedQuery : documentRoot.getSqlQuery() ) {
			NamedQueryBinder.processNamedNativeQuery( this, namedQuery );
		}
	}

	@Override
	public void processIdentifierGenerators() {
		for ( var identifierGenerator : documentRoot.getIdentifierGenerator() ) {
			IdentifierGeneratorDefinitionBinder.processIdentifierGeneratorDefinition( this, identifierGenerator );
		}
	}

	@Override
	public void prepareForEntityHierarchyProcessing() {
		// should *not* be called
	}

	@Override
	public void processEntityHierarchies(Set<String> processedEntityNames) {
		// should *not* be called
	}

	@Override
	public void postProcessEntityHierarchies() {
		// should *not* be called
	}

	@Override
	public void processResultSetMappings() {
		documentRoot.getResultset()
				.forEach( hbmResultSetMapping ->
						getMetadataCollector().addResultSetMapping(
								new HbmResultSetMappingDescriptor( hbmResultSetMapping, rootBuildingContext ) ) );
	}

	@Override
	public void finishUp() {
		// nothing to do
	}

}
