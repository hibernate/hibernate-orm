/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.LegacyFetchBuilder;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderCollectionStandard;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderContainer;
import org.hibernate.query.results.internal.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.collections.ArrayHelper.slice;
import static org.hibernate.internal.util.collections.ArrayHelper.toStringArray;
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

	public SQLQueryParser.ParserContext process() {
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		resultSetMapping.visitResultBuilders(
				(i, resultBuilder) -> {
					if ( resultBuilder instanceof NativeQuery.RootReturn rootReturn ) {
						alias2Return.put( rootReturn.getTableAlias(), rootReturn );
						resultBuilder.visitFetchBuilders( this::processFetchBuilder );
					}
					else if ( resultBuilder instanceof NativeQuery.CollectionReturn collectionReturn ) {
						alias2Return.put( collectionReturn.getTableAlias(), collectionReturn );
						addCollection(
								collectionReturn.getNavigablePath().getFullPath(),
								collectionReturn.getTableAlias(),
								emptyMap() //fetchReturn.getPropertyResultsMap()
						);
					}
				}
		);

		// handle fetches defined using {@code hbm.xml} or NativeQuery apis
		resultSetMapping.visitLegacyFetchBuilders( fetchBuilder -> {
			alias2Return.put( fetchBuilder.getTableAlias(),
					(NativeQuery.ReturnableResultNode) fetchBuilder );
			alias2OwnerAlias.put( fetchBuilder.getTableAlias(), fetchBuilder.getOwnerAlias() );
		} );

		// Now, process the returns
		for ( var queryReturn : alias2Return.values() ) {
			processReturn( queryReturn );
		}

		return this;
	}

	private void processFetchBuilder(Fetchable attributeName, FetchBuilder fetchBuilder) {
		if ( fetchBuilder instanceof LegacyFetchBuilder legacyFetchBuilder ) {
			resultSetMapping.addLegacyFetchBuilder( legacyFetchBuilder );
		}
		else if ( fetchBuilder instanceof NativeQuery.FetchReturn fetchReturn ) {
			alias2Return.put( fetchReturn.getTableAlias(), fetchReturn );
			alias2OwnerAlias.put( fetchReturn.getTableAlias(), fetchReturn.getOwnerAlias() );
		}
		fetchBuilder.visitFetchBuilders( this::processFetchBuilder );
	}

	public ResultSetMapping generateResultMapping(boolean queryHadAliases) {
		if ( queryHadAliases ) {
			final var mapping = resolveResultSetMapping( null, false, factory );
			final Set<String> visited = new HashSet<>();
			resultSetMapping.visitResultBuilders( (i, builder)
					-> visitResultSetBuilder( mapping, builder, visited ) );
			resultSetMapping.visitLegacyFetchBuilders( builder
					-> applyFetchBuilder( mapping, builder, visited ) );
			return mapping;
		}
		else {
			return resultSetMapping;
		}

	}

	private void visitResultSetBuilder(
			ResultSetMapping resultSetMapping,
			ResultBuilder resultBuilder,
			Set<String> visited) {
		if ( resultBuilder instanceof NativeQuery.RootReturn rootReturn ) {
			final String suffix = alias2Suffix.get( rootReturn.getTableAlias() );
			visited.add( rootReturn.getTableAlias() );
			if ( suffix == null ) {
				resultSetMapping.addResultBuilder( resultBuilder );
			}
			else {
				final var resultBuilderEntity = createSuffixedResultBuilder( rootReturn, suffix );
				resultSetMapping.addResultBuilder( resultBuilderEntity );
				alias2Return.put( rootReturn.getTableAlias(), resultBuilderEntity );
			}
		}
		else if ( resultBuilder instanceof NativeQuery.CollectionReturn collectionReturn ) {
			final String suffix = alias2CollectionSuffix.get( collectionReturn.getTableAlias() );
			if ( suffix == null ) {
				resultSetMapping.addResultBuilder( resultBuilder );
			}
			else {
				final var resultBuilderCollection =
						createSuffixedResultBuilder( collectionReturn, suffix,
								alias2Suffix.get( collectionReturn.getTableAlias() ) );
				resultSetMapping.addResultBuilder( resultBuilderCollection );
				alias2Return.put( collectionReturn.getTableAlias(), resultBuilderCollection );
			}
		}
		else {
			resultSetMapping.addResultBuilder( resultBuilder );
		}
	}

	private void applyFetchBuilder(
			ResultSetMapping resultSetMapping,
			LegacyFetchBuilder fetchBuilder,
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
						(DynamicFetchBuilderLegacy)
								alias2Return.get( fetchBuilder.getOwnerAlias() ),
						visited
				);
			}
			// At this point, the owner builder must be a
			// DynamicResultBuilderEntityStandard to which
			// we can add this builder
			final var ownerBuilder =
					(DynamicResultBuilderEntityStandard)
							alias2Return.get( fetchBuilder.getOwnerAlias() );
			final var resultBuilderEntity =
					createSuffixedResultBuilder(
							alias2Persister.get( fetchBuilder.getTableAlias() )
									.findContainingEntityMapping(),
							fetchBuilder.getTableAlias(),
							suffix,
							null,
							determineNavigablePath( fetchBuilder )
					);
			ownerBuilder.addFetchBuilder(
					fetchBuilder.getFetchable(),
					new DynamicFetchBuilderLegacy(
							fetchBuilder.getTableAlias(),
							fetchBuilder.getOwnerAlias(),
							fetchBuilder.getFetchable(),
							columnNames( resultBuilderEntity, fetchBuilder ),
							emptyMap(),
							resultBuilderEntity
					)
			);
//			resultSetMapping.addResultBuilder( resultBuilderEntity );
			alias2Return.put( fetchBuilder.getTableAlias(), resultBuilderEntity );
		}
	}

	private List<String> columnNames(
			DynamicResultBuilderEntityStandard resultBuilder,
			LegacyFetchBuilder fetchBuilder) {
		final String[] columnAliases =
				alias2Persister.get( fetchBuilder.getOwnerAlias() )
						.getSubclassPropertyColumnAliases( fetchBuilder.getFetchable().getFetchableName(),
								alias2Suffix.get( fetchBuilder.getOwnerAlias() ) );
		if ( columnAliases.length == 0 ) {
			final var collectionPersister = alias2CollectionPersister.get( fetchBuilder.getTableAlias() );
			if ( collectionPersister == null ) {
				return emptyList();
			}
			else {
				final String collectionSuffix = alias2CollectionSuffix.get( fetchBuilder.getTableAlias() );
				if ( collectionPersister.hasIndex() ) {
					final var fetchable = (PluralAttributeMapping) fetchBuilder.getFetchable();
					resultBuilder.addProperty( fetchable.getIndexDescriptor(),
							collectionPersister.getIndexColumnAliases( collectionSuffix ) );
				}
				return asList( collectionPersister.getKeyColumnAliases( collectionSuffix ) );
			}
		}
		else {
			return asList( columnAliases );
		}
	}

	private NavigablePath determineNavigablePath(LegacyFetchBuilder fetchBuilder) {
		final var ownerResult = alias2Return.get( fetchBuilder.getOwnerAlias() );
		NavigablePath path;
		if ( ownerResult instanceof NativeQuery.RootReturn rootReturn ) {
			path = rootReturn.getNavigablePath();
		}
		else if ( ownerResult instanceof DynamicFetchBuilderLegacy dynamicFetchBuilderLegacy ) {
			path = determineNavigablePath( dynamicFetchBuilderLegacy );
		}
		else {
			throw new AssertionFailure( "Unexpected fetch builder" );
		}
		if ( alias2CollectionPersister.containsKey( fetchBuilder.getOwnerAlias() ) ) {
			path = path.append( "{element}" );
		}
		return path.append( fetchBuilder.getFetchable().getFetchableName() );
	}

	private DynamicResultBuilderEntityStandard createSuffixedResultBuilder(
			NativeQuery.RootReturn rootReturn,
			String suffix) {
		return createSuffixedResultBuilder(
				rootReturn.getEntityMapping(),
				rootReturn.getTableAlias(),
				suffix,
				rootReturn.getLockMode(),
				new NavigablePath( rootReturn.getEntityMapping().getEntityName(),
						rootReturn.getTableAlias() )
		);
	}

	private DynamicResultBuilderEntityStandard createSuffixedResultBuilder(
			EntityMappingType entityMapping,
			String tableAlias,
			String suffix,
			LockMode lockMode,
			NavigablePath navigablePath) {
		final var resultBuilderEntity =
				new DynamicResultBuilderEntityStandard( entityMapping, tableAlias, navigablePath );
		resultBuilderEntity.setLockMode( lockMode );

		final var persister = entityMapping.getEntityPersister();
		final String[] identifierAliases = persister.getIdentifierAliases( suffix );
		resultBuilderEntity.addIdColumnAliases( identifierAliases );
		resultBuilderEntity.setDiscriminatorAlias( persister.getDiscriminatorAlias( suffix ) );
		if ( persister.hasIdentifierProperty() ) {
			resultBuilderEntity.addProperty( persister.getIdentifierMapping(), identifierAliases );
		}

		persister.visitFetchables(
				(index, fetchable) -> {
					if ( fetchable.isSelectable() ) {
						addFetchBuilder(
								suffix,
								persister,
								resultBuilderEntity,
								tableAlias,
								identifierAliases,
								fetchable,
								persister.getSubclassPropertyColumnAliases( fetchable.getFetchableName(), suffix ),
								persister instanceof SingleTableEntityPersister singleTableEntityPersister
										? singleTableEntityPersister.getSubclassPropertyType( index )
										: persister.getPropertyType( fetchable.getFetchableName() )
						);
					}
				},
				null
		);
		return resultBuilderEntity;
	}

	private void addFetchBuilder(
			String suffix,
			EntityPersister loadable,
			DynamicFetchBuilderContainer resultBuilderEntity,
			String tableAlias,
			String[] identifierAliases,
			Fetchable fetchable,
			String[] columnAliases,
			Type propertyType) {
		if ( propertyType instanceof CollectionType collectionType ) {
			final String[] keyColumnAliases =
					collectionType.useLHSPrimaryKey()
							? identifierAliases
							: loadable.getSubclassPropertyColumnAliases( collectionType.getLHSPropertyName(), suffix );
			resultBuilderEntity.addProperty( fetchable, keyColumnAliases );
		}
		else if ( propertyType instanceof ComponentType componentType ) {
			final var fetchBuilder = new DynamicFetchBuilderLegacy(
					"",
					tableAlias,
					fetchable,
					asList( columnAliases ),
					new HashMap<>()
			);
			final String[] propertyNames = componentType.getPropertyNames();
			final Type[] propertyTypes = componentType.getSubtypes();
			int aliasIndex = 0;
			for ( int i = 0; i < propertyNames.length; i++ ) {
				final Type type = propertyTypes[i];
				final int columnSpan = type.getColumnSpan( loadable.getFactory().getRuntimeMetamodels() );
				addFetchBuilder(
						suffix,
						loadable,
						fetchBuilder,
						tableAlias,
						identifierAliases,
						fetchable,
						slice( columnAliases, aliasIndex, columnSpan ),
						type
				);
				aliasIndex += columnSpan;
			}

			resultBuilderEntity.addFetchBuilder( fetchable, fetchBuilder );
		}
		else if ( columnAliases.length != 0 ) {
			if ( propertyType instanceof EntityType ) {
				final var toOne = (ToOneAttributeMapping) fetchable;
				if ( !toOne.getIdentifyingColumnsTableExpression().equals( loadable.getTableName() ) ) {
					// The to-one has a join-table, use the plain join column name instead of the alias
					assert columnAliases.length == 1;
					final String[] targetAliases = new String[1];
					targetAliases[0] = toOne.getTargetKeyPropertyName();
					resultBuilderEntity.addProperty( fetchable, targetAliases );
					return;
				}
			}
			resultBuilderEntity.addProperty( fetchable, columnAliases );
		}
	}

	private CompleteResultBuilderCollectionStandard createSuffixedResultBuilder(
			NativeQuery.CollectionReturn collectionReturn,
			String suffix,
			String entitySuffix) {
		final var collectionPersister = collectionReturn.getPluralAttribute().getCollectionDescriptor();
		return new CompleteResultBuilderCollectionStandard(
				collectionReturn.getTableAlias(),
				collectionReturn.getNavigablePath(),
				collectionReturn.getPluralAttribute(),
				collectionPersister.getKeyColumnAliases( suffix ),
				collectionPersister.hasIndex()
						? collectionPersister.getIndexColumnAliases( suffix )
						: null,
				getElementColumnAliases( suffix, entitySuffix, collectionPersister )
		);
	}

	private static String[] getElementColumnAliases(
			String suffix, String entitySuffix, CollectionPersister collectionPersister) {
		if ( collectionPersister.getElementType().isEntityType() ) {
			final var elementPersister = collectionPersister.getElementPersister();
			final String[] propertyNames = elementPersister.getPropertyNames();
			final String[] identifierAliases = elementPersister.getIdentifierAliases( entitySuffix );
			final String discriminatorAlias = elementPersister.getDiscriminatorAlias( entitySuffix );
			final int size =
					propertyNames.length + identifierAliases.length
						+ (discriminatorAlias == null ? 0 : 1);
			final List<String> aliases = new ArrayList<>( size );
			addAll( aliases, identifierAliases );
			if ( discriminatorAlias != null ) {
				aliases.add( discriminatorAlias );
			}
			for ( int i = 0; i < propertyNames.length; i++ ) {
				addAll( aliases, elementPersister.getPropertyAliases( entitySuffix, i ) );
			}
			return toStringArray( aliases );
		}
		else {
			return collectionPersister.getElementColumnAliases( suffix );
		}
	}

	private String generateEntitySuffix() {
		return AliasConstantsHelper.get( entitySuffixSeed++ );
	}

	private String generateCollectionSuffix() {
		return collectionSuffixSeed++ + "__";
	}

	private void processReturn(NativeQuery.ResultNode resultNode) {
		if ( resultNode instanceof NativeQuery.RootReturn rootReturn ) {
			processRootReturn( rootReturn );
		}
		else if ( resultNode instanceof NativeQuery.FetchReturn fetchReturn ) {
			processFetchReturn( fetchReturn );
		}
		else if ( resultNode instanceof NativeQuery.InstantiationResultNode<?> instantiationResultNode ) {
			processConstructorReturn( instantiationResultNode );
		}
		else if ( resultNode instanceof NativeQuery.ReturnProperty returnProperty ) {
			processScalarReturn( returnProperty );
		}
		else if ( resultNode instanceof NativeQuery.ReturnableResultNode returnableResultNode ) {
			processPropertyReturn( returnableResultNode );
		}
		else {
			throw new AssertionFailure( "Unrecognized ResultNode concrete type: " + resultNode );
		}
	}

	private void processPropertyReturn(NativeQuery.ReturnableResultNode returnableResultNode) {
		//nothing to do
	}

	private void processConstructorReturn(NativeQuery.InstantiationResultNode<?> instantiationResultNode) {
		//nothing to do
	}

	private void processScalarReturn(NativeQuery.ReturnProperty typeReturn) {
//		scalarColumnAliases.add( typeReturn.getColumnAlias() );
//		scalarTypes.add( typeReturn.getType() );
	}

	private void processRootReturn(NativeQuery.RootReturn rootReturn) {
		if ( !alias2Persister.containsKey( rootReturn.getTableAlias() ) ) {
			addPersister(
					rootReturn.getTableAlias(),
					emptyMap(), //rootReturn.getPropertyResultsMap(),
					rootReturn.getEntityMapping().getEntityPersister()
			);
		}
		// else already processed
	}

	private void addPersister(String alias, Map<String, String[]> propertyResult, EntityPersister persister) {
		alias2Persister.put( alias, persister );
		alias2Suffix.put( alias, generateEntitySuffix() );
		entityPropertyResultMaps.put( alias, propertyResult );
	}

	private void addCollection(String role, String alias, Map<String, String[]> propertyResults) {
		final var collectionDescriptor =
				factory.getMappingMetamodel()
						.getCollectionDescriptor( role );
		alias2CollectionPersister.put( alias, collectionDescriptor );
		alias2CollectionSuffix.put( alias, generateCollectionSuffix() );
		collectionPropertyResultMaps.put( alias, propertyResults );
		if ( collectionDescriptor.isOneToMany() || collectionDescriptor.isManyToMany() ) {
			addPersister(
					alias,
					filter( propertyResults ),
					collectionDescriptor.getElementPersister()
			);
		}
	}

	private Map<String, String[]> filter(Map<String, String[]> propertyResults) {
		final Map<String, String[]> result = new HashMap<>( propertyResults.size() );
		final String keyPrefix = "element.";
		for ( var element : propertyResults.entrySet() ) {
			final String path = element.getKey();
			if ( path.startsWith( keyPrefix ) ) {
				result.put( path.substring( keyPrefix.length() ),
						element.getValue() );
			}
		}
		return result;
	}

	private void processFetchReturn(NativeQuery.FetchReturn fetchReturn) {
		final String alias = fetchReturn.getTableAlias();
		if ( !alias2Persister.containsKey( alias ) && !alias2CollectionPersister.containsKey( alias ) ) {
			final String ownerAlias = fetchReturn.getOwnerAlias();

			// Make sure the owner alias is known...
			if ( !alias2Return.containsKey( ownerAlias ) ) {
				throw new HibernateException( "Owner alias [" + ownerAlias + "] is unknown for alias [" + alias + "]" );
			}

			// If this return's alias has not been processed yet, do so before further processing of this return
			if ( !alias2Persister.containsKey( ownerAlias ) ) {
				processReturn( alias2Return.get( ownerAlias ) );
			}

			final var ownerPersister = alias2Persister.get( ownerAlias );
			final String fetchableName = fetchReturn.getFetchable().getFetchableName();
			final Type returnType = ownerPersister.getPropertyType( fetchableName );
			if ( returnType instanceof CollectionType ) {
				addCollection(
						ownerPersister.getEntityName() + '.' + fetchableName,
						alias,
						emptyMap() //fetchReturn.getPropertyResultsMap()
				);
	//			collectionOwnerAliases.add( ownerAlias );
			}
			else if ( returnType instanceof EntityType entityType ) {
				addPersister(
						alias,
						emptyMap(), //fetchReturn.getPropertyResultsMap()
						factory.getMappingMetamodel()
								.getEntityDescriptor( entityType.getAssociatedEntityName() )
				);
			}
		}
		// else already processed
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
	public EntityPersister getEntityPersister(String alias) {
		return alias2Persister.get( alias );
	}

	@Override
	public CollectionPersister getCollectionPersister(String alias) {
		return alias2CollectionPersister.get( alias );
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
		final var collectionMap = collectionPropertyResultMaps.get( alias );
		if ( collectionMap != null ) {
			return collectionMap;
		}
		final var entityMap = entityPropertyResultMaps.get( alias );
		if ( entityMap != null ) {
			return entityMap;
		}

		final var resultNode = alias2Return.get( alias );
		return resultNode instanceof NativeQuery.ReturnProperty
			&& !( resultNode instanceof NativeQuery.FetchReturn )
				? null
				// todo (6.0): access property results map somehow which was on NativeSQLQueryNonScalarReturn before
				: emptyMap();
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
