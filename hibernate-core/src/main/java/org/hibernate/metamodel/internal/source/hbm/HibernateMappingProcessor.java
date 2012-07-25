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
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbCacheModeAttribute;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbClassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbDatabaseObjectElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbDialectScopeElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFetchProfileElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFilterDefElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbFlushModeAttribute;
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
			processEntityElementsImport( root.getClazz() );
			processEntityElementsImport( root.getJoinedSubclass() );
			processEntityElementsImport( root.getUnionSubclass() );
			processEntityElementsImport( root.getSubclass() );
		}
	}

	private void processEntityElementsImport(List<? extends EntityElement> entityElements){
		for(final EntityElement element : entityElements){
			processEntityElementImport( element );
		}
	}

	private void processEntityElementImport(EntityElement entityElement) {
		final String qualifiedName = bindingContext().determineEntityName( entityElement );
		final String importName = entityElement.getEntityName() == null
				? entityElement.getName()
				: entityElement.getEntityName();
		metadata.addImport( importName, qualifiedName );

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
		for(final JaxbResultsetElement element : resultsetElements){
			bindResultSetMappingDefinitions( element );
		}

	}

	private static void findResultSets(List<JaxbResultsetElement> resultsetElements, List<? extends EntityElement> entityElements) {
		for(final EntityElement element : entityElements){
			addAllIfNotEmpty( resultsetElements, element.getResultset() );
		}
	}

	private static void addAllIfNotEmpty(List target, List values) {
		if ( CollectionHelper.isNotEmpty( values ) ) {
			target.addAll(values );
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
		for(final JaxbQueryElement element : mappingRoot().getQuery()){
			bindNamedQuery( element );
		}
		for(final JaxbSqlQueryElement element : mappingRoot().getSqlQuery()){
			bindNamedSQLQuery( element );
		}
	}

	private void bindNamedQuery(JaxbQueryElement queryElement) {
		final NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder(  );
		parseQueryElement( builder, queryElement, new QueryElementContentsParserImpl() );
		metadata.addNamedQuery( builder.createNamedQueryDefinition() );

	}
	private static void parseQueryElement(NamedQueryDefinitionBuilder builder, JaxbQueryElement queryElement, QueryElementContentsParser parser) {
		final String queryName = queryElement.getName();
		final boolean cacheable = queryElement.isCacheable();
		final String region = queryElement.getCacheRegion();
		final Integer timeout = queryElement.getTimeout();
		final Integer fetchSize = queryElement.getFetchSize();
		final boolean readonly = queryElement.isReadOnly();
		final String comment = queryElement.getComment();
		final CacheMode cacheMode = queryElement.getCacheMode() == null ? null : CacheMode.valueOf( queryElement.getCacheMode().value().toUpperCase() );
		final FlushMode flushMode = queryElement.getFlushMode() == null ? null : FlushMode.valueOf( queryElement.getFlushMode().value().toUpperCase() );

		builder.setName( queryName )
				.setCacheable( cacheable )
				.setCacheRegion( region )
				.setTimeout( timeout )
				.setFetchSize( fetchSize )
				.setFlushMode( flushMode )
				.setCacheMode( cacheMode )
				.setReadOnly( readonly )
				.setComment( comment );

		final List<Serializable> list = queryElement.getContent();
		parser.parse( queryName, list, builder );
	}
	private static interface QueryElementContentsParser{
		void parse(String queryName, List<Serializable> contents, NamedQueryDefinitionBuilder builder);
	}

	private  class QueryElementContentsParserImpl implements QueryElementContentsParser {
		@Override
		public void parse(String queryName, List<Serializable> contents, NamedQueryDefinitionBuilder builder) {
			final Map<String, String> queryParam = new HashMap<String, String>();
			String query = "";
			boolean isQueryDefined=false;
			for ( Serializable obj : contents ) {
				if ( JaxbQueryParamElement.class.isInstance( obj ) ) {
					JaxbQueryParamElement element = JaxbQueryParamElement.class.cast( obj );
					queryParam.put( element.getName(), element.getType() );
				}
				else if ( String.class.isInstance( obj ) ) {
					if ( !isQueryDefined ) {
						query = obj.toString();
					}
					else {
						throw new MappingException(
								"Duplicated query string is defined in Named query[+"+queryName+"]",
								HibernateMappingProcessor.this.origin()
						);
					}
				}
				parseExtra(queryName, obj, builder);
			}
			builder.setParameterTypes( queryParam );
			if ( StringHelper.isEmpty( query ) ) {
				throw new MappingException(
						"Named query[" + queryName + "] has no query string defined",
						HibernateMappingProcessor.this.origin()
				);
			}
			builder.setQuery( query );
		}

		protected void parseExtra(String queryName, Serializable obj, NamedQueryDefinitionBuilder builder) {
			//do nothing here
		}
	}

	private class SQLQueryElementContentParserImpl extends QueryElementContentsParserImpl{
		@Override
		protected void parseExtra(String queryName, Serializable obj, NamedQueryDefinitionBuilder builder) {
			if ( JaxbSynchronizeElement.class.isInstance( obj ) ) {
				JaxbSynchronizeElement element = JaxbSynchronizeElement.class.cast( obj );
//				synchronizedTables.add( element.getTable() );
			}
			else if ( JaxbLoadCollectionElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnScalarElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnElement.class.isInstance( obj ) ) {

			}
			else if ( JaxbReturnJoinElement.class.isInstance( obj ) ) {

			}
		}
	}

	private void bindNamedSQLQuery(JaxbSqlQueryElement queryElement) {
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder(  );
		parseQueryElement( builder, queryElement, new SQLQueryElementContentParserImpl() );

		final boolean callable = queryElement.isCallable();
		final String resultSetRef = queryElement.getResultsetRef();
		builder.setCallable( callable ).setResultSetRef( resultSetRef );

		NamedSQLQueryDefinition namedQuery=null;
		if(StringHelper.isNotEmpty( resultSetRef )){
			namedQuery = builder.createNamedQueryDefinition();
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