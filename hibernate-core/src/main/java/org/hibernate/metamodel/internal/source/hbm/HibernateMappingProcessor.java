/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbDatabaseObjectElement;
import org.hibernate.jaxb.spi.hbm.JaxbDialectScopeElement;
import org.hibernate.jaxb.spi.hbm.JaxbFetchProfileElement;
import org.hibernate.jaxb.spi.hbm.JaxbFilterDefElement;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbIdentifierGeneratorElement;
import org.hibernate.jaxb.spi.hbm.JaxbImportElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbResultsetElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbTypedefElement;
import org.hibernate.jaxb.spi.hbm.JaxbUnionSubclassElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;

/**
 * Responsible for processing a {@code <hibernate-mapping/>} element.  Allows processing to be coordinated across
 * all hbm files in an ordered fashion.  The order is essentially the same as defined in
 * {@link org.hibernate.metamodel.spi.MetadataSourceProcessor}
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class HibernateMappingProcessor {
	private static final CoreMessageLogger LOG = Logger
			.getMessageLogger( CoreMessageLogger.class, HibernateMappingProcessor.class.getName() );

	private final MetadataImplementor metadata;
	private final MappingDocument mappingDocument;

	private final ValueHolder<ClassLoaderService> classLoaderService = new ValueHolder<ClassLoaderService>(
			new ValueHolder.DeferredInitializer<ClassLoaderService>() {
				@Override
				public ClassLoaderService initialize() {
					return metadata.getServiceRegistry().getService( ClassLoaderService.class );
				}
			}
	);

	public HibernateMappingProcessor(MetadataImplementor metadata, MappingDocument mappingDocument) {
		this.metadata = metadata;
		this.mappingDocument = mappingDocument;
		processDatabaseObjectDefinitions();
		processIdentifierGenerators();
	}

	private JaxbHibernateMapping mappingRoot() {
		return mappingDocument.getMappingRoot();
	}

	private HbmBindingContext bindingContext() {
		return mappingDocument.getMappingLocalBindingContext();
	}

	private <T> Class<T> classForName(String name) {
		return classLoaderService.getValue().classForName( bindingContext().qualifyClassName( name ) );
	}

	private void processDatabaseObjectDefinitions() {
		if ( mappingRoot().getDatabaseObject() == null ) {
			return;
		}

		for ( JaxbDatabaseObjectElement databaseObjectElement : mappingRoot().getDatabaseObject() ) {
			final AuxiliaryDatabaseObject auxiliaryDatabaseObject;
			if ( databaseObjectElement.getDefinition() != null ) {
				final String className = databaseObjectElement.getDefinition().getClazz();
				try {
					auxiliaryDatabaseObject = (AuxiliaryDatabaseObject) classForName( className ).newInstance();
				}
				catch ( ClassLoadingException e ) {
					throw e;
				}
				catch ( Exception e ) {
					throw bindingContext().makeMappingException( "could not instantiate custom database object class [" + className + "]" );
				}
			}
			else {
				Set<String> dialectScopes = new HashSet<String>();
				if ( databaseObjectElement.getDialectScope() != null ) {
					for ( JaxbDialectScopeElement dialectScope : databaseObjectElement.getDialectScope() ) {
						dialectScopes.add( dialectScope.getName() );
					}
				}
				auxiliaryDatabaseObject = new BasicAuxiliaryDatabaseObjectImpl(
						metadata.getDatabase().getDefaultSchema(),
						databaseObjectElement.getCreate(),
						databaseObjectElement.getDrop(),
						dialectScopes
				);
			}
			metadata.getDatabase().addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		}
	}

	public void collectTypeDescriptorSources(List<TypeDescriptorSource> typeDescriptorSources) {
		if ( mappingRoot().getTypedef() == null ) {
			return;
		}

		for ( JaxbTypedefElement typeDefElement : mappingRoot().getTypedef() ) {
			typeDescriptorSources.add( new TypeDescriptorSourceImpl( typeDefElement ) );
		}
	}

	public void collectFilterDefSources(List<FilterDefinitionSource> filterDefinitionSources) {
		if ( mappingRoot().getFilterDef() == null ) {
			return;
		}

		for ( JaxbFilterDefElement filterDefElement : mappingRoot().getFilterDef() ) {
			filterDefinitionSources.add( new FilterDefinitionSourceImpl( mappingDocument, filterDefElement ) );
		}
	}


	private void processIdentifierGenerators() {
		if ( mappingRoot().getIdentifierGenerator() == null ) {
			return;
		}

		for ( JaxbIdentifierGeneratorElement identifierGeneratorElement : mappingRoot().getIdentifierGenerator() ) {
			metadata.registerIdentifierGenerator(
					identifierGeneratorElement.getName(),
					identifierGeneratorElement.getClazz()
			);
		}
	}

	public void processMappingDependentMetadata() {
		processFetchProfiles();
		processImports();
		processResultSetMappings();
		processNamedQueries();
	}

	private void processFetchProfiles() {
		processFetchProfiles( mappingRoot().getFetchProfile(), null );
		for ( JaxbClassElement classElement : mappingRoot().getClazz() ) {
			processFetchProfile( classElement );

			// processing fetch profiles defined in the <joined-subclass>
			processFetchProfilesInJoinedSubclass( classElement.getJoinedSubclass() );
			// <union-subclass>
			processFetchProfilesInUnionSubclass( classElement.getUnionSubclass() );
			// <subclass>
			processFetchProfilesInSubclass( classElement.getSubclass() );
		}
	}

	private void processFetchProfilesInSubclass(List<JaxbSubclassElement> subclass) {
		for ( JaxbSubclassElement subclassElement : subclass ) {
			processFetchProfile( subclassElement );
			processFetchProfilesInSubclass( subclassElement.getSubclass() );
		}
	}

	private void processFetchProfile(final EntityElement entityElement){
		processFetchProfiles(
				entityElement.getFetchProfile(),
				bindingContext().qualifyClassName( entityElement.getName() )
		);
	}

	private void processFetchProfilesInUnionSubclass(List<JaxbUnionSubclassElement> unionSubclass) {
		for ( JaxbUnionSubclassElement subclassElement : unionSubclass ) {
			processFetchProfile( subclassElement );
			processFetchProfilesInUnionSubclass( subclassElement.getUnionSubclass() );
		}
	}

	private void processFetchProfilesInJoinedSubclass(List<JaxbJoinedSubclassElement> joinedSubclassElements) {
		for ( JaxbJoinedSubclassElement subclassElement : joinedSubclassElements ) {
			processFetchProfile( subclassElement );
			processFetchProfilesInJoinedSubclass( subclassElement.getJoinedSubclass() );
		}
	}

	public void processFetchProfiles(List<JaxbFetchProfileElement> fetchProfiles, String containingEntityName) {
		for ( JaxbFetchProfileElement fetchProfile : fetchProfiles ) {
			String profileName = fetchProfile.getName();
			Set<FetchProfile.Fetch> fetches = new HashSet<FetchProfile.Fetch>();
			for ( JaxbFetchProfileElement.JaxbFetch fetch : fetchProfile.getFetch() ) {
				String entityName = fetch.getEntity() == null ? containingEntityName : fetch.getEntity();
				if ( entityName == null ) {
					throw bindingContext().makeMappingException( "could not determine entity for fetch-profile fetch [" + profileName + "]:[" +
							fetch.getAssociation() + "]" );
				}
				fetches.add( new FetchProfile.Fetch( entityName, fetch.getAssociation(), fetch.getStyle().value() ) );
			}
			metadata.addFetchProfile( new FetchProfile( profileName, fetches ) );
		}
	}

	private void processImports() {
		JaxbHibernateMapping root = mappingRoot();
		for ( JaxbImportElement importValue : root.getImport() ) {
			String className = bindingContext().qualifyClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			metadata.addImport( rename, className );
		}
		if ( root.isAutoImport() ) {
			processEntityElementsImport( root.getClazz() );
			processEntityElementsImport( root.getJoinedSubclass() );
			processEntityElementsImport( root.getUnionSubclass() );
			processEntityElementsImport( root.getSubclass() );
		}
	}

	private void processEntityElementsImport(List<? extends EntityElement> entityElements) {
		for ( final EntityElement element : entityElements ) {
			processEntityElementImport( element );
		}
	}

	private void processEntityElementImport(EntityElement entityElement) {
		final String qualifiedName = bindingContext().determineEntityName( entityElement );
		final String importName = entityElement.getEntityName() == null
				? entityElement.getName()
				: entityElement.getEntityName();
		metadata.addImport( importName, qualifiedName );
		metadata.addImport( StringHelper.unqualify(importName  ), qualifiedName );

		if ( JaxbClassElement.class.isInstance( entityElement ) ) {
			processEntityElementsImport( ( (JaxbClassElement) entityElement ).getSubclass() );
			processEntityElementsImport( ( (JaxbClassElement) entityElement ).getJoinedSubclass() );
			processEntityElementsImport( ( (JaxbClassElement) entityElement ).getUnionSubclass() );
		}
		else if ( JaxbSubclassElement.class.isInstance( entityElement ) ) {
			processEntityElementsImport( ( (JaxbSubclassElement) entityElement ).getSubclass() );
		}
		else if ( JaxbJoinedSubclassElement.class.isInstance( entityElement ) ) {
			processEntityElementsImport( ( (JaxbJoinedSubclassElement) entityElement ).getJoinedSubclass() );
		}
		else if ( JaxbUnionSubclassElement.class.isInstance( entityElement ) ) {
			processEntityElementsImport( ( (JaxbUnionSubclassElement) entityElement ).getUnionSubclass() );
		}
	}

	private void processResultSetMappings() {
		List<JaxbResultsetElement> resultsetElements = new ArrayList<JaxbResultsetElement>();

		addAllIfNotEmpty( resultsetElements, mappingRoot().getResultset() );
		findResultSets( resultsetElements, mappingRoot().getClazz() );
		findResultSets( resultsetElements, mappingRoot().getJoinedSubclass() );
		findResultSets( resultsetElements, mappingRoot().getUnionSubclass() );
		findResultSets( resultsetElements, mappingRoot().getSubclass() );
		if ( resultsetElements.isEmpty() ) {
			return;
		}
		for ( final JaxbResultsetElement element : resultsetElements ) {
			metadata.addResultSetMapping(
					ResultSetMappingBinder.buildResultSetMappingDefinitions(
							element, bindingContext(), metadata
					)
			);
		}

	}

	private static void findResultSets(List<JaxbResultsetElement> resultsetElements, List<? extends EntityElement> entityElements) {
		for ( final EntityElement element : entityElements ) {
			addAllIfNotEmpty( resultsetElements, element.getResultset() );
		}
	}

	private static void addAllIfNotEmpty(List target, List values) {
		if ( CollectionHelper.isNotEmpty( values ) ) {
			target.addAll( values );
		}
	}

	private void processNamedQueries() {
		for ( final JaxbQueryElement element : mappingRoot().getQuery() ) {
			NamedQueryBindingHelper.bindNamedQuery( element, metadata );
		}
		for ( final JaxbSqlQueryElement element : mappingRoot().getSqlQuery() ) {
			NamedQueryBindingHelper.bindNamedSQLQuery(
					element,
					bindingContext(),
					metadata
			);
		}
	}

}