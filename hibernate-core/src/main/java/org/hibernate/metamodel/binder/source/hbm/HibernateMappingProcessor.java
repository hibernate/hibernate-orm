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
package org.hibernate.metamodel.binder.source.hbm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.Value;
import org.hibernate.metamodel.binder.EntityBinder;
import org.hibernate.metamodel.binder.MappingException;
import org.hibernate.metamodel.binder.Origin;
import org.hibernate.metamodel.binder.source.EntityDescriptor;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetaAttributeContext;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binder.source.internal.JaxbRoot;
import org.hibernate.metamodel.binder.source.internal.OverriddenMappingDefaults;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLFetchProfileElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLParamElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLQueryElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSqlQueryElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLUnionSubclassElement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.type.Type;

/**
 * Responsible for processing a {@code <hibernate-mapping/>} element.  Allows processing to be coordinated across
 * all hbm files in an ordered fashion.  The order is essentially the same as defined in
 * {@link org.hibernate.metamodel.binder.source.SourceProcessor}
 *
 * @author Steve Ebersole
 */
public class HibernateMappingProcessor implements HbmBindingContext {
	private final HbmSourceProcessorImpl hbmHandler;
	private final JaxbRoot<XMLHibernateMapping> jaxbRoot;

	private final XMLHibernateMapping hibernateMapping;

	private final MappingDefaults mappingDefaults;
	private final MetaAttributeContext metaAttributeContext;


	private final EntityBinder entityBinder;

	private final boolean autoImport;

	public HibernateMappingProcessor(HbmSourceProcessorImpl hbmHandler, JaxbRoot<XMLHibernateMapping> jaxbRoot) {
		this.hbmHandler = hbmHandler;
		this.jaxbRoot = jaxbRoot;

		this.hibernateMapping = jaxbRoot.getRoot();
		this.mappingDefaults = new OverriddenMappingDefaults(
				hbmHandler.getMappingDefaults(),
				hibernateMapping.getPackage(),
				hibernateMapping.getSchema(),
				hibernateMapping.getCatalog(),
				null,	// idColumnName
				null,	// discriminatorColumnName
				hibernateMapping.getDefaultCascade(),
				hibernateMapping.getDefaultAccess(),
				hibernateMapping.isDefaultLazy()
		);

		this.autoImport = hibernateMapping.isAutoImport();

		this.entityBinder = new EntityBinder( this );

		this.metaAttributeContext = extractMetaAttributes();
	}

	private MetaAttributeContext extractMetaAttributes() {
		return hibernateMapping.getMeta() == null
				? new MetaAttributeContext( hbmHandler.getMetadataImplementor().getGlobalMetaAttributeContext() )
				: HbmHelper.extractMetaAttributeContext( hibernateMapping.getMeta(), true, hbmHandler.getMetadataImplementor().getGlobalMetaAttributeContext() );
	}

	XMLHibernateMapping getHibernateMapping() {
		return hibernateMapping;
	}

	@Override
	public boolean isAutoImport() {
		return autoImport;
	}

	@Override
	public Origin getOrigin() {
		return jaxbRoot.getOrigin();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return getMetadataImplementor().getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return getMetadataImplementor().getOptions().getNamingStrategy();
	}

    @Override
    public boolean isGloballyQuotedIdentifiers() {
        return getMetadataImplementor().isGloballyQuotedIdentifiers();
    }

    @Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return hbmHandler.getMetadataImplementor();
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return getMetadataImplementor().locateClassByName( name );
	}

	@Override
	public JavaType makeJavaType(String className) {
		return getMetadataImplementor().makeJavaType( className );
	}

	public void processIndependentMetadata() {
		processDatabaseObjectDefinitions();
		processTypeDefinitions();
	}

	private void processDatabaseObjectDefinitions() {
		if ( hibernateMapping.getDatabaseObject() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLDatabaseObject databaseObjectElement : hibernateMapping.getDatabaseObject() ) {
			final AuxiliaryDatabaseObject auxiliaryDatabaseObject;
			if ( databaseObjectElement.getDefinition() != null ) {
				final String className = databaseObjectElement.getDefinition().getClazz();
				try {
					auxiliaryDatabaseObject = (AuxiliaryDatabaseObject) classLoaderService.getValue().classForName( className ).newInstance();
				}
				catch (ClassLoadingException e) {
					throw e;
				}
				catch (Exception e) {
					throw new MappingException(
							"could not instantiate custom database object class [" + className + "]",
							jaxbRoot.getOrigin()
					);
				}
			}
			else {
				Set<String> dialectScopes = new HashSet<String>();
				if ( databaseObjectElement.getDialectScope() != null ) {
					for ( XMLHibernateMapping.XMLDatabaseObject.XMLDialectScope dialectScope : databaseObjectElement.getDialectScope() ) {
						dialectScopes.add( dialectScope.getName() );
					}
				}
				auxiliaryDatabaseObject = new BasicAuxiliaryDatabaseObjectImpl(
						databaseObjectElement.getCreate(),
						databaseObjectElement.getDrop(),
						dialectScopes
				);
			}
			getMetadataImplementor().addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		}
	}

	private void processTypeDefinitions() {
		if ( hibernateMapping.getTypedef() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLTypedef typedef : hibernateMapping.getTypedef() ) {
			final Map<String, String> parameters = new HashMap<String, String>();
			for ( XMLParamElement paramElement : typedef.getParam() ) {
				parameters.put( paramElement.getName(), paramElement.getValue() );
			}
			getMetadataImplementor().addTypeDefinition(
					new TypeDef(
							typedef.getName(),
							typedef.getClazz(),
							parameters
					)
			);
		}
	}

	public void processTypeDependentMetadata() {
		processFilterDefinitions();
		processIdentifierGenerators();
	}

	private void processFilterDefinitions() {
		if(hibernateMapping.getFilterDef() == null){
			return;
		}
		for ( XMLHibernateMapping.XMLFilterDef filterDefinition : hibernateMapping.getFilterDef() ) {
			final String name = filterDefinition.getName();
			final Map<String,Type> parameters = new HashMap<String, Type>();
			String condition = null;
			for ( Object o : filterDefinition.getContent() ) {
				if ( o instanceof String ) {
					// represents the condition
					if ( condition != null ) {
						// log?
					}
					condition = (String) o;
				}
				else if ( o instanceof XMLHibernateMapping.XMLFilterDef.XMLFilterParam ) {
					final XMLHibernateMapping.XMLFilterDef.XMLFilterParam paramElement = (XMLHibernateMapping.XMLFilterDef.XMLFilterParam) o;
					// todo : should really delay this resolution until later to allow typedef names
					parameters.put(
							paramElement.getName(),
							getMetadataImplementor().getTypeResolver().heuristicType( paramElement.getType() )
					);
				}
				else {
					throw new MappingException( "Unrecognized nested filter content", jaxbRoot.getOrigin() );
				}
			}
			if ( condition == null ) {
				condition = filterDefinition.getCondition();
			}
			getMetadataImplementor().addFilterDefinition( new FilterDefinition( name, condition, parameters ) );
		}
	}

	private void processIdentifierGenerators() {
		if ( hibernateMapping.getIdentifierGenerator() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLIdentifierGenerator identifierGeneratorElement : hibernateMapping.getIdentifierGenerator() ) {
			getMetadataImplementor().registerIdentifierGenerator(
					identifierGeneratorElement.getName(),
					identifierGeneratorElement.getClazz()
			);
		}
	}

	public void processMappingMetadata(List<String> processedEntityNames) {
		if ( hibernateMapping.getClazzOrSubclassOrJoinedSubclass() == null ) {
			return;
		}

		for ( Object entityElementO : hibernateMapping.getClazzOrSubclassOrJoinedSubclass() ) {
			final EntityElement entityElement = (EntityElement) entityElementO;

			// determine the type of root element we have and build appropriate entity descriptor.  Might be:
			//		1) <class/>
			//		2) <subclass/>
			//		3) <joined-subclass/>
			//		4) <union-subclass/>

			final EntityDescriptor entityDescriptor;
			if ( XMLHibernateMapping.XMLClass.class.isInstance( entityElement ) ) {
				entityDescriptor = new RootEntityDescriptorImpl( entityElement, this );
			}
			else if ( XMLSubclassElement.class.isInstance( entityElement ) ) {
				entityDescriptor = new DiscriminatedSubClassEntityDescriptorImpl( entityElement, this );
			}
			else if ( XMLJoinedSubclassElement.class.isInstance( entityElement ) ) {
				entityDescriptor = new JoinedSubClassEntityDescriptorImpl( entityElement, this );
			}
			else if ( XMLUnionSubclassElement.class.isInstance( entityElement ) ) {
				entityDescriptor = new UnionSubClassEntityDescriptorImpl( entityElement, this );
			}
			else {
				throw new MappingException(
						"unknown type of class or subclass: " + entityElement.getClass().getName(),
						jaxbRoot.getOrigin()
				);
			}

			if ( processedEntityNames.contains( entityDescriptor.getEntityName() ) ) {
				continue;
			}

			final EntityBinding entityBinding = entityBinder.createEntityBinding( entityDescriptor );
			getMetadataImplementor().addEntity( entityBinding );
			processedEntityNames.add( entityBinding.getEntity().getName() );
		}
	}

	public void processMappingDependentMetadata() {
		processFetchProfiles();
		processImports();
		processResultSetMappings();
		processNamedQueries();
	}

	private void processFetchProfiles(){
		if ( hibernateMapping.getFetchProfile() == null ) {
			return;
		}
		processFetchProfiles( hibernateMapping.getFetchProfile(), null );
	}

	public void processFetchProfiles(List<XMLFetchProfileElement> fetchProfiles, String containingEntityName) {
		for ( XMLFetchProfileElement fetchProfile : fetchProfiles ) {
			String profileName = fetchProfile.getName();
			Set<FetchProfile.Fetch> fetches = new HashSet<FetchProfile.Fetch>();
			for ( XMLFetchProfileElement.XMLFetch fetch : fetchProfile.getFetch() ) {
				String entityName = fetch.getEntity() == null ? containingEntityName : fetch.getEntity();
				if ( entityName == null ) {
					throw new MappingException(
							"could not determine entity for fetch-profile fetch [" + profileName + "]:[" +
									fetch.getAssociation() + "]",
							jaxbRoot.getOrigin()
					);
				}
				fetches.add( new FetchProfile.Fetch( entityName, fetch.getAssociation(), fetch.getStyle() ) );
			}
			getMetadataImplementor().addFetchProfile( new FetchProfile( profileName, fetches ) );
		}
	}

	private void processImports() {
		if ( hibernateMapping.getImport() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLImport importValue : hibernateMapping.getImport() ) {
			String className = getClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			getMetadataImplementor().addImport( className, rename );
		}
	}

	private void processResultSetMappings() {
		if ( hibernateMapping.getResultset() == null ) {
			return;
		}
//			bindResultSetMappingDefinitions( element, null, mappings );
	}

	private void processNamedQueries() {
		if ( hibernateMapping.getQueryOrSqlQuery() == null ) {
			return;
		}
		for ( Object queryOrSqlQuery : hibernateMapping.getQueryOrSqlQuery() ) {
			if ( XMLQueryElement.class.isInstance( queryOrSqlQuery ) ) {
//					bindNamedQuery( element, null, mappings );
			}
			else if ( XMLSqlQueryElement.class.isInstance( queryOrSqlQuery ) ) {
//				bindNamedSQLQuery( element, null, mappings );
			}
			else {
				throw new MappingException(
						"unknown type of query: " +
								queryOrSqlQuery.getClass().getName(), jaxbRoot.getOrigin()
				);
			}
		}
	}

	private Value<ClassLoaderService> classLoaderService = new Value<ClassLoaderService>(
			new Value.DeferredInitializer<ClassLoaderService>() {
				@Override
				public ClassLoaderService initialize() {
					return getMetadataImplementor().getServiceRegistry().getService( ClassLoaderService.class );
				}
			}
	);

	@Override
	public String getClassName(String unqualifiedName) {
		return HbmHelper.getClassName( unqualifiedName, mappingDefaults.getPackageName() );
	}

	@Override
	public String determineEntityName(EntityElement entityElement) {
		return HbmHelper.determineEntityName( entityElement, mappingDefaults.getPackageName() );
	}
}