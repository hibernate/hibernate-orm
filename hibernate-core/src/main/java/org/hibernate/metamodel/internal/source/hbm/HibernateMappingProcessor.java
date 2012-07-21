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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.cfg.HbmBinder;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbClassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbDatabaseObjectElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFetchProfileElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFilterDefElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbIdentifierGeneratorElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbImportElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbJoinedSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbLoadCollectionElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbQueryElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbQueryParamElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbResultsetElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbReturnElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbReturnJoinElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbReturnScalarElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSqlQueryElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSynchronizeElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbTypedefElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbUnionSubclassElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.type.Type;

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

	private ValueHolder<ClassLoaderService> classLoaderService = new ValueHolder<ClassLoaderService>(
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

		for ( JaxbDatabaseObjectElement databaseObjectElement : mappingRoot().getDatabaseObject() ) {
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
					for ( JaxbDatabaseObjectElement.JaxbDialectScope dialectScope : databaseObjectElement.getDialectScope() ) {
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
				fetches.add( new FetchProfile.Fetch( entityName, fetch.getAssociation(), fetch.getStyle().value() ) );
			}
			metadata.addFetchProfile( new FetchProfile( profileName, fetches ) );
		}
	}

	private void processImports() {
		JaxbHibernateMapping root = mappingRoot();
		for ( JaxbImportElement importValue : root.getImport() ) {
			String className = mappingDocument.getMappingLocalBindingContext().qualifyClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			metadata.addImport( className, rename );
		}
		if ( root.isAutoImport() ) {
			for(final JaxbClassElement element : root.getClazz()){
				processEntityElement( element );
			}
			for(final JaxbJoinedSubclassElement element : root.getJoinedSubclass()){
				processEntityElement( element );
			}
			for(final JaxbUnionSubclassElement element : root.getUnionSubclass()){
				processEntityElement( element );
			}
			for(final JaxbSubclassElement element : root.getSubclass()){
				processEntityElement( element );
			}
		}
	}

	private void processEntityElement(EntityElement element) {
		EntityElement entityElement = element;
		String qualifiedName = bindingContext().determineEntityName( entityElement );
		metadata.addImport( entityElement.getEntityName() == null
							? entityElement.getName()
							: entityElement.getEntityName(), qualifiedName );
	}

	private void processResultSetMappings() {
		List<JaxbResultsetElement> resultsetElements = new ArrayList<JaxbResultsetElement>();
		if ( CollectionHelper.isNotEmpty( mappingRoot().getResultset() ) ) {
			resultsetElements.addAll( mappingRoot().getResultset() );
		}
		for(final JaxbClassElement element : mappingRoot().getClazz()){
			if ( CollectionHelper.isNotEmpty( element.getResultset() ) ) {
				resultsetElements.addAll( element.getResultset() );
			}
		}
		for(final JaxbJoinedSubclassElement element : mappingRoot().getJoinedSubclass()){
			if ( CollectionHelper.isNotEmpty( element.getResultset() ) ) {
				resultsetElements.addAll( element.getResultset() );
			}
		}
		for(final JaxbUnionSubclassElement element : mappingRoot().getUnionSubclass()){
			if ( CollectionHelper.isNotEmpty( element.getResultset() ) ) {
				resultsetElements.addAll( element.getResultset() );
			}
		}
		for(final JaxbSubclassElement element : mappingRoot().getSubclass()){
			if ( CollectionHelper.isNotEmpty( element.getResultset() ) ) {
				resultsetElements.addAll( element.getResultset() );
			}
		}
		if ( resultsetElements.isEmpty() ) {
			return;
		}
		for(final JaxbResultsetElement element : resultsetElements){
			bindResultSetMappingDefinitions( element );
		}

	}

	private void bindResultSetMappingDefinitions(JaxbResultsetElement element) {
		final ResultSetMappingDefinition definition = new ResultSetMappingDefinition( element.getName() );
		int cnt=0;
		NativeSQLQueryReturn nativeSQLQueryReturn;
		for(final JaxbReturnScalarElement r : element.getReturnScalar()){
			cnt++;
			String column = r.getColumn();
			String typeFromXML = r.getType();
			Type type = metadata.getTypeResolver().heuristicType( typeFromXML );
			nativeSQLQueryReturn = new NativeSQLQueryScalarReturn( column, type );
			definition.addQueryReturn( nativeSQLQueryReturn );
		}
		for(final JaxbReturnJoinElement r : element.getReturnJoin()){
			cnt++;
			nativeSQLQueryReturn = bindReturnJoin(r, cnt);
			definition.addQueryReturn( nativeSQLQueryReturn );

		}
		for(final JaxbLoadCollectionElement r : element.getLoadCollection()){
			cnt++;
			nativeSQLQueryReturn = bindLoadCollection( r, cnt );
			definition.addQueryReturn( nativeSQLQueryReturn );

		}
		for(final JaxbReturnElement r : element.getReturn()){
			cnt++;
			nativeSQLQueryReturn = bindReturn( r, cnt );
			definition.addQueryReturn( nativeSQLQueryReturn );

		}
		metadata.addResultSetMapping( definition );

	}

	private NativeSQLQueryReturn bindLoadCollection(JaxbLoadCollectionElement returnElement, int cnt) {
		final String alias = returnElement.getAlias();
		final String collectionAttribute = returnElement.getRole();
		final LockMode lockMode = Helper.interpretLockMode( returnElement.getLockMode(), origin() );
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Collection attribute for sql query return [alias=" + alias +
							"] not formatted correctly {OwnerClassName.propertyName}", origin()
			);
		}
		final String ownerClassName = HbmBinder.getClassName( collectionAttribute.substring( 0, dot ), bindingContext().getMappingDefaults().getPackageName() );
		final String ownerPropertyName = collectionAttribute.substring( dot + 1 );

//		//FIXME: get the PersistentClass
//		java.util.Map propertyResults = bindPropertyResults(alias, returnElem, null, mappings );
//
//		return new NativeSQLQueryCollectionReturn(
//				alias,
//				ownerClassName,
//				ownerPropertyName,
//				propertyResults,
//				lockMode
//		);
		return null;
	}

	private NativeSQLQueryReturn bindReturnJoin(JaxbReturnJoinElement returnJoinElement, int cnt) {
		final String alias = returnJoinElement.getAlias();
		final String roleAttribute = returnJoinElement.getProperty();
		final LockMode lockMode  = Helper.interpretLockMode( returnJoinElement.getLockMode(), origin() );
		int dot = roleAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Role attribute for sql query return [alias=" + alias +
							"] not formatted correctly {owningAlias.propertyName}", origin()
			);
		}
		String roleOwnerAlias = roleAttribute.substring( 0, dot );
		String roleProperty = roleAttribute.substring( dot + 1 );

		//FIXME: get the PersistentClass
//		java.util.Map propertyResults = bindPropertyResults( alias, returnJoinElement, null );

//		return new NativeSQLQueryJoinReturn(
//				alias,
//				roleOwnerAlias,
//				roleProperty,
//				propertyResults, // TODO: bindpropertyresults(alias, returnElem)
//				lockMode
//		);
		return null;
	}

	private NativeSQLQueryRootReturn bindReturn(JaxbReturnElement returnElement,int elementCount) {
		String alias = returnElement.getAlias();
		if( StringHelper.isEmpty( alias )) {
			alias = "alias_" + elementCount; // hack/workaround as sqlquery impl depend on having a key.
		}
		String clazz = returnElement.getClazz();
		String entityName = returnElement.getEntityName();
		if(StringHelper.isEmpty( clazz ) && StringHelper.isEmpty( entityName )) {
			throw new org.hibernate.MappingException( "<return alias='" + alias + "'> must specify either a class or entity-name");
		}
		LockMode lockMode = Helper.interpretLockMode( returnElement.getLockMode(), origin() );


		EntityBinding entityBinding = null;

		if ( StringHelper.isNotEmpty( entityName ) ) {
			entityBinding = metadata.getEntityBinding( entityName );
		}
		if ( StringHelper.isNotEmpty( clazz ) ) {
			//todo look up entitybinding by class name
		}
		java.util.Map<String, String[]> propertyResults = bindPropertyResults( alias, returnElement, entityBinding );

		return new NativeSQLQueryRootReturn(
				alias,
				entityName,
				propertyResults,
				lockMode
		);


	}
	//TODO impl this, see org.hibernate.metamodel.internal.source.annotations.global.SqlResultSetProcessor.bindEntityResult()
	// and org.hibernate.cfg.ResultSetMappingBinder.bindPropertyResults()
	private Map<String, String[]> bindPropertyResults(String alias, JaxbReturnElement returnElement, EntityBinding entityBinding) {
		return null;
	}

	private void processNamedQueries() {
		if ( CollectionHelper.isEmpty( mappingRoot().getQueryOrSqlQuery() ) ) {
			return;
		}

		for ( Object queryOrSqlQuery : mappingRoot().getQueryOrSqlQuery() ) {
			if ( JaxbQueryElement.class.isInstance( queryOrSqlQuery ) ) {
					bindNamedQuery( JaxbQueryElement.class.cast( queryOrSqlQuery ) );
			}
			else if ( JaxbSqlQueryElement.class.isInstance( queryOrSqlQuery ) ) {
				bindNamedSQLQuery( JaxbSqlQueryElement.class.cast( queryOrSqlQuery ) );
			}
			else {
				throw new MappingException(
						"unknown type of query: " +
								queryOrSqlQuery.getClass().getName(), origin()
				);
			}
		}
	}

	private void bindNamedQuery(JaxbQueryElement queryElement) {
		final String queryName = queryElement.getName();
		//path??
		List<Serializable> list = queryElement.getContent();
		final Map<String, String> queryParam;
		String query = "";
		if ( CollectionHelper.isNotEmpty( list ) ) {
			queryParam = new HashMap<String, String>( list.size() );
			for ( Serializable obj : list ) {
				if ( JaxbQueryParamElement.class.isInstance( obj ) ) {
					JaxbQueryParamElement element = JaxbQueryParamElement.class.cast( obj );
					queryParam.put( element.getName(), element.getType() );
				}else if(String.class.isInstance( obj )){
					query = obj.toString();
				}
			}
		}
		else {
			queryParam = Collections.emptyMap();
		}
		if ( StringHelper.isEmpty( query ) ) {
			throw new MappingException( "Named query[" + queryName + "] has no hql defined", origin() );
		}
		final boolean cacheable = queryElement.isCacheable();
		final String region = queryElement.getCacheRegion();
		final Integer timeout = queryElement.getTimeout();
		final Integer fetchSize = queryElement.getFetchSize();
		final boolean readonly = queryElement.isReadOnly() == null ? false : queryElement.isReadOnly();
		final CacheMode cacheMode = queryElement.getCacheMode() == null ? null : CacheMode.valueOf(
				queryElement.getCacheMode()
						.value()
						.toUpperCase()
		);
		final String comment = queryElement.getComment();
		final FlushMode flushMode = queryElement.getFlushMode() == null ? null : FlushMode.valueOf(
				queryElement.getFlushMode()
						.value()
						.toUpperCase()
		);
		NamedQueryDefinition namedQuery = new NamedQueryDefinition(
				queryName,
				query,
				cacheable,
				region,
				timeout,
				fetchSize,
				flushMode,
				cacheMode,
				readonly,
				comment,
				queryParam
		);
		metadata.addNamedQuery( namedQuery );

	}

	private void bindNamedSQLQuery(JaxbSqlQueryElement queryElement) {
		final String queryName = queryElement.getName();
		//todo patch
		final boolean cacheable = queryElement.isCacheable();
		String query = "";
		final String region = queryElement.getCacheRegion();
		final Integer timeout = queryElement.getTimeout();
		final Integer fetchSize = queryElement.getFetchSize();
		final boolean readonly = queryElement.isReadOnly() == null ? false : queryElement.isReadOnly();
		final CacheMode cacheMode = queryElement.getCacheMode()==null ? null : CacheMode.valueOf( queryElement.getCacheMode().value().toUpperCase() );
		final FlushMode flushMode = queryElement.getFlushMode() ==null ? null : FlushMode.valueOf( queryElement.getFlushMode().value().toUpperCase() );
		final String comment = queryElement.getComment();
		final boolean callable = queryElement.isCallable();
		final String resultSetRef = queryElement.getResultsetRef();
		final java.util.List<String> synchronizedTables = new ArrayList<String>();
		final Map<String, String> queryParam = new HashMap<String, String>( );
		List<Serializable> list = queryElement.getContent();
		for ( Serializable obj : list ) {
			if ( JaxbSynchronizeElement.class.isInstance( obj ) ) {
				 JaxbSynchronizeElement element = JaxbSynchronizeElement.class.cast( obj );
				synchronizedTables.add( element.getTable() );
			}
			else if ( JaxbQueryParamElement.class.isInstance( obj ) ) {
				JaxbQueryParamElement element = JaxbQueryParamElement.class.cast( obj );
				queryParam.put( element.getName(), element.getType() );
			}
			else if ( JaxbLoadCollectionElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnScalarElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnJoinElement.class.isInstance( obj ) ) {

			}
			else if ( String.class.isInstance( obj ) ){
				query = obj.toString();
			}

		}
		if ( StringHelper.isEmpty( query ) ) {
			throw new MappingException( "Named sql query[" + queryName + "] has no sql defined", origin() );
		}
		NamedSQLQueryDefinition namedQuery=null;
		if(StringHelper.isNotEmpty( resultSetRef )){
			namedQuery = new NamedSQLQueryDefinition(
					queryName,
					query,
					resultSetRef,
					synchronizedTables,
					cacheable,
					region,
					timeout,
					fetchSize,
					flushMode,
					cacheMode,
					readonly,
					comment,
					queryParam,
					callable
			);
			//TODO check there is no actual definition elemnents when a ref is defined
		}   else {
//			ResultSetMappingDefinition definition = buildResultSetMappingDefinition( queryElem, path, mappings );
//			namedQuery = new NamedSQLQueryDefinition(
//					queryName,
//					query,
//					definition.getQueryReturns(),
//					synchronizedTables,
//					cacheable,
//					region,
//					timeout,
//					fetchSize,
//					flushMode,
//					cacheMode,
//					readonly,
//					comment,
//					queryParam,
//					callable
//			);

		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Named SQL query: %s -> %s", namedQuery.getName(), namedQuery.getQueryString() );
		}
		metadata.addNamedNativeQuery( namedQuery );

	}
}