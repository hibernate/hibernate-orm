/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.boot.jaxb.hbm.spi.ResultSetMappingBindingDefinition;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.source.internal.OverriddenMappingDefaults;
import org.hibernate.boot.model.source.spi.MetadataSourceProcessor;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;

import org.jboss.logging.Logger;

/**
 * Aggregates together information about a mapping document.
 *
 * @author Steve Ebersole
 */
public class MappingDocument implements HbmLocalMetadataBuildingContext, MetadataSourceProcessor {
	private static final Logger log = Logger.getLogger( MappingDocument.class );

	private final JaxbHbmHibernateMapping documentRoot;
	private final Origin origin;
	private final MetadataBuildingContext rootBuildingContext;
	private final MappingDefaults mappingDefaults;

	private final ToolingHintContext toolingHintContext;


	public MappingDocument(
			JaxbHbmHibernateMapping documentRoot,
			Origin origin,
			MetadataBuildingContext rootBuildingContext) {
		this.documentRoot = documentRoot;
		this.origin = origin;
		this.rootBuildingContext = rootBuildingContext;

		// todo : allow for a split in default-lazy for singular/plural

		this.mappingDefaults = new OverriddenMappingDefaults.Builder( rootBuildingContext.getMappingDefaults() )
				.setImplicitSchemaName( documentRoot.getSchema() )
				.setImplicitCatalogName( documentRoot.getCatalog() )
				.setImplicitPackageName( documentRoot.getPackage() )
				.setImplicitPropertyAccessorName( documentRoot.getDefaultAccess() )
				.setImplicitCascadeStyleName( documentRoot.getDefaultCascade() )
				.setEntitiesImplicitlyLazy( documentRoot.isDefaultLazy() )
				.setAutoImportEnabled( documentRoot.isAutoImport() )
				.setPluralAttributesImplicitlyLazy( documentRoot.isDefaultLazy() )
				.build();

		this.toolingHintContext = Helper.collectToolingHints( null, documentRoot );
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
		return name;
	}

	@Override
	public String determineEntityName(String entityName, String clazz) {
		return entityName != null
				? entityName
				: qualifyIfNeeded( clazz, mappingDefaults.getImplicitPackageName() );
	}

	@Override
	public String qualifyClassName(String name) {
		return qualifyIfNeeded( name, mappingDefaults.getImplicitPackageName() );
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
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return rootBuildingContext.getMetadataCollector();
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return rootBuildingContext.getClassLoaderAccess();
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return rootBuildingContext.getObjectNameNormalizer();
	}

	@Override
	public void prepare() {
		// nothing to do here
	}

	@Override
	public void processTypeDefinitions() {
		for ( JaxbHbmTypeDefinitionType typeDef : documentRoot.getTypedef() ) {
			TypeDefinitionBinder.processTypeDefinition( this, typeDef );
		}
	}

	@Override
	public void processQueryRenames() {
		for ( JaxbHbmClassRenameType renameBinding : documentRoot.getImport() ) {
			final String name = qualifyClassName( renameBinding.getClazz() );
			final String rename = renameBinding.getRename() == null
					? StringHelper.unqualify( name )
					: renameBinding.getRename();
			getMetadataCollector().addImport( rename, name );
			log.debugf( "Import (query rename): %s -> %s", rename, name );
		}
	}

	@Override
	public void processFilterDefinitions() {
		for ( JaxbHbmFilterDefinitionType filterDefinitionBinding : documentRoot.getFilterDef() ) {
			FilterDefinitionBinder.processFilterDefinition( this, filterDefinitionBinding );
		}
	}

	@Override
	public void processFetchProfiles() {
		for ( JaxbHbmFetchProfileType fetchProfileBinding : documentRoot.getFetchProfile() ) {
			FetchProfileBinder.processFetchProfile( this, fetchProfileBinding );
		}
	}

	@Override
	public void processAuxiliaryDatabaseObjectDefinitions() {
		for ( JaxbHbmAuxiliaryDatabaseObjectType auxDbObjectBinding : documentRoot.getDatabaseObject() ) {
			AuxiliaryDatabaseObjectBinder.processAuxiliaryDatabaseObject( this, auxDbObjectBinding );
		}
	}

	@Override
	public void processNamedQueries() {
		for ( JaxbHbmNamedQueryType namedQuery : documentRoot.getQuery() ) {
			NamedQueryBinder.processNamedQuery( this, namedQuery );
		}
		for ( JaxbHbmNamedNativeQueryType namedQuery : documentRoot.getSqlQuery() ) {
			NamedQueryBinder.processNamedNativeQuery( this, namedQuery );
		}
	}

	@Override
	public void processIdentifierGenerators() {
		for ( JaxbHbmIdentifierGeneratorDefinitionType identifierGenerator : documentRoot.getIdentifierGenerator() ) {
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
		for ( ResultSetMappingBindingDefinition resultSetMappingBinding : documentRoot.getResultset() ) {
			final ResultSetMappingDefinition binding = ResultSetMappingBinder.bind( resultSetMappingBinding, this );
			getMetadataCollector().addResultSetMapping( binding );
		}
	}

	@Override
	public void finishUp() {
		// nothing to do
	}

}
