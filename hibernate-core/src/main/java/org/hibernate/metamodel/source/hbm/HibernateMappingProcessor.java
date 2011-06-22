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
package org.hibernate.metamodel.source.hbm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.Origin;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLFetchProfileElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLParamElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLQueryElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlQueryElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclassElement;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.internal.OverriddenMappingDefaults;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.type.Type;

/**
 * Responsible for processing a {@code <hibernate-mapping/>} element.  Allows processing to be coordinated across
 * all hbm files in an ordered fashion.  The order is essentially the same as defined in
 * {@link org.hibernate.metamodel.source.spi.Binder}
 *
 * @author Steve Ebersole
 */
public class HibernateMappingProcessor implements HbmBindingContext {
	private final MetadataImplementor metadata;
	private final JaxbRoot<XMLHibernateMapping> jaxbRoot;

	private final XMLHibernateMapping hibernateMapping;

	private final MappingDefaults mappingDefaults;
	private final MetaAttributeContext metaAttributeContext;

	private final boolean autoImport;

	public HibernateMappingProcessor(MetadataImplementor metadata, JaxbRoot<XMLHibernateMapping> jaxbRoot) {
		this.metadata = metadata;
		this.jaxbRoot = jaxbRoot;

		this.hibernateMapping = jaxbRoot.getRoot();
		this.mappingDefaults = new OverriddenMappingDefaults(
				metadata.getMappingDefaults(),
				hibernateMapping.getPackage(),
				hibernateMapping.getSchema(),
				hibernateMapping.getCatalog(),
				null,
				null,
				hibernateMapping.getDefaultCascade(),
				hibernateMapping.getDefaultAccess(),
				hibernateMapping.isDefaultLazy()
		);

		autoImport = hibernateMapping.isAutoImport();

		metaAttributeContext = extractMetaAttributes();
	}

	private MetaAttributeContext extractMetaAttributes() {
		return hibernateMapping.getMeta() == null
				? new MetaAttributeContext( metadata.getMetaAttributeContext() )
				: HbmHelper.extractMetaAttributeContext( hibernateMapping.getMeta(), true, metadata.getMetaAttributeContext() );
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
		return metadata.getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return metadata.getOptions().getNamingStrategy();
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
		return metadata;
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return metadata.locateClassByName( name );
	}

	@Override
	public JavaType makeJavaType(String className) {
		return metadata.makeJavaType( className );
	}

	public void bindIndependentMetadata() {
		bindDatabaseObjectDefinitions();
		bindTypeDefinitions();
	}

	private void bindDatabaseObjectDefinitions() {
		if ( hibernateMapping.getDatabaseObject() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLDatabaseObject databaseObjectElement : hibernateMapping.getDatabaseObject() ) {
			final AuxiliaryDatabaseObject auxiliaryDatabaseObject;
			if ( databaseObjectElement.getDefinition() != null ) {
				final String className = databaseObjectElement.getDefinition().getClazz();
				try {
					auxiliaryDatabaseObject = (AuxiliaryDatabaseObject) classLoaderService().classForName( className ).newInstance();
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
			metadata.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
		}
	}

	private void bindTypeDefinitions() {
		if ( hibernateMapping.getTypedef() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLTypedef typedef : hibernateMapping.getTypedef() ) {
			final Map<String, String> parameters = new HashMap<String, String>();
			for ( XMLParamElement paramElement : typedef.getParam() ) {
				parameters.put( paramElement.getName(), paramElement.getValue() );
			}
			metadata.addTypeDefinition( new TypeDef( typedef.getName(), typedef.getClazz(), parameters ) );
		}
	}

	public void bindTypeDependentMetadata() {
		bindFilterDefinitions();
		bindIdentifierGenerators();
	}

	private void bindFilterDefinitions() {
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
							metadata.getTypeResolver().heuristicType( paramElement.getType() )
					);
				}
				else {
					throw new MappingException( "Unrecognized nested filter content", jaxbRoot.getOrigin() );
				}
			}
			if ( condition == null ) {
				condition = filterDefinition.getCondition();
			}
			metadata.addFilterDefinition( new FilterDefinition( name, condition, parameters ) );
		}
	}

	private void bindIdentifierGenerators() {
		if ( hibernateMapping.getIdentifierGenerator() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLIdentifierGenerator identifierGeneratorElement : hibernateMapping.getIdentifierGenerator() ) {
			metadata.registerIdentifierGenerator(
					identifierGeneratorElement.getName(),
					identifierGeneratorElement.getClazz()
			);
		}
	}

	public void bindMappingMetadata(List<String> processedEntityNames) {
		if ( hibernateMapping.getClazzOrSubclassOrJoinedSubclass() == null ) {
			return;
		}
		for ( Object clazzOrSubclass : hibernateMapping.getClazzOrSubclassOrJoinedSubclass() ) {
			if ( XMLHibernateMapping.XMLClass.class.isInstance( clazzOrSubclass ) ) {
				XMLHibernateMapping.XMLClass clazz =
						XMLHibernateMapping.XMLClass.class.cast( clazzOrSubclass );
				new RootEntityBinder( this, clazz ).process( clazz );
			}
			else if ( XMLSubclassElement.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleSubclass( superModel, mappings, element, inheritedMetas );
			}
			else if ( XMLJoinedSubclassElement.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleJoinedSubclass( superModel, mappings, element, inheritedMetas );
			}
			else if ( XMLUnionSubclassElement.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleUnionSubclass( superModel, mappings, element, inheritedMetas );
			}
			else {
				throw new org.hibernate.metamodel.source.MappingException(
						"unknown type of class or subclass: " +
								clazzOrSubclass.getClass().getName(), jaxbRoot.getOrigin()
				);
			}
		}
	}

	public void bindMappingDependentMetadata() {
		bindFetchProfiles();
		bindImports();
		bindResultSetMappings();
		bindNamedQueries();
	}

	private void bindFetchProfiles(){
		if(hibernateMapping.getFetchProfile() == null){
			return;
		}
		bindFetchProfiles( hibernateMapping.getFetchProfile(),null );
	}

	public void bindFetchProfiles(List<XMLFetchProfileElement> fetchProfiles, String containingEntityName) {
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
			metadata.addFetchProfile( new FetchProfile( profileName, fetches ) );
		}
	}

	private void bindImports() {
		if ( hibernateMapping.getImport() == null ) {
			return;
		}
		for ( XMLHibernateMapping.XMLImport importValue : hibernateMapping.getImport() ) {
			String className = getClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			metadata.addImport( className, rename );
		}
	}

	private void bindResultSetMappings() {
		if ( hibernateMapping.getResultset() == null ) {
			return;
		}
//			bindResultSetMappingDefinitions( element, null, mappings );
	}

	private void bindNamedQueries() {
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

	private ClassLoaderService classLoaderService;

	private ClassLoaderService classLoaderService() {
		if ( classLoaderService == null ) {
			classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		}
		return classLoaderService;
	}

	@Override
	public String extractEntityName(XMLHibernateMapping.XMLClass entityClazz) {
		return HbmHelper.extractEntityName( entityClazz, mappingDefaults.getPackageName() );
	}

	@Override
	public String getClassName(String unqualifiedName) {
		return HbmHelper.getClassName( unqualifiedName, mappingDefaults.getPackageName() );
	}
}