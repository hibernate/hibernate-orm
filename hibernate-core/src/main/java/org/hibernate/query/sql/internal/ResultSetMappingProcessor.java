/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sql.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.SQLLoadableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ResultSetMappingImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Responsible for processing the {@link ResultSetMapping}
 * defined by a {@link org.hibernate.query.sql.spi.NativeSelectQueryDefinition} and
 * pre-process it for consumption in {@link SQLQueryParser}.
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

	private boolean hasPropertyResultMap(String alias) {
		Map<String, String[]> propertyMaps = internalGetPropertyResultsMap( alias );
		return propertyMaps != null && ! propertyMaps.isEmpty();
	}

	public SQLQueryParser.ParserContext process() {
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		resultSetMapping.visitResultBuilders(
				(i, resultBuilder) -> {
					if ( resultBuilder instanceof NativeQuery.RootReturn ) {
						final NativeQuery.RootReturn rootReturn = (NativeQuery.RootReturn) resultBuilder;
						alias2Return.put( rootReturn.getTableAlias(), rootReturn );
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

	public ResultSetMapping generateResultMapping(boolean queryHadAliases) {
		if ( !queryHadAliases ) {
			return this.resultSetMapping;
		}
		final ResultSetMappingImpl resultSetMapping = new ResultSetMappingImpl( null );
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
			ResultSetMappingImpl resultSetMapping,
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
					determineNavigablePath( fetchBuilder )
			);
			final SQLLoadable loadable = (SQLLoadable) alias2Persister.get( fetchBuilder.getOwnerAlias() );
			final List<String> columnNames;
			final String[] columnAliases = loadable.getSubclassPropertyColumnAliases(
					fetchBuilder.getFetchableName(),
					suffix
			);
			if ( columnAliases.length == 0 ) {
				final CollectionPersister collectionPersister = alias2CollectionPersister.get( fetchBuilder.getTableAlias() );
				if ( collectionPersister == null ) {
					columnNames = Collections.emptyList();
				}
				else {
					columnNames = Arrays.asList( collectionPersister.getKeyColumnAliases( suffix ) );
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
				new NavigablePath( rootReturn.getEntityMapping().getEntityName() )
		);
	}

	private DynamicResultBuilderEntityStandard createSuffixedResultBuilder(
			EntityMappingType entityMapping,
			String tableAlias,
			String suffix,
			NavigablePath navigablePath) {
		final SQLLoadable loadable = (SQLLoadable) entityMapping.getEntityPersister();
		final DynamicResultBuilderEntityStandard resultBuilderEntity = new DynamicResultBuilderEntityStandard(
				entityMapping,
				tableAlias,
				navigablePath
		);

		resultBuilderEntity.addIdColumnAliases( loadable.getIdentifierAliases( suffix ) );
		resultBuilderEntity.setDiscriminatorAlias( loadable.getDiscriminatorAlias( suffix ) );

		for ( String propertyName : loadable.getPropertyNames() ) {
			final String[] columnAliases = loadable.getSubclassPropertyColumnAliases(
					propertyName,
					suffix
			);
			if ( columnAliases.length != 0 ) {
				resultBuilderEntity.addProperty(
						propertyName,
						columnAliases
				);
			}
		}
		return resultBuilderEntity;
	}

	private SQLLoadable getSQLLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister( entityName );
		if ( !(persister instanceof SQLLoadable) ) {
			throw new MappingException( "class persister is not SQLLoadable: " + entityName );
		}
		return (SQLLoadable) persister;
	}

	private String generateEntitySuffix() {
		return AliasConstantsHelper.get( entitySuffixSeed++ );
	}

	private String generateCollectionSuffix() {
		return collectionSuffixSeed++ + "__";
	}

	private void processReturn(NativeQuery.ResultNode rtn) {
		if ( rtn instanceof NativeQuery.RootReturn ) {
			processRootReturn( ( NativeQuery.RootReturn ) rtn );
		}
		else if ( rtn instanceof NativeQuery.FetchReturn ) {
			processFetchReturn( (NativeQuery.FetchReturn) rtn );
		}
		else if ( rtn instanceof NativeQuery.InstantiationResultNode<?> ) {
			processConstructorReturn( (NativeQuery.InstantiationResultNode<?>) rtn );
		}
		else if ( rtn instanceof NativeQuery.ReturnProperty ) {
			processScalarReturn( ( NativeQuery.ReturnProperty ) rtn );
		}
		else if ( rtn instanceof NativeQuery.ReturnableResultNode ) {
		}
		else {
			throw new IllegalStateException(
					"Unrecognized NativeSQLQueryReturn concrete type encountered : " + rtn
			);
		}
	}

	private void processConstructorReturn(NativeQuery.InstantiationResultNode<?> rtn) {

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
		SQLLoadableCollection collectionPersister = ( SQLLoadableCollection ) factory.getCollectionPersister( role );
		alias2CollectionPersister.put( alias, collectionPersister );
		String suffix = generateCollectionSuffix();
		LOG.tracev( "Mapping alias [{0}] to collection-suffix [{1}]", alias, suffix );
		alias2CollectionSuffix.put( alias, suffix );
		collectionPropertyResultMaps.put( alias, propertyResults );

		if ( collectionPersister.isOneToMany() || collectionPersister.isManyToMany() ) {
			SQLLoadable persister = ( SQLLoadable ) collectionPersister.getElementPersister();
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
			processReturn( alias2Return.get(ownerAlias) );
		}

		SQLLoadable ownerPersister = ( SQLLoadable ) alias2Persister.get( ownerAlias );
		Type returnType = ownerPersister.getPropertyType( fetchReturn.getFetchableName() );

		if ( returnType.isCollectionType() ) {
			String role = ownerPersister.getEntityName() + '.' + fetchReturn.getFetchableName();
			Map<String, String[]> propertyResultsMap = Collections.emptyMap();//fetchReturn.getPropertyResultsMap()
			addCollection( role, alias, propertyResultsMap );
//			collectionOwnerAliases.add( ownerAlias );
		}
		else if ( returnType.isEntityType() ) {
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

	public String[] collectQuerySpaces() {
		final HashSet<String> spaces = new HashSet<String>();
		collectQuerySpaces( spaces );
		return spaces.toArray( new String[ spaces.size() ] );
	}

	public void collectQuerySpaces(Collection<String> spaces) {
		for ( EntityPersister persister : alias2Persister.values() ) {
			Collections.addAll( spaces, (String[]) persister.getQuerySpaces() );
		}
		for ( CollectionPersister persister : alias2CollectionPersister.values() ) {
			final Type elementType = persister.getElementType();
			if ( elementType.isEntityType() && ! elementType.isAnyType() ) {
				final Joinable joinable = ( (EntityType) elementType ).getAssociatedJoinable( factory );
				Collections.addAll( spaces, (String[]) ( (EntityPersister) joinable ).getQuerySpaces() );
			}
		}
	}
}
