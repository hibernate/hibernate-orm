/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.complete.CompleteResultBuilderCollectionStandard;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderContainer;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.query.results.ResultSetMapping.resolveResultSetMapping;


/**
 * Responsible for processing the {@link ResultSetMapping} defined by a
 * {@link org.hibernate.query.sql.spi.NativeSelectQueryDefinition} and
 * preprocessing it for consumption by {@link SQLQueryParser}.
 *
 * @author Gavin King
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class ResultSetMappingProcessor implements SQLQueryParser.ParserContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ResultSetMappingProcessor.class );

	private final ResultSetMapping resultSetMapping;

	private final Map<String, NativeQuery.ResultNode> alias2Return = new HashMap<>();
	private final Map<String, String> alias2OwnerAlias = new HashMap<>();

	private final Map<String, EntityPersister> alias2Persister = new HashMap<>();
	private final Map<String, String> alias2Suffix = new HashMap<>();

	private final Map<String, CollectionPersister> alias2CollectionPersister = new HashMap<>();
	private final Map<String, String> alias2CollectionSuffix = new HashMap<>();

	private final Map<String, Map<String, String[]>> entityPropertyResultMaps = new HashMap<>();
	private final Map<String, Map<String, String[]>> collectionPropertyResultMaps = new HashMap<>();

	private final SessionFactoryImplementor factory;

	private int entitySuffixSeed;
	private int collectionSuffixSeed;


	public ResultSetMappingProcessor(ResultSetMapping resultSetMapping, SessionFactoryImplementor factory) {
		this.resultSetMapping = resultSetMapping;
		this.factory = factory;
	}

	private Map<String, String[]> internalGetPropertyResultsMap(String alias) {
		Map<String, String[]> propertyResultMaps = collectionPropertyResultMaps.get( alias );
		if ( propertyResultMaps == null ) {
			propertyResultMaps = entityPropertyResultMaps.get( alias );
		}
		if ( propertyResultMaps != null ) {
			return propertyResultMaps;
		}
		NativeQuery.ResultNode rtn = alias2Return.get( alias );
		if ( rtn instanceof NativeQuery.ReturnProperty && !( rtn instanceof NativeQuery.FetchReturn ) ) {
			return null;
		}
		else {
			// todo (6.0): access property results map somehow which was on NativeSQLQueryNonScalarReturn before
			return Collections.emptyMap();
		}
	}

	public SQLQueryParser.ParserContext process() {
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		resultSetMapping.visitResultBuilders(
				(i, resultBuilder) -> {
					if ( resultBuilder instanceof NativeQuery.RootReturn ) {
						final NativeQuery.RootReturn rootReturn = (NativeQuery.RootReturn) resultBuilder;
						alias2Return.put( rootReturn.getTableAlias(), rootReturn );
						resultBuilder.visitFetchBuilders( this::processFetchBuilder );
					}
					else if ( resultBuilder instanceof NativeQuery.CollectionReturn ) {
						final NativeQuery.CollectionReturn collectionReturn = (NativeQuery.CollectionReturn) resultBuilder;
						alias2Return.put( collectionReturn.getTableAlias(), collectionReturn );
						Map<String, String[]> propertyResultsMap = Collections.emptyMap();//fetchReturn.getPropertyResultsMap()
						addCollection(
								collectionReturn.getNavigablePath().getFullPath(),
								collectionReturn.getTableAlias(),
								propertyResultsMap
						);
					}
				}
		);
		resultSetMapping.visitLegacyFetchBuilders(
				fetchBuilder -> {
					alias2Return.put( fetchBuilder.getTableAlias(), fetchBuilder );
					alias2OwnerAlias.put( fetchBuilder.getTableAlias(), fetchBuilder.getOwnerAlias() );
				}
		);

		// Now, process the returns
		for ( NativeQuery.ResultNode queryReturn : alias2Return.values() ) {
			processReturn( queryReturn );
		}

		return this;
	}

	private void processFetchBuilder(String attributeName, FetchBuilder fetchBuilder) {
		if ( fetchBuilder instanceof DynamicFetchBuilderLegacy ) {
			resultSetMapping.addLegacyFetchBuilder( (DynamicFetchBuilderLegacy) fetchBuilder );
		}
		else if ( fetchBuilder instanceof NativeQuery.FetchReturn ) {
			final NativeQuery.FetchReturn fetchReturn = (NativeQuery.FetchReturn) fetchBuilder;
			alias2Return.put( fetchReturn.getTableAlias(), fetchReturn );
			alias2OwnerAlias.put( fetchReturn.getTableAlias(), fetchReturn.getOwnerAlias() );
		}
		fetchBuilder.visitFetchBuilders( this::processFetchBuilder );
	}

	public ResultSetMapping generateResultMapping(boolean queryHadAliases) {
		if ( !queryHadAliases ) {
			return this.resultSetMapping;
		}

		final ResultSetMapping resultSetMapping = resolveResultSetMapping( null, false, factory );
		final Set<String> visited = new HashSet<>();
		this.resultSetMapping.visitResultBuilders(
				(i, resultBuilder) -> {
					if ( resultBuilder instanceof NativeQuery.RootReturn ) {
						final NativeQuery.RootReturn rootReturn = (NativeQuery.RootReturn) resultBuilder;
						final String suffix = alias2Suffix.get( rootReturn.getTableAlias() );
						visited.add( rootReturn.getTableAlias() );
						if ( suffix == null ) {
							resultSetMapping.addResultBuilder( resultBuilder );
						}
						else {
							final DynamicResultBuilderEntityStandard resultBuilderEntity = createSuffixedResultBuilder(
									rootReturn,
									suffix
							);

							resultSetMapping.addResultBuilder( resultBuilderEntity );
							alias2Return.put( rootReturn.getTableAlias(), resultBuilderEntity );
						}
					}
					else if ( resultBuilder instanceof NativeQuery.CollectionReturn ) {
						final NativeQuery.CollectionReturn collectionReturn = (NativeQuery.CollectionReturn) resultBuilder;
						final String suffix = alias2CollectionSuffix.get( collectionReturn.getTableAlias() );
						if ( suffix == null ) {
							resultSetMapping.addResultBuilder( resultBuilder );
						}
						else {
							final CompleteResultBuilderCollectionStandard resultBuilderCollection = createSuffixedResultBuilder(
									collectionReturn,
									suffix,
									alias2Suffix.get( collectionReturn.getTableAlias() )
							);

							resultSetMapping.addResultBuilder( resultBuilderCollection );
							alias2Return.put( collectionReturn.getTableAlias(), resultBuilderCollection );
						}
					}
					else {
						resultSetMapping.addResultBuilder( resultBuilder );
					}
				}
		);
		this.resultSetMapping.visitLegacyFetchBuilders(
				fetchBuilder -> applyFetchBuilder( resultSetMapping, fetchBuilder, visited )
		);
		return resultSetMapping;
	}

	private void applyFetchBuilder(
			ResultSetMapping resultSetMapping,
			DynamicFetchBuilderLegacy fetchBuilder,
			Set<String> visited) {
		if ( !visited.add( fetchBuilder.getTableAlias() ) ) {
			return;
		}
		final String suffix = alias2Suffix.get( fetchBuilder.getTableAlias() );
		if ( suffix == null ) {
			resultSetMapping.addLegacyFetchBuilder( fetchBuilder );
		}
		else {
			if ( !visited.contains( fetchBuilder.getOwnerAlias() ) ) {
				applyFetchBuilder(
						resultSetMapping,
						// At this point, only legacy fetch builders weren't visited
						(DynamicFetchBuilderLegacy) alias2Return.get( fetchBuilder.getOwnerAlias() ),
						visited
				);
			}
			// At this point, the owner builder must be a DynamicResultBuilderEntityStandard to which we can add this builder to
			final DynamicResultBuilderEntityStandard ownerBuilder = (DynamicResultBuilderEntityStandard) alias2Return.get(
					fetchBuilder.getOwnerAlias()
			);
			final DynamicResultBuilderEntityStandard resultBuilderEntity = createSuffixedResultBuilder(
					alias2Persister.get( fetchBuilder.getTableAlias() ).findContainingEntityMapping(),
					fetchBuilder.getTableAlias(),
					suffix,
					null,
					determineNavigablePath( fetchBuilder )
			);
			final SQLLoadable loadable = (SQLLoadable) alias2Persister.get( fetchBuilder.getOwnerAlias() );
			final List<String> columnNames;
			final String[] columnAliases = loadable.getSubclassPropertyColumnAliases(
					fetchBuilder.getFetchableName(),
					alias2Suffix.get( fetchBuilder.getOwnerAlias() )
			);
			if ( columnAliases.length == 0 ) {
				final CollectionPersister collectionPersister = alias2CollectionPersister.get( fetchBuilder.getTableAlias() );
				if ( collectionPersister == null ) {
					columnNames = Collections.emptyList();
				}
				else {
					final String collectionSuffix = alias2CollectionSuffix.get( fetchBuilder.getTableAlias() );
					final String[] keyColumnAliases = collectionPersister.getKeyColumnAliases( collectionSuffix );
					columnNames = Arrays.asList( keyColumnAliases );
					if ( collectionPersister.hasIndex() ) {
						resultBuilderEntity.addProperty(
								CollectionPart.Nature.INDEX.getName(),
								collectionPersister.getIndexColumnAliases( collectionSuffix )
						);
					}
				}
			}
			else {
				columnNames = Arrays.asList( columnAliases );
			}
			ownerBuilder.addFetchBuilder(
					fetchBuilder.getFetchableName(),
					new DynamicFetchBuilderLegacy(
							fetchBuilder.getTableAlias(),
							fetchBuilder.getOwnerAlias(),
							fetchBuilder.getFetchableName(),
							columnNames,
							Collections.emptyMap(),
							resultBuilderEntity
					)
			);
//			resultSetMapping.addResultBuilder( resultBuilderEntity );
			alias2Return.put( fetchBuilder.getTableAlias(), resultBuilderEntity );
		}
	}

	private NavigablePath determineNavigablePath(DynamicFetchBuilderLegacy fetchBuilder) {
		final NativeQuery.ResultNode ownerResult = alias2Return.get( fetchBuilder.getOwnerAlias() );
		if ( ownerResult instanceof NativeQuery.RootReturn ) {
			return ( (NativeQuery.RootReturn) ownerResult ).getNavigablePath()
					.append( fetchBuilder.getFetchableName() );
		}
		else {
			return determineNavigablePath( ( DynamicFetchBuilderLegacy) ownerResult )
					.append( fetchBuilder.getFetchableName() );
		}
	}

	private DynamicResultBuilderEntityStandard createSuffixedResultBuilder(
			NativeQuery.RootReturn rootReturn,
			String suffix) {
		return createSuffixedResultBuilder(
				rootReturn.getEntityMapping(),
				rootReturn.getTableAlias(),
				suffix,
				rootReturn.getLockMode(),
				new NavigablePath( rootReturn.getEntityMapping().getEntityName(), rootReturn.getTableAlias() )
		);
	}

	private DynamicResultBuilderEntityStandard createSuffixedResultBuilder(
			EntityMappingType entityMapping,
			String tableAlias,
			String suffix,
			LockMode lockMode,
			NavigablePath navigablePath) {
		final SQLLoadable loadable = (SQLLoadable) entityMapping.getEntityPersister();
		final DynamicResultBuilderEntityStandard resultBuilderEntity = new DynamicResultBuilderEntityStandard(
				entityMapping,
				tableAlias,
				navigablePath
		);
		resultBuilderEntity.setLockMode( lockMode );

		final String[] identifierAliases = loadable.getIdentifierAliases( suffix );
		resultBuilderEntity.addIdColumnAliases( identifierAliases );
		resultBuilderEntity.setDiscriminatorAlias( loadable.getDiscriminatorAlias( suffix ) );
		if ( loadable.hasIdentifierProperty() ) {
			resultBuilderEntity.addProperty( loadable.getIdentifierPropertyName(), identifierAliases );
		}

		final String[] propertyNames = loadable.getPropertyNames();
		for ( int i = 0; i < propertyNames.length; i++ ) {
			if ( !loadable.isPropertySelectable( i ) ) {
				continue;
			}
			final String propertyName = propertyNames[i];
			final String[] columnAliases = loadable.getSubclassPropertyColumnAliases( propertyName, suffix );
			final Type propertyType = loadable.getPropertyType( propertyName );
			addFetchBuilder(
					suffix,
					loadable,
					resultBuilderEntity,
					tableAlias,
					identifierAliases,
					propertyName,
					columnAliases,
					propertyType
			);
		}
		return resultBuilderEntity;
	}

	private void addFetchBuilder(
			String suffix,
			SQLLoadable loadable,
			DynamicFetchBuilderContainer resultBuilderEntity,
			String tableAlias,
			String[] identifierAliases,
			String propertyName,
			String[] columnAliases,
			Type propertyType) {
		if ( propertyType instanceof CollectionType ) {
			final CollectionType collectionType = (CollectionType) propertyType;
			final String[] keyColumnAliases;
			if ( collectionType.useLHSPrimaryKey() ) {
				keyColumnAliases = identifierAliases;
			}
			else {
				keyColumnAliases = loadable.getSubclassPropertyColumnAliases(
						collectionType.getLHSPropertyName(),
						suffix
				);
			}
			resultBuilderEntity.addProperty( propertyName, keyColumnAliases );
		}
		else if ( propertyType instanceof ComponentType ) {
			final Map<String, FetchBuilder> fetchBuilderMap = new HashMap<>();
			final DynamicFetchBuilderLegacy fetchBuilder = new DynamicFetchBuilderLegacy(
					"",
					tableAlias,
					propertyName,
					null,
					fetchBuilderMap
			);
			final ComponentType componentType = (ComponentType) propertyType;
			final String[] propertyNames = componentType.getPropertyNames();
			final Type[] propertyTypes = componentType.getSubtypes();
			int aliasIndex = 0;
			for ( int i = 0; i < propertyNames.length; i++ ) {
				final int columnSpan = propertyTypes[i].getColumnSpan( loadable.getFactory() );
				addFetchBuilder(
						suffix,
						loadable,
						fetchBuilder,
						tableAlias,
						identifierAliases,
						propertyNames[i],
						ArrayHelper.slice( columnAliases, aliasIndex, columnSpan ),
						propertyTypes[i]
				);
				aliasIndex += columnSpan;
			}

			resultBuilderEntity.addFetchBuilder( propertyName, fetchBuilder );
		}
		else if ( columnAliases.length != 0 ) {
			if ( propertyType instanceof EntityType ) {
				final ToOneAttributeMapping toOne = (ToOneAttributeMapping) loadable.findAttributeMapping( propertyName );
				if ( !toOne.getIdentifyingColumnsTableExpression().equals( loadable.getMappedTableDetails().getTableName() ) ) {
					// The to-one has a join-table, use the plain join column name instead of the alias
					assert columnAliases.length == 1;
					final String[] targetAliases = new String[1];
					targetAliases[0] = toOne.getTargetKeyPropertyName();
					resultBuilderEntity.addProperty( propertyName, targetAliases );
					return;
				}
			}
			resultBuilderEntity.addProperty( propertyName, columnAliases );
		}
	}

	private CompleteResultBuilderCollectionStandard createSuffixedResultBuilder(
			NativeQuery.CollectionReturn collectionReturn,
			String suffix,
			String entitySuffix) {
		final CollectionPersister collectionPersister = collectionReturn.getPluralAttribute().getCollectionDescriptor();
		final String[] elementColumnAliases;
		if ( collectionPersister.getElementType() instanceof EntityType ) {
			final Loadable elementPersister = (Loadable) ( ( QueryableCollection ) collectionPersister).getElementPersister();
			final String[] propertyNames = elementPersister.getPropertyNames();
			final String[] identifierAliases = elementPersister.getIdentifierAliases( entitySuffix );
			final String discriminatorAlias = elementPersister.getDiscriminatorAlias( entitySuffix );
			final List<String> aliases = new ArrayList<>(
					propertyNames.length + identifierAliases.length + ( discriminatorAlias == null ? 0 : 1 )
			);
			Collections.addAll( aliases, identifierAliases );
			if ( discriminatorAlias != null ) {
				aliases.add( discriminatorAlias );
			}
			for ( int i = 0; i < propertyNames.length; i++ ) {
				Collections.addAll( aliases, elementPersister.getPropertyAliases( entitySuffix, i ) );
			}
			elementColumnAliases = ArrayHelper.toStringArray( aliases );
		}
		else {
			elementColumnAliases = collectionPersister.getElementColumnAliases( suffix );
		}
		return new CompleteResultBuilderCollectionStandard(
				collectionReturn.getTableAlias(),
				collectionReturn.getNavigablePath(),
				collectionReturn.getPluralAttribute(),
				collectionPersister.getKeyColumnAliases( suffix ),
				collectionPersister.hasIndex()
						? collectionPersister.getIndexColumnAliases( suffix )
						: null,
				elementColumnAliases
		);
	}

	private SQLLoadable getSQLLoadable(String entityName) throws MappingException {
		final EntityPersister entityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		if ( !(entityDescriptor instanceof SQLLoadable) ) {
			throw new MappingException( "class persister is not SQLLoadable: " + entityName );
		}
		return (SQLLoadable) entityDescriptor;
	}

	private String generateEntitySuffix() {
		return AliasConstantsHelper.get( entitySuffixSeed++ );
	}

	private String generateCollectionSuffix() {
		return collectionSuffixSeed++ + "__";
	}

	private void processReturn(NativeQuery.ResultNode rtn) {
		if ( rtn instanceof NativeQuery.RootReturn ) {
			processRootReturn( (NativeQuery.RootReturn) rtn );
		}
		else if ( rtn instanceof NativeQuery.FetchReturn ) {
			processFetchReturn( (NativeQuery.FetchReturn) rtn );
		}
		else if ( rtn instanceof NativeQuery.InstantiationResultNode<?> ) {
			processConstructorReturn( (NativeQuery.InstantiationResultNode<?>) rtn );
		}
		else if ( rtn instanceof NativeQuery.ReturnProperty ) {
			processScalarReturn( (NativeQuery.ReturnProperty) rtn );
		}
		else if ( rtn instanceof NativeQuery.ReturnableResultNode ) {
			processPropertyReturn( (NativeQuery.ReturnableResultNode) rtn );
		}
		else {
			throw new IllegalStateException(
					"Unrecognized NativeSQLQueryReturn concrete type encountered : " + rtn
			);
		}
	}

	private void processPropertyReturn(NativeQuery.ReturnableResultNode rtn) {
		//nothing to do
	}

	private void processConstructorReturn(NativeQuery.InstantiationResultNode<?> rtn) {
		//nothing to do
	}

	private void processScalarReturn(NativeQuery.ReturnProperty typeReturn) {
//		scalarColumnAliases.add( typeReturn.getColumnAlias() );
//		scalarTypes.add( typeReturn.getType() );
	}

	private void processRootReturn(NativeQuery.RootReturn rootReturn) {
		if ( alias2Persister.containsKey( rootReturn.getTableAlias() ) ) {
			// already been processed...
			return;
		}

		SQLLoadable persister = (SQLLoadable) rootReturn.getEntityMapping().getEntityPersister();
		Map<String, String[]> propertyResultsMap = Collections.emptyMap();//rootReturn.getPropertyResultsMap()
		addPersister( rootReturn.getTableAlias(), propertyResultsMap, persister );
	}

	private void addPersister(String alias, Map<String, String[]> propertyResult, SQLLoadable persister) {
		alias2Persister.put( alias, persister );
		String suffix = generateEntitySuffix();
		LOG.tracev( "Mapping alias [{0}] to entity-suffix [{1}]", alias, suffix );
		alias2Suffix.put( alias, suffix );
		entityPropertyResultMaps.put( alias, propertyResult );
	}

	private void addCollection(String role, String alias, Map<String, String[]> propertyResults) {

		final SQLLoadableCollection collectionDescriptor = (SQLLoadableCollection) factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor( role );

		alias2CollectionPersister.put( alias, collectionDescriptor );
		String suffix = generateCollectionSuffix();
		LOG.tracev( "Mapping alias [{0}] to collection-suffix [{1}]", alias, suffix );
		alias2CollectionSuffix.put( alias, suffix );
		collectionPropertyResultMaps.put( alias, propertyResults );

		if ( collectionDescriptor.isOneToMany() || collectionDescriptor.isManyToMany() ) {
			SQLLoadable persister = ( SQLLoadable ) collectionDescriptor.getElementPersister();
			addPersister( alias, filter( propertyResults ), persister );
		}
	}

	private Map<String, String[]> filter(Map<String, String[]> propertyResults) {
		final Map<String, String[]> result = new HashMap<>( propertyResults.size() );
		final String keyPrefix = "element.";

		for ( Map.Entry<String, String[]> element : propertyResults.entrySet() ) {
			final String path = element.getKey();
			if ( path.startsWith( keyPrefix ) ) {
				result.put( path.substring( keyPrefix.length() ), element.getValue() );
			}
		}

		return result;
	}

	private void processFetchReturn(NativeQuery.FetchReturn fetchReturn) {
		String alias = fetchReturn.getTableAlias();
		if ( alias2Persister.containsKey( alias ) || alias2CollectionPersister.containsKey( alias ) ) {
			// already been processed...
			return;
		}

		String ownerAlias = fetchReturn.getOwnerAlias();

		// Make sure the owner alias is known...
		if ( !alias2Return.containsKey( ownerAlias ) ) {
			throw new HibernateException( "Owner alias [" + ownerAlias + "] is unknown for alias [" + alias + "]" );
		}

		// If this return's alias has not been processed yet, do so before further processing of this return
		if ( !alias2Persister.containsKey( ownerAlias ) ) {
			processReturn( alias2Return.get( ownerAlias ) );
		}

		SQLLoadable ownerPersister = ( SQLLoadable ) alias2Persister.get( ownerAlias );
		Type returnType = ownerPersister.getPropertyType( fetchReturn.getFetchableName() );

		if ( returnType instanceof CollectionType ) {
			String role = ownerPersister.getEntityName() + '.' + fetchReturn.getFetchableName();
			Map<String, String[]> propertyResultsMap = Collections.emptyMap();//fetchReturn.getPropertyResultsMap()
			addCollection( role, alias, propertyResultsMap );
//			collectionOwnerAliases.add( ownerAlias );
		}
		else if ( returnType instanceof EntityType ) {
			EntityType eType = ( EntityType ) returnType;
			String returnEntityName = eType.getAssociatedEntityName();
			SQLLoadable persister = getSQLLoadable( returnEntityName );
			Map<String, String[]> propertyResultsMap = Collections.emptyMap();//fetchReturn.getPropertyResultsMap()
			addPersister( alias, propertyResultsMap, persister );
		}
	}

	@Override
	public boolean isEntityAlias(String alias) {
		return this.getEntityPersister( alias ) != null;
	}

	@Override
	public boolean isCollectionAlias(String alias) {
		return this.getCollectionPersister( alias ) != null;
	}

	@Override
	public SQLLoadable getEntityPersister(String alias) {
		return (SQLLoadable) alias2Persister.get( alias );
	}

	@Override
	public SQLLoadableCollection getCollectionPersister(String alias) {
		return (SQLLoadableCollection) alias2CollectionPersister.get( alias );
	}

	@Override
	public String getEntitySuffix(String alias) {
		return alias2Suffix.get( alias );
	}

	@Override
	public String getCollectionSuffix(String alias) {
		return alias2CollectionSuffix.get( alias );
	}

	public String getOwnerAlias(String alias) {
		return alias2OwnerAlias.get( alias );
	}

	@Override
	public Map<String, String[]> getPropertyResultsMap(String alias) {
		return internalGetPropertyResultsMap( alias );
	}

//	public String[] collectQuerySpaces() {
//		final HashSet<String> spaces = new HashSet<>();
//		collectQuerySpaces( spaces );
//		return spaces.toArray( EMPTY_STRING_ARRAY );
//	}
//
//	public void collectQuerySpaces(Collection<String> spaces) {
//		for ( EntityPersister persister : alias2Persister.values() ) {
//			Collections.addAll( spaces, (String[]) persister.getQuerySpaces() );
//		}
//		for ( CollectionPersister persister : alias2CollectionPersister.values() ) {
//			final Type elementType = persister.getElementType();
//			if ( elementType instanceof EntityType && ! elementType instanceof AnyType ) {
//				final Joinable joinable = ( (EntityType) elementType ).getAssociatedJoinable( factory );
//				Collections.addAll( spaces, (String[]) ( (EntityPersister) joinable ).getQuerySpaces() );
//			}
//		}
//	}
}
