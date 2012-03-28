/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFetchProfileElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping.JaxbImport;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbQueryElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSqlQueryElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * Responsible for processing a {@code <hibernate-mapping/>} element.  Allows processing to be coordinated across
 * all hbm files in an ordered fashion.  The order is essentially the same as defined in
 * {@link org.hibernate.metamodel.spi.MetadataSourceProcessor}
 *
 * @author Steve Ebersole
 */
public class HibernateMappingProcessor {
	private final MetadataImplementor metadata;
	private final MappingDocument mappingDocument;

	private Value<ClassLoaderService> classLoaderService = new Value<ClassLoaderService>(
			new Value.DeferredInitializer<ClassLoaderService>() {
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
	}

	private JaxbHibernateMapping mappingRoot() {
		return mappingDocument.getMappingRoot();
	}

	private Origin origin() {
		return mappingDocument.getOrigin();
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

		for ( JaxbHibernateMapping.JaxbDatabaseObject databaseObjectElement : mappingRoot().getDatabaseObject() ) {
			final AuxiliaryDatabaseObject auxiliaryDatabaseObject;
			if ( databaseObjectElement.getDefinition() != null ) {
				final String className = databaseObjectElement.getDefinition().getClazz();
				try {
					auxiliaryDatabaseObject = (AuxiliaryDatabaseObject) classForName( className ).newInstance();
				}
				catch (ClassLoadingException e) {
					throw e;
				}
				catch (Exception e) {
					throw new MappingException(
							"could not instantiate custom database object class [" + className + "]",
							origin()
					);
				}
			}
			else {
				Set<String> dialectScopes = new HashSet<String>();
				if ( databaseObjectElement.getDialectScope() != null ) {
					for ( JaxbHibernateMapping.JaxbDatabaseObject.JaxbDialectScope dialectScope : databaseObjectElement.getDialectScope() ) {
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

		for ( JaxbHibernateMapping.JaxbTypedef typeDefElement : mappingRoot().getTypedef() ) {
			typeDescriptorSources.add( new TypeDescriptorSourceImpl( typeDefElement ) );
		}
	}

	public void collectFilterDefSources(List<FilterDefinitionSource> filterDefinitionSources) {
		if ( mappingRoot().getFilterDef() == null ) {
			return;
		}

		for ( JaxbHibernateMapping.JaxbFilterDef filterDefElement : mappingRoot().getFilterDef() ) {
			filterDefinitionSources.add( new FilterDefinitionSourceImpl( mappingDocument, filterDefElement ) );
		}
	}


	private void processIdentifierGenerators() {
		if ( mappingRoot().getIdentifierGenerator() == null ) {
			return;
		}

		for ( JaxbHibernateMapping.JaxbIdentifierGenerator identifierGeneratorElement : mappingRoot().getIdentifierGenerator() ) {
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

	private void processFetchProfiles(){
		if ( mappingRoot().getFetchProfile() == null ) {
			return;
		}

		processFetchProfiles( mappingRoot().getFetchProfile(), null );
	}

	public void processFetchProfiles(List<JaxbFetchProfileElement> fetchProfiles, String containingEntityName) {
		for ( JaxbFetchProfileElement fetchProfile : fetchProfiles ) {
			String profileName = fetchProfile.getName();
			Set<FetchProfile.Fetch> fetches = new HashSet<FetchProfile.Fetch>();
			for ( JaxbFetchProfileElement.JaxbFetch fetch : fetchProfile.getFetch() ) {
				String entityName = fetch.getEntity() == null ? containingEntityName : fetch.getEntity();
				if ( entityName == null ) {
					throw new MappingException(
							"could not determine entity for fetch-profile fetch [" + profileName + "]:[" +
									fetch.getAssociation() + "]",
							origin()
					);
				}
				fetches.add( new FetchProfile.Fetch( entityName, fetch.getAssociation(), fetch.getStyle() ) );
			}
			metadata.addFetchProfile( new FetchProfile( profileName, fetches ) );
		}
	}

	private void processImports() {
		JaxbHibernateMapping root = mappingRoot();
		for ( JaxbImport importValue : root.getImport() ) {
			String className = mappingDocument.getMappingLocalBindingContext().qualifyClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			metadata.addImport( className, rename );
		}
		if ( root.isAutoImport() ) {
			for ( Object obj : root.getClazzOrSubclassOrJoinedSubclass() ) {
				EntityElement entityElement = ( EntityElement ) obj;
				String qualifiedName = bindingContext().determineEntityName( entityElement );
				metadata.addImport( entityElement.getEntityName() == null
									? entityElement.getName()
									: entityElement.getEntityName(), qualifiedName );
			}
		}
	}

	private void processResultSetMappings() {
		if ( mappingRoot().getResultset() == null ) {
			return;
		}

//			bindResultSetMappingDefinitions( element, null, mappings );
	}

	private void processNamedQueries() {
		if ( mappingRoot().getQueryOrSqlQuery() == null ) {
			return;
		}

		for ( Object queryOrSqlQuery : mappingRoot().getQueryOrSqlQuery() ) {
			if ( JaxbQueryElement.class.isInstance( queryOrSqlQuery ) ) {
//					bindNamedQuery( element, null, mappings );
			}
			else if ( JaxbSqlQueryElement.class.isInstance( queryOrSqlQuery ) ) {
//				bindNamedSQLQuery( element, null, mappings );
			}
			else {
				throw new MappingException(
						"unknown type of query: " +
								queryOrSqlQuery.getClass().getName(), origin()
				);
			}
		}
	}
}