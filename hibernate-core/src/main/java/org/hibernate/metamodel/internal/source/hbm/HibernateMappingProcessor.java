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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.jboss.logging.Logger;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.HbmBinder;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbCacheModeAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbDatabaseObjectElement;
import org.hibernate.jaxb.spi.hbm.JaxbDialectScopeElement;
import org.hibernate.jaxb.spi.hbm.JaxbFetchProfileElement;
import org.hibernate.jaxb.spi.hbm.JaxbFilterDefElement;
import org.hibernate.jaxb.spi.hbm.JaxbFlushModeAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbIdentifierGeneratorElement;
import org.hibernate.jaxb.spi.hbm.JaxbImportElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbLoadCollectionElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbQueryParamElement;
import org.hibernate.jaxb.spi.hbm.JaxbResultsetElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnScalarElement;
import org.hibernate.jaxb.spi.hbm.JaxbSqlQueryElement;
import org.hibernate.jaxb.spi.hbm.JaxbSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbSynchronizeElement;
import org.hibernate.jaxb.spi.hbm.JaxbTypedefElement;
import org.hibernate.jaxb.spi.hbm.JaxbUnionSubclassElement;
import org.hibernate.jaxb.spi.hbm.QuerySourceElement;
import org.hibernate.jaxb.spi.hbm.ReturnElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.BasicAuxiliaryDatabaseObjectImpl;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;
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
				catch ( ClassLoadingException e ) {
					throw e;
				}
				catch ( Exception e ) {
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

	private void processFetchProfiles() {
		processFetchProfiles( mappingRoot().getFetchProfile(), null );
		for ( JaxbClassElement classElement : mappingRoot().getClazz() ) {
			processFetchProfiles(
					classElement.getFetchProfile(), mappingDocument.getMappingLocalBindingContext()
					.qualifyClassName( classElement.getName() )
			);

			// processing fetch profiles defined in the <joined-subclass>
			processFetchProfilesInJoinedSubclass(classElement.getJoinedSubclass());
			// <union-subclass>
			processFetchProfilesInUnionSubclass( classElement.getUnionSubclass() );
			// <subclass>
			processFetchProfilesInSubclass( classElement.getSubclass() );
		}
	}

	private void processFetchProfilesInSubclass(List<JaxbSubclassElement> subclass) {
		for ( JaxbSubclassElement subclassElement : subclass ) {
			processFetchProfiles(
					subclassElement.getFetchProfile(), mappingDocument.getMappingLocalBindingContext()
					.qualifyClassName( subclassElement.getName() )
			);
			processFetchProfilesInSubclass( subclassElement.getSubclass() );
		}
	}

	private void processFetchProfilesInUnionSubclass(List<JaxbUnionSubclassElement> unionSubclass) {
		for ( JaxbUnionSubclassElement subclassElement : unionSubclass ) {
			processFetchProfiles(
					subclassElement.getFetchProfile(), mappingDocument.getMappingLocalBindingContext()
					.qualifyClassName( subclassElement.getName() )
			);
			processFetchProfilesInUnionSubclass( subclassElement.getUnionSubclass() );
		}
	}

	private void processFetchProfilesInJoinedSubclass(List<JaxbJoinedSubclassElement> joinedSubclassElements) {
		for ( JaxbJoinedSubclassElement subclassElement : joinedSubclassElements ) {
			processFetchProfiles(
					subclassElement.getFetchProfile(), mappingDocument.getMappingLocalBindingContext()
					.qualifyClassName( subclassElement.getName() )
			);
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
			String className = mappingDocument.getMappingLocalBindingContext()
					.qualifyClassName( importValue.getClazz() );
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
			bindResultSetMappingDefinitions( element );
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

	private ResultSetMappingDefinition buildResultSetMappingDefinition(String name, SQLQueryElementContentParserImpl parser) {
		final ResultSetMappingDefinition definition = new ResultSetMappingDefinition( name );
		int cnt = 0;
		for ( final JaxbReturnScalarElement r : parser.returnScalarElements ) {
			String column = r.getColumn();
			String typeFromXML = r.getType();
			Type type = StringHelper.isNotEmpty( typeFromXML ) ? metadata.getTypeResolver()
					.heuristicType( typeFromXML ) : null;
			definition.addQueryReturn( new NativeSQLQueryScalarReturn( column, type ) );
		}
		for ( final JaxbReturnJoinElement r : parser.returnJoinElements ) {
			definition.addQueryReturn( bindReturnJoin( r, cnt++ ) );

		}
		for ( final JaxbLoadCollectionElement r : parser.loadCollectionElements ) {
			definition.addQueryReturn( bindLoadCollection( r, cnt++ ) );

		}
		for ( final JaxbReturnElement r : parser.returnElements ) {
			definition.addQueryReturn( bindReturn( r, cnt++ ) );

		}
		return definition;
	}

	private void bindResultSetMappingDefinitions(JaxbResultsetElement element) {
		final ResultSetMappingDefinition definition = new ResultSetMappingDefinition( element.getName() );
		int cnt = 0;
		for ( final JaxbReturnScalarElement r : element.getReturnScalar() ) {
			String column = r.getColumn();
			String typeFromXML = r.getType();
			Type type = StringHelper.isNotEmpty( typeFromXML ) ? metadata.getTypeResolver()
					.heuristicType( typeFromXML ) : null;
			definition.addQueryReturn( new NativeSQLQueryScalarReturn( column, type ) );
		}
		for ( final JaxbReturnJoinElement r : element.getReturnJoin() ) {
			definition.addQueryReturn( bindReturnJoin( r, cnt++ ) );

		}
		for ( final JaxbLoadCollectionElement r : element.getLoadCollection() ) {
			definition.addQueryReturn( bindLoadCollection( r, cnt++ ) );

		}
		for ( final JaxbReturnElement r : element.getReturn() ) {
			definition.addQueryReturn( bindReturn( r, cnt++ ) );

		}
		metadata.addResultSetMapping( definition );
	}

	private String getAlias(ReturnElement element, int elementCount) {
		return StringHelper.isEmpty( element.getAlias() ) ? "alias_" + elementCount : element.getAlias();
	}

	private NativeSQLQueryReturn bindReturnJoin(JaxbReturnJoinElement returnJoinElement, int elementCount) {
		final String alias = getAlias( returnJoinElement, elementCount );
		final String roleAttribute = returnJoinElement.getProperty();
		final LockMode lockMode = Helper.interpretLockMode( returnJoinElement.getLockMode(), origin() );
		int dot = roleAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Role attribute for sql query return [alias=" + alias +
							"] not formatted correctly {owningAlias.propertyName}", origin()
			);
		}
		final String roleOwnerAlias = roleAttribute.substring( 0, dot );
		final String roleProperty = roleAttribute.substring( dot + 1 );
		return new NativeSQLQueryJoinReturn(
				alias,
				roleOwnerAlias,
				roleProperty,
				bindPropertyResults( alias, returnJoinElement, null ),
				lockMode
		);
	}

	private NativeSQLQueryReturn bindLoadCollection(JaxbLoadCollectionElement returnElement, int elementCount) {
		final String alias = getAlias( returnElement, elementCount );
		final String collectionAttribute = returnElement.getRole();
		final LockMode lockMode = Helper.interpretLockMode( returnElement.getLockMode(), origin() );
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException(
					"Collection attribute for sql query return [alias=" + alias +
							"] not formatted correctly {OwnerClassName.propertyName}", origin()
			);
		}
		final String ownerClassName = HbmBinder.getClassName(
				collectionAttribute.substring( 0, dot ),
				bindingContext().getMappingDefaults().getPackageName()
		);
		final String ownerPropertyName = collectionAttribute.substring( dot + 1 );
		return new NativeSQLQueryCollectionReturn(
				alias,
				ownerClassName,
				ownerPropertyName,
				bindPropertyResults( alias, returnElement, null ),
				lockMode
		);
	}

	private NativeSQLQueryRootReturn bindReturn(JaxbReturnElement returnElement, int elementCount) {
		final String alias = getAlias( returnElement, elementCount );
		final String clazz = returnElement.getClazz();
		final String entityName = returnElement.getEntityName();
		if ( StringHelper.isEmpty( clazz ) && StringHelper.isEmpty( entityName ) ) {
			throw new org.hibernate.MappingException( "<return alias='" + alias + "'> must specify either a class or entity-name" );
		}
		final LockMode lockMode = Helper.interpretLockMode( returnElement.getLockMode(), origin() );
		EntityBinding entityBinding = null;
		if ( StringHelper.isNotEmpty( entityName ) ) {
			entityBinding = metadata.getEntityBinding( entityName );
		}
		if ( StringHelper.isNotEmpty( clazz ) ) {
			//todo look up entitybinding by class name
		}
		return new NativeSQLQueryRootReturn(
				alias,
				entityName,
				bindPropertyResults( alias, returnElement, entityBinding ),
				lockMode
		);
	}

	private AttributeBinding getRecursiveAttributeBinding(EntityBinding entityBinding, String propertyPath) {
		Iterable<AttributeBinding> attributeBindings = entityBinding.getAttributeBindingClosure();
		StringTokenizer st = new StringTokenizer( propertyPath, "." );
		AttributeBinding attributeBinding = null;
		while ( st.hasMoreElements() ) {
			String element = st.nextToken();
			for ( AttributeBinding binding : attributeBindings ) {

			}
		}
		return attributeBinding;

	}

	private Map bindPropertyResults(String alias, JaxbReturnJoinElement returnJoinElement, EntityBinding entityBinding) {
		throw new NotYetImplementedException();
	}

	// and org.hibernate.cfg.ResultSetMappingBinder.bindPropertyResults()
	private Map<String, String[]> bindPropertyResults(String alias, JaxbReturnElement returnElement, EntityBinding entityBinding) {
		HashMap<String, String[]> propertyresults = new HashMap<String, String[]>();
		JaxbReturnElement.JaxbReturnDiscriminator discriminator = returnElement.getReturnDiscriminator();
		if ( discriminator != null && StringHelper.isNotEmpty( discriminator.getColumn() ) ) {
			String discriminatorColumn = StringHelper.unquote( discriminator.getColumn() );
			propertyresults.put( "class", new String[] { discriminatorColumn } );
		}
		List<JaxbReturnPropertyElement> returnPropertyElements = returnElement.getReturnProperty();

		return propertyresults.isEmpty() ? Collections.EMPTY_MAP : propertyresults;
	}

	private Map<String, String[]> bindPropertyResults(String alias, JaxbLoadCollectionElement element, EntityBinding entityBinding) {
		List<JaxbReturnPropertyElement> returnPropertyElements = element.getReturnProperty();
		List<JaxbReturnPropertyElement> properties = new ArrayList<JaxbReturnPropertyElement>();
		List<String> propertyNames = new ArrayList<String>();
		HashMap propertyresults = new HashMap();
		for ( JaxbReturnPropertyElement propertyElement : returnPropertyElements ) {
			String name = propertyElement.getName();
			if ( entityBinding == null || name.indexOf( '.' ) == -1 ) {
				properties.add( propertyElement );
				propertyNames.add( name );
			}
			else {
				/**
				 * Reorder properties
				 * 1. get the parent property
				 * 2. list all the properties following the expected one in the parent property
				 * 3. calculate the lowest index and insert the property
				 */
				if ( entityBinding == null ) {
					throw new org.hibernate.MappingException(
							"dotted notation in <return-join> or <load_collection> not yet supported"
					);
				}
				int dotIndex = name.lastIndexOf( '.' );
				String reducedName = name.substring( 0, dotIndex );
				AttributeBinding value = getRecursiveAttributeBinding( entityBinding, reducedName );
				Iterable<AttributeBinding> parentPropIter;
				if ( CompositeAttributeBinding.class.isInstance( value ) ) {
					CompositeAttributeBinding comp = (CompositeAttributeBinding) value;
					parentPropIter = comp.attributeBindings();
				}
				else if ( SingularAssociationAttributeBinding.class.isInstance( value ) ) {
					SingularAssociationAttributeBinding toOne = SingularAssociationAttributeBinding.class.cast( value );
					EntityBinding referencedEntityBinding = toOne.getReferencedEntityBinding();
					SingularAttributeBinding referencedAttributeBinding = toOne.getReferencedAttributeBinding();
					try {
						parentPropIter = CompositeAttributeBinding.class.cast( referencedAttributeBinding )
								.attributeBindings();
					}
					catch ( ClassCastException e ) {
						throw new org.hibernate.MappingException(
								"dotted notation reference neither a component nor a many/one to one",
								e
						);
					}
				}
				else {
					throw new org.hibernate.MappingException(
							"dotted notation reference neither a component nor a many/one to one"
					);
				}
				boolean hasFollowers = false;
				List followers = new ArrayList();
				for ( AttributeBinding binding : parentPropIter ) {
					String currentPropertyName = binding.getAttribute().getName();
					String currentName = reducedName + '.' + currentPropertyName;
					if ( hasFollowers ) {
						followers.add( currentName );
					}
					if ( name.equals( currentName ) ) {
						hasFollowers = true;
					}
				}

				int index = propertyNames.size();
				int followersSize = followers.size();
				for ( int loop = 0; loop < followersSize; loop++ ) {
					String follower = (String) followers.get( loop );
					int currentIndex = getIndexOfFirstMatchingProperty( propertyNames, follower );
					index = currentIndex != -1 && currentIndex < index ? currentIndex : index;
				}
				propertyNames.add( index, name );
				properties.add( index, propertyElement );
			}
		}
		Set<String> uniqueReturnProperty = new HashSet<String>();
		for ( JaxbReturnPropertyElement propertyElement : properties ) {
			final String name = propertyElement.getName();
			if ( "class".equals( name ) ) {
				throw new org.hibernate.MappingException(
						"class is not a valid property name to use in a <return-property>, use <return-discriminator> instead"
				);
			}
			//TODO: validate existing of property with the chosen name. (secondpass )
			ArrayList allResultColumns = getResultColumns( propertyElement );

			if ( allResultColumns.isEmpty() ) {
				throw new org.hibernate.MappingException(
						"return-property for alias " + alias +
								" must specify at least one column or return-column name"
				);
			}
			if ( uniqueReturnProperty.contains( name ) ) {
				throw new org.hibernate.MappingException(
						"duplicate return-property for property " + name +
								" on alias " + alias
				);
			}
			String key = name;
			ArrayList intermediateResults = (ArrayList) propertyresults.get( key );
			if ( intermediateResults == null ) {
				propertyresults.put( key, allResultColumns );
			}
			else {
				intermediateResults.addAll( allResultColumns );
			}
		}
		Iterator entries = propertyresults.entrySet().iterator();
		while ( entries.hasNext() ) {
			Map.Entry entry = (Map.Entry) entries.next();
			if ( entry.getValue() instanceof ArrayList ) {
				ArrayList list = (ArrayList) entry.getValue();
				entry.setValue( list.toArray( new String[list.size()] ) );
			}
		}
		return propertyresults.isEmpty() ? Collections.EMPTY_MAP : propertyresults;
	}

	private static ArrayList getResultColumns(JaxbReturnPropertyElement propertyresult) {
		String column = StringHelper.unquote( propertyresult.getColumn() );
		ArrayList allResultColumns = new ArrayList();
		if ( column != null ) {
			allResultColumns.add( column );
		}
		List<JaxbReturnPropertyElement.JaxbReturnColumn> resultColumns = propertyresult.getReturnColumn();
		for ( JaxbReturnPropertyElement.JaxbReturnColumn column1 : resultColumns ) {
			allResultColumns.add( StringHelper.unquote( column1.getName() ) );
		}
		return allResultColumns;
	}

	private static int getIndexOfFirstMatchingProperty(List propertyNames, String follower) {
		int propertySize = propertyNames.size();
		for ( int propIndex = 0; propIndex < propertySize; propIndex++ ) {
			if ( ( (String) propertyNames.get( propIndex ) ).startsWith( follower ) ) {
				return propIndex;
			}
		}
		return -1;
	}

	private void processNamedQueries() {
		for ( final JaxbQueryElement element : mappingRoot().getQuery() ) {
			bindNamedQuery( element );
		}
		for ( final JaxbSqlQueryElement element : mappingRoot().getSqlQuery() ) {
			bindNamedSQLQuery( element );
		}
	}

	private void bindNamedQuery(final JaxbQueryElement queryElement) {
		final NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder();
		parseQueryElement(
				builder, new QuerySourceElement() {
			@Override
			public List<Serializable> getContent() {
				return queryElement.getContent();
			}

			@Override
			public JaxbCacheModeAttribute getCacheMode() {
				return queryElement.getCacheMode();
			}

			@Override
			public String getCacheRegion() {
				return queryElement.getCacheRegion();
			}

			@Override
			public boolean isCacheable() {
				return queryElement.isCacheable();
			}

			@Override
			public String getComment() {
				return queryElement.getComment();
			}

			@Override
			public Integer getFetchSize() {
				return queryElement.getFetchSize();
			}

			@Override
			public JaxbFlushModeAttribute getFlushMode() {
				return queryElement.getFlushMode();
			}

			@Override
			public String getName() {
				return queryElement.getName();
			}

			@Override
			public boolean isReadOnly() {
				return queryElement.isReadOnly();
			}

			@Override
			public Integer getTimeout() {
				return queryElement.getTimeout();
			}
		}, new QueryElementContentsParserImpl()
		);
		metadata.addNamedQuery( builder.createNamedQueryDefinition() );

	}

	private static void parseQueryElement(NamedQueryDefinitionBuilder builder, QuerySourceElement queryElement, QueryElementContentsParser parser) {
		final String queryName = queryElement.getName();
		final boolean cacheable = queryElement.isCacheable();
		final String region = queryElement.getCacheRegion();
		final Integer timeout = queryElement.getTimeout();
		final Integer fetchSize = queryElement.getFetchSize();
		final boolean readonly = queryElement.isReadOnly();
		final String comment = queryElement.getComment();
		final CacheMode cacheMode = queryElement.getCacheMode() == null ? null : CacheMode.valueOf(
				queryElement.getCacheMode()
						.value()
						.toUpperCase()
		);
		final FlushMode flushMode = queryElement.getFlushMode() == null ? null : FlushMode.valueOf(
				queryElement.getFlushMode()
						.value()
						.toUpperCase()
		);

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

	private static interface QueryElementContentsParser {
		void parse(String queryName, List<Serializable> contents, NamedQueryDefinitionBuilder builder);
	}

	private class QueryElementContentsParserImpl implements QueryElementContentsParser {
		@Override
		public void parse(String queryName, List<Serializable> contents, NamedQueryDefinitionBuilder builder) {
			final Map<String, String> queryParam = new HashMap<String, String>();
			String query = "";
			boolean isQueryDefined = false;
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
								"Duplicated query string is defined in Named query[+" + queryName + "]",
								HibernateMappingProcessor.this.origin()
						);
					}
				}
				parseExtra( queryName, obj, builder );
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

	private class SQLQueryElementContentParserImpl extends QueryElementContentsParserImpl {
		List<String> synchronizedTables = new ArrayList<String>();
		List<JaxbLoadCollectionElement> loadCollectionElements = new ArrayList<JaxbLoadCollectionElement>();
		List<JaxbReturnScalarElement> returnScalarElements = new ArrayList<JaxbReturnScalarElement>();
		List<JaxbReturnElement> returnElements = new ArrayList<JaxbReturnElement>();
		List<JaxbReturnJoinElement> returnJoinElements = new ArrayList<JaxbReturnJoinElement>();

		@Override
		protected void parseExtra(String queryName, Serializable obj, NamedQueryDefinitionBuilder builder) {
			NamedSQLQueryDefinitionBuilder sqlBuilder = NamedSQLQueryDefinitionBuilder.class.cast( builder );
			if ( JaxbSynchronizeElement.class.isInstance( obj ) ) {
				JaxbSynchronizeElement element = JaxbSynchronizeElement.class.cast( obj );
				synchronizedTables.add( element.getTable() );
			}
			else if ( JaxbLoadCollectionElement.class.isInstance( obj ) ) {
				loadCollectionElements.add( JaxbLoadCollectionElement.class.cast( obj ) );
			}
			else if ( JaxbReturnScalarElement.class.isInstance( obj ) ) {
				returnScalarElements.add( JaxbReturnScalarElement.class.cast( obj ) );
			}
			else if ( JaxbReturnElement.class.isInstance( obj ) ) {
				returnElements.add( JaxbReturnElement.class.cast( obj ) );
			}
			else if ( JaxbReturnJoinElement.class.isInstance( obj ) ) {
				returnJoinElements.add( JaxbReturnJoinElement.class.cast( obj ) );
			}
		}
	}

	private void bindNamedSQLQuery(final JaxbSqlQueryElement queryElement) {
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder();
		SQLQueryElementContentParserImpl parser = new SQLQueryElementContentParserImpl();
		parseQueryElement(
				builder, new QuerySourceElement() {
			@Override
			public List<Serializable> getContent() {
				return queryElement.getContent();
			}

			@Override
			public JaxbCacheModeAttribute getCacheMode() {
				return queryElement.getCacheMode();
			}

			@Override
			public String getCacheRegion() {
				return queryElement.getCacheRegion();
			}

			@Override
			public boolean isCacheable() {
				return queryElement.isCacheable();
			}

			@Override
			public String getComment() {
				return queryElement.getComment();
			}

			@Override
			public Integer getFetchSize() {
				return queryElement.getFetchSize();
			}

			@Override
			public JaxbFlushModeAttribute getFlushMode() {
				return queryElement.getFlushMode();
			}

			@Override
			public String getName() {
				return queryElement.getName();
			}

			@Override
			public boolean isReadOnly() {
				return queryElement.isReadOnly();
			}

			@Override
			public Integer getTimeout() {
				return queryElement.getTimeout();
			}
		}, parser
		);

		final boolean callable = queryElement.isCallable();
		final String resultSetRef = queryElement.getResultsetRef();
		builder.setCallable( callable ).setResultSetRef( resultSetRef );

		NamedSQLQueryDefinition namedQuery = null;
		if ( StringHelper.isNotEmpty( resultSetRef ) ) {
			namedQuery = builder.createNamedQueryDefinition();
		}
		else {
			ResultSetMappingDefinition definition = buildResultSetMappingDefinition( queryElement.getName(), parser );
			namedQuery = builder.setQueryReturns( definition.getQueryReturns() )
					.setQuerySpaces( parser.synchronizedTables )
					.createNamedQueryDefinition();

		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Named SQL query: %s -> %s", namedQuery.getName(), namedQuery.getQueryString() );
		}
		metadata.addNamedNativeQuery( namedQuery );
	}

}