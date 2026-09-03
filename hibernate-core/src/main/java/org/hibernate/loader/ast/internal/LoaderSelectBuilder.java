/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.cascade.spi.CascadingActions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.Restrictable;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.FromClauseAccess;
import org.hibernate.sql.ast.spi.creation.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.SqlTuple;
import org.hibernate.sql.ast.spi.query.from.PluralTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InArrayPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InListPredicate;
import org.hibernate.sql.ast.spi.query.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.spi.query.predicate.PredicateContainer;
import org.hibernate.sql.ast.spi.query.select.QueryPart;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;


import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.internal.AuditHelper.isFetchableAuditExcluded;
import static org.hibernate.query.results.internal.ResultsHelper.attributeName;

/**
 * Builder for SQL AST trees used by {@link Loader} implementations.
 *
 * @author Steve Ebersole
 * @author Nahtan Xu
 */
public class LoaderSelectBuilder {

	/**
	 * Create an SQL AST select-statement for loading by unique key
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	@Nonnull
	public static SelectStatement createSelectByUniqueKey(
			@Nonnull Loadable loadable,
			@Nonnull List<? extends ModelPart> partsToSelect,
			@Nonnull ModelPart restrictedPart,
			@Nullable DomainResult<?> cachedDomainResult,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				loadable,
				partsToSelect,
				singletonList( restrictedPart ),
				cachedDomainResult,
				1,
				loadQueryInfluencers,
				lockOptions,
				determineGraphTraversalState( loadQueryInfluencers, sessionFactory.getJpaMetamodel() ),
				true,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect();
	}

	static SelectStatement createAssociationKeySelect(
			ToOneAttributeMapping association,
			ModelPart restrictedPart, LoadQueryInfluencers influencers,
			Consumer<JdbcParameter> parameterConsumer) {
		final var target = association.getEntityMappingType();
		final var factory = influencers.getSessionFactory();
		final var builder = new LoaderSelectBuilder( factory.getSqlTranslationEngine(), target,
				List.of( target.getIdentifierMapping() ), restrictedPart, null, 1, influencers,
				LockOptions.NONE, parameterConsumer, new SqlAliasBaseManager() );
		return builder.generateSelect( association );
	}

	/**
	 * Create a select-statement (SQL AST) for loading by multiple keys using a single SQL ARRAY parameter
	 */
	@Nonnull
	public static SelectStatement createSelectBySingleArrayParameter(
			@Nonnull Loadable loadable,
			@Nonnull ValuedModelPart restrictedPart,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull JdbcParameter jdbcArrayParameter,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var builder = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				loadable,
				null,
				singletonList( restrictedPart ),
				null,
				-1,
				influencers,
				lockOptions,
				determineGraphTraversalState( influencers, sessionFactory.getJpaMetamodel() ),
				true,
				null,
				sqlAliasBaseGenerator
		);

		final var rootQuerySpec = new QuerySpec( true );
		final var sqlAstCreationState = builder.createSqlAstCreationState( rootQuerySpec );

		final var rootNavigablePath = new NavigablePath( loadable.getRootPathName() );
		rootQuerySpec.applyRootPathForLocking( rootNavigablePath );

		final var rootTableGroup =
				builder.buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		final var domainResult = loadable.createDomainResult(
				rootNavigablePath,
				rootTableGroup,
				null,
				sqlAstCreationState
		);

		final List<DomainResult<?>> domainResults = singletonList( domainResult );

		applyArrayParamRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				restrictedPart,
				jdbcArrayParameter,
				sqlAstCreationState
		);

		if ( loadable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			builder.applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
			builder.applyOrdering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
		}
		else {
			builder.applyFiltering( rootQuerySpec, rootTableGroup, (Restrictable) loadable, sqlAstCreationState );
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private static void applyArrayParamRestriction(
			@Nonnull QuerySpec rootQuerySpec,
			@Nonnull NavigablePath rootNavigablePath,
			@Nonnull TableGroup rootTableGroup,
			@Nonnull ValuedModelPart restrictedPart,
			@Nonnull JdbcParameter jdbcArrayParameter,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		assert restrictedPart.getJdbcTypeCount() == 1;
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var restrictedPartMapping = restrictedPart.getSelectable( 0 );
		final var restrictionPath =
				rootNavigablePath.append( restrictedPart.getNavigableRole().getNavigableName() );
		final var tableReference =
				rootTableGroup.resolveTableReference( restrictionPath,
						restrictedPartMapping.getContainingTableExpression() );
		final var columnRef =
				(ColumnReference)
						sqlExpressionResolver.resolveSqlExpression( tableReference, restrictedPartMapping );

		rootQuerySpec.applyPredicate( new InArrayPredicate( columnRef, jdbcArrayParameter ) );
	}

	/**
	 * Create an SQL AST select-statement based on matching one-or-more keys
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param numberOfKeysToLoad How many keys should be accounted for in the where clause restriction?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	@Nonnull
	public static SelectStatement createSelect(
			@Nonnull Loadable loadable,
			@Nullable List<? extends ModelPart> partsToSelect,
			@Nonnull ModelPart restrictedPart,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				loadable,
				partsToSelect,
				restrictedPart,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect();
	}

	@Nonnull
	public static SelectStatement createSelect(
			@Nonnull Loadable loadable,
			@Nullable List<? extends ModelPart> partsToSelect,
			@Nonnull List<ModelPart> restrictedParts,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect();
	}

	// TODO: this method is probably unnecessary if we make
	// determineWhetherToForceIdSelection() a bit smarter
	@Nonnull
	static SelectStatement createSelect(
			@Nonnull Loadable loadable,
			@Nullable List<ModelPart> partsToSelect,
			boolean forceIdentifierSelection,
			@Nonnull List<ModelPart> restrictedParts,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				determineGraphTraversalState( loadQueryInfluencers, sessionFactory.getJpaMetamodel() ),
				forceIdentifierSelection,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect();
	}

	/**
	 * Create an SQL AST select-statement used for subselect-based CollectionLoader
	 *
	 * @param attributeMapping The plural-attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 *
	 * @see CollectionLoaderSubSelectFetch
	 */
	@Nonnull
	public static SelectStatement createSubSelectFetchSelect(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nullable DomainResult<?> cachedDomainResult,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				cachedDomainResult,
				-1,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect( subselect, sqlAliasBaseGenerator );
	}

	/**
	 * Create an SQL AST select-statement used for subselect-based to-one entity loading
	 *
	 * @param entityMapping The entity being loaded
	 * @param attributeMapping The to-one attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 *
	 * @see EntityLoaderSubSelectFetch
	 */
	@Nonnull
	public static SelectStatement createSubSelectFetchSelect(
			@Nonnull EntityMappingType entityMapping,
			@Nonnull ToOneAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nullable DomainResult<?> cachedDomainResult,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		final var process = new LoaderSelectBuilder(
				sessionFactory.getSqlTranslationEngine(),
				entityMapping,
				null,
				entityMapping.getIdentifierMapping(),
				cachedDomainResult,
				-1,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer,
				sqlAliasBaseGenerator
		);
		return process.generateSelect( attributeMapping, subselect, sqlAliasBaseGenerator );
	}

	private final SqlAstCreationContext creationContext;
	private final Loadable loadable;
	@Nullable
	private final List<? extends ModelPart> partsToSelect;
	private final List<ModelPart> restrictedParts;
	@Nullable
	private final DomainResult<?> cachedDomainResult;
	private final int numberOfKeysToLoad;
	private final boolean forceIdentifierSelection;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;
	@Nullable
	private final Consumer<JdbcParameter> jdbcParameterConsumer;
	@Nullable
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private RowCardinality rowCardinality = RowCardinality.SINGLE;
	private final SqlAliasBaseGenerator sqlAliasBasGenerator;

	private LoaderSelectBuilder(
			@Nonnull SqlAstCreationContext creationContext,
			@Nonnull Loadable loadable,
			@Nullable List<? extends ModelPart> partsToSelect,
			@Nonnull List<ModelPart> restrictedParts,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nullable EntityGraphTraversalState entityGraphTraversalState,
			boolean forceIdentifierSelection,
			@Nullable Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBasGenerator) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedParts = restrictedParts;
		this.cachedDomainResult = cachedDomainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions;
		this.entityGraphTraversalState = entityGraphTraversalState;
		this.forceIdentifierSelection = forceIdentifierSelection;
		this.jdbcParameterConsumer = jdbcParameterConsumer;
		this.sqlAliasBasGenerator = sqlAliasBasGenerator;
		if ( loadable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			if ( pluralAttributeMapping.getMappedType().getCollectionSemantics()
						.getCollectionClassification() == CollectionClassification.BAG ) {
				rowCardinality = RowCardinality.BAG;
			}
		}
	}

	private LoaderSelectBuilder(
			@Nonnull SqlAstCreationContext creationContext,
			@Nonnull Loadable loadable,
			@Nullable List<? extends ModelPart> partsToSelect,
			@Nonnull List<ModelPart> restrictedParts,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nullable LockOptions lockOptions,
			@Nullable Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBasGenerator) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions != null ? lockOptions : new LockOptions(),
				determineGraphTraversalState( loadQueryInfluencers, creationContext.getJpaMetamodel() ),
				determineWhetherToForceIdSelection( loadable, numberOfKeysToLoad, restrictedParts, loadQueryInfluencers ),
				jdbcParameterConsumer,
				sqlAliasBasGenerator
		);
	}

	private LoaderSelectBuilder(
			@Nonnull SqlAstCreationContext creationContext,
			@Nonnull Loadable loadable,
			@Nullable List<? extends ModelPart> partsToSelect,
			@Nonnull ModelPart restrictedPart,
			@Nullable DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull LockOptions lockOptions,
			@Nullable Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull SqlAliasBaseGenerator sqlAliasBasGenerator) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				singletonList( restrictedPart ),
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer,
				sqlAliasBasGenerator
		);
	}

	private static boolean determineWhetherToForceIdSelection(
			@Nonnull Loadable loadable,
			int numberOfKeysToLoad,
			@Nonnull List<ModelPart> restrictedParts,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( numberOfKeysToLoad > 1 ) {
			return true;
		}

		if ( restrictedParts.size() == 1 ) {
			final var restrictedPart = restrictedParts.get( 0 );
			if ( Objects.equals( restrictedPart.getPartName(), NaturalIdMapping.PART_NAME ) ) {
				return true;
			}
		}

		for ( var restrictedPart : restrictedParts ) {
			if ( restrictedPart instanceof ForeignKeyDescriptor
					|| restrictedPart instanceof NonAggregatedIdentifierMapping ) {
				return true;
			}
		}

		// Force identifier selection if the identifier type cascades on refresh
		return influencers.getEnabledCascadingFetchProfile() == CascadingFetchProfile.REFRESH
				&& loadable.asEntityMappingType().getEntityPersister().getIdentifierCascadeStyle().doCascade( CascadingActions.REFRESH );
	}

	@Nullable
	private static EntityGraphTraversalState determineGraphTraversalState(
			@Nullable LoadQueryInfluencers loadQueryInfluencers,
			@Nonnull JpaMetamodel jpaMetamodel) {
		if ( loadQueryInfluencers != null ) {
			final var effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
			if ( effectiveEntityGraph != null ) {
				final var graphSemantic = effectiveEntityGraph.getSemantic();
				final var rootGraph = effectiveEntityGraph.getGraph();
				if ( graphSemantic != null && rootGraph != null ) {
					return new StandardEntityGraphTraversalStateImpl( graphSemantic, rootGraph, jpaMetamodel );
				}
			}
		}
		return null;
	}

	@Nonnull
	private SelectStatement generateSelect() {
		return generateSelect( null );
	}

	private SelectStatement generateSelect(@Nullable ToOneAttributeMapping association) {
		final var rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final var rootQuerySpec = new QuerySpec( true );
		rootQuerySpec.applyRootPathForLocking( rootNavigablePath );

		final var sqlAstCreationState = createSqlAstCreationState( rootQuerySpec );

		final var rootTableGroup = buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		final List<DomainResult<?>> domainResults;
		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = buildRequestedDomainResults( rootNavigablePath, sqlAstCreationState, rootTableGroup );
		}
		else if ( cachedDomainResult != null ) {
			domainResults = singletonList( cachedDomainResult );
		}
		else {
			final var domainResult = loadable.createDomainResult(
					rootNavigablePath,
					rootTableGroup,
					null,
					sqlAstCreationState
			);
			domainResults = singletonList( domainResult );
		}

		for ( var restrictedPart : restrictedParts ) {
			applyRestriction(
					rootQuerySpec,
					rootNavigablePath,
					rootTableGroup,
					restrictedPart,
					restrictedPart.getJdbcTypeCount(),
					castNonNull( jdbcParameterConsumer ),
					sqlAstCreationState
			);
		}

		if ( loadable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
			applyOrdering( rootQuerySpec, rootTableGroup, pluralAttributeMapping, sqlAstCreationState );
		}
		else {
			applyFiltering( rootQuerySpec, rootTableGroup, (Restrictable) loadable, sqlAstCreationState );
		}

		if ( association != null ) {
			association.applyAssociationRestrictions( rootQuerySpec::applyPredicate, rootTableGroup, sqlAstCreationState );
		}
		return new SelectStatement( rootQuerySpec, domainResults );
	}

	@Nonnull
	private List<DomainResult<?>> buildRequestedDomainResults(
			@Nonnull NavigablePath rootNavigablePath, @Nonnull LoaderSqlAstCreationState sqlAstCreationState, @Nonnull TableGroup rootTableGroup) {
		final var partsToSelect = castNonNull( this.partsToSelect );
		final List<DomainResult<?>> domainResults = new ArrayList<>( partsToSelect.size() );
		for ( var part : partsToSelect ) {
			final var navigablePath = rootNavigablePath.append( part.getPartName() );
			final TableGroup tableGroup;
			if ( part instanceof TableGroupJoinProducer tableGroupJoinProducer ) {
				final var tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
						navigablePath,
						rootTableGroup,
						null,
						null,
						SqlAstJoinType.LEFT,
						true,
						false,
						sqlAstCreationState
				);
				rootTableGroup.addTableGroupJoin( tableGroupJoin );
				tableGroup = tableGroupJoin.getJoinedGroup();
				sqlAstCreationState.getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
				registerPluralTableGroupParts( sqlAstCreationState.getFromClauseAccess(), tableGroup );
			}
			else {
				tableGroup = rootTableGroup;
			}
			domainResults.add(
					part.createDomainResult(
							navigablePath,
							tableGroup,
							null,
							sqlAstCreationState
					)
			);
		}
		return domainResults;
	}

	@Nonnull
	private TableGroup buildRootTableGroup(
			@Nonnull NavigablePath rootNavigablePath, @Nonnull QuerySpec rootQuerySpec, @Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		final var rootTableGroup = loadable.createRootTableGroup(
				true,
				rootNavigablePath,
				null,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );
		registerPluralTableGroupParts( sqlAstCreationState.getFromClauseAccess(), rootTableGroup );
		return rootTableGroup;
	}

	@Nonnull
	private LoaderSqlAstCreationState createSqlAstCreationState(@Nonnull QuerySpec rootQuerySpec) {
		return new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBasGenerator,
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				forceIdentifierSelection,
				loadQueryInfluencers,
				creationContext
		);
	}

	@Nonnull
	private SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBasGenerator;
	}

	private void applyRestriction(
			@Nonnull QuerySpec rootQuerySpec,
			@Nonnull NavigablePath rootNavigablePath,
			@Nonnull TableGroup rootTableGroup,
			@Nonnull ModelPart restrictedPart,
			int numberColumns,
			@Nonnull Consumer<JdbcParameter> jdbcParameterConsumer,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var navigablePath =
				rootNavigablePath.append( restrictedPart.getNavigableRole().getNavigableName() );

		if ( numberColumns == 1 ) {
			restrictedPart.forEachSelectable(
					(columnIndex, selection) -> {
						final var tableReference =
								rootTableGroup.resolveTableReference( navigablePath,
										selection.getContainingTableExpression() );
						final var columnRef =
								(ColumnReference)
										sqlExpressionResolver.resolveSqlExpression( tableReference, selection );
						if ( numberOfKeysToLoad == 1 ) {
							final var jdbcParameter = new SqlTypedMappingJdbcParameter( selection );
							jdbcParameterConsumer.accept( jdbcParameter );
							rootQuerySpec.applyPredicate(
									new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
							);
						}
						else {
							final var predicate = new InListPredicate( columnRef );
							for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
								final var jdbcParameter = new SqlTypedMappingJdbcParameter( selection );
								jdbcParameterConsumer.accept( jdbcParameter );
								predicate.addExpression( jdbcParameter );
							}
							rootQuerySpec.applyPredicate( predicate );
						}
					}
			);

		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberColumns );
			restrictedPart.forEachSelectable(
					(columnIndex, selection) -> {
						final var tableReference =
								rootTableGroup.resolveTableReference( navigablePath,
										selection.getContainingTableExpression() );
						columnReferences.add(
								(ColumnReference)
										sqlExpressionResolver.resolveSqlExpression( tableReference, selection )
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, restrictedPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>( numberColumns );
				restrictedPart.forEachSelectable(
						(columnIndex, selection) -> {
							final JdbcParameter jdbcParameter = new SqlTypedMappingJdbcParameter( selection );
							jdbcParameterConsumer.accept( jdbcParameter );
							tupleParams.add( jdbcParameter );
						}
				);
				final SqlTuple paramTuple = new SqlTuple( tupleParams, restrictedPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private void applyFiltering(
			@Nonnull QuerySpec querySpec,
			@Nonnull TableGroup tableGroup,
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull SqlAstCreationState astCreationState) {
		// Only apply restrictions for root table groups,
		// because for table group joins the restriction is applied
		// via PluralAttributeMappingImpl.createTableGroupJoin
		assert tableGroup.getNavigablePath().getParent() == null;
		pluralAttributeMapping.applyBaseRestrictions(
				querySpec::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				false,
				null,
				astCreationState
		);
		pluralAttributeMapping.applyBaseManyToManyRestrictions(
				querySpec::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				null,
				astCreationState
		);
	}

	private void applyFiltering(
			@Nonnull PredicateContainer predicateContainer,
			@Nonnull TableGroup tableGroup,
			@Nonnull Restrictable restrictable,
			@Nonnull SqlAstCreationState astCreationState) {
		restrictable.applyBaseRestrictions(
				predicateContainer::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				true,
				null,
				astCreationState
		);
	}

	private void applyOrdering(
			@Nonnull QuerySpec querySpec,
			@Nonnull TableGroup tableGroup,
			@Nonnull PluralAttributeMapping pluralAttributeMapping,
			@Nonnull SqlAstCreationState astCreationState) {
		final var orderByFragment = pluralAttributeMapping.getOrderByFragment();
		if ( orderByFragment != null ) {
			applyOrdering( querySpec, tableGroup, orderByFragment, astCreationState );
		}

		final var manyToManyOrderByFragment = pluralAttributeMapping.getManyToManyOrderByFragment();
		if ( manyToManyOrderByFragment != null ) {
			applyOrdering(
					querySpec,
					tableGroup,
					manyToManyOrderByFragment,
					astCreationState
			);
		}
	}

	private void applyOrdering(
			@Nonnull QuerySpec querySpec,
			@Nonnull TableGroup tableGroup,
			@Nonnull OrderByFragment orderByFragment,
			@Nonnull SqlAstCreationState astCreationState) {
		orderByFragment.apply( querySpec, tableGroup, astCreationState );
	}

	@Nonnull
	private ImmutableFetchList visitFetches(@Nonnull FetchParent fetchParent, @Nonnull LoaderSqlAstCreationState creationState) {
		final var fetches = new ImmutableFetchList.Builder( fetchParent.getReferencedMappingContainer() );
		final var processor = createFetchableConsumer( fetchParent, creationState, fetches );

		final var referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		if ( fetchParent.getNavigablePath().getParent() != null ) {
			final int size = referencedMappingContainer.getNumberOfKeyFetchables();
			for ( int i = 0; i < size; i++ ) {
				processor.accept( referencedMappingContainer.getKeyFetchable( i ), true, false );
			}
		}

		final int size = referencedMappingContainer.getNumberOfFetchables();
		List<Fetchable> bagFetchables = null;
		for ( int i = 0; i < size; i++ ) {
			final Fetchable fetchable = referencedMappingContainer.getFetchable( i );
			if ( isBag( fetchable ) ) {
				if ( bagFetchables == null ) {
					bagFetchables = new ArrayList<>();
				}
				// Delay processing of bag fetchables at last since they cannot be joined and will create subsequent selects
				bagFetchables.add( fetchable );
			}
			else {
				processor.accept( fetchable, false, false );
			}
		}
		if ( bagFetchables != null ) {
			for ( Fetchable fetchable : bagFetchables ) {
				processor.accept( fetchable, false, true );
			}
		}
		return fetches.build();
	}

	private boolean isBag(@Nonnull Fetchable fetchable) {
		return isPluralAttributeMapping( fetchable )
			&& ( (PluralAttributeMapping) fetchable ).getMappedType().getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.BAG;
	}

	private boolean isPluralAttributeMapping(@Nonnull Fetchable fetchable) {
		final var attributeMapping = fetchable.asAttributeMapping();
		return attributeMapping != null && attributeMapping.isPluralAttributeMapping();
	}

	@FunctionalInterface
	private interface FetchableConsumer {
		void accept(@Nonnull Fetchable fetchable, boolean isKeyFetchable, boolean isABag);
	}

	@Nonnull
	private FetchableConsumer createFetchableConsumer(
			@Nonnull FetchParent fetchParent,
			@Nonnull LoaderSqlAstCreationState creationState,
			@Nonnull ImmutableFetchList.Builder fetches) {
		return (fetchable, isKeyFetchable, isABag) -> {
			if ( !fetchable.isSelectable() || isFetchableAuditExcluded( fetchable, fetchParent, loadQueryInfluencers ) ) {
				return;
			}

			final var fetchablePath = getFetchablePath( fetchParent, fetchable, isKeyFetchable );

			final var mappedFetchOptions = fetchable.getMappedFetchOptions();
			var fetchTiming = mappedFetchOptions.getTiming();
			boolean joined = mappedFetchOptions.getStyle() == FetchStyle.JOIN;
			boolean explicitFetch = false;
			EntityGraphTraversalState.TraversalResult traversalResult = null;

			final boolean isFetchablePluralAttributeMapping = isABag || isPluralAttributeMapping( fetchable );
			final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();
			boolean cascadeReachable = false;

			if ( !( fetchable instanceof CollectionPart ) ) {
				// 'entity graph' takes precedence over 'fetch profile'
				if ( entityGraphTraversalState != null ) {
					traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, isKeyFetchable );
					final var fetchStrategy = traversalResult.getFetchStrategy();
					creationState.registerFetchOptions( fetchablePath, traversalResult.getFetchOptions() );
					if ( fetchStrategy != null ) {
						fetchTiming = fetchStrategy.getFetchTiming();
						joined = fetchStrategy.isJoined();
						explicitFetch = shouldExplicitFetch( maximumFetchDepth, fetchable, creationState );
					}
				}
				else if ( loadQueryInfluencers.hasEnabledFetchProfiles() ) {
					// There is no point in checking the fetch profile if it can't affect this fetchable
					if ( fetchTiming != FetchTiming.IMMEDIATE || fetchable.incrementFetchDepth() ) {
						final String fetchableRole = fetchable.getNavigableRole().getFullPath();
						for ( String enabledFetchProfileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
							final var enabledFetchProfile = creationContext.getFetchProfile( enabledFetchProfileName );
							final var profileFetch = enabledFetchProfile.getFetchByRole( fetchableRole );
							if ( profileFetch != null ) {
								fetchTiming = profileFetch.getTiming();
								joined = joined || profileFetch.getMethod() == FetchStyle.JOIN;
								explicitFetch = shouldExplicitFetch( maximumFetchDepth, fetchable, creationState );
							}
						}
					}
				}
				else if ( loadQueryInfluencers.getEnabledCascadingFetchProfile() != null ) {
					final var attributeMapping = fetchable.asAttributeMapping();
					final var cascadeStyle =
							attributeMapping != null
									? attributeMapping.getAttributeMetadata().getCascadeStyle()
									: null;
					final var cascadingAction =
							loadQueryInfluencers.getEnabledCascadingFetchProfile().getCascadingAction();
					if ( cascadeStyle == null || cascadeStyle.doCascade( cascadingAction ) ) {
						fetchTiming = FetchTiming.IMMEDIATE;
						// In 5.x the CascadeEntityJoinWalker only join fetched the first collection fetch
						joined = !isFetchablePluralAttributeMapping || rowCardinality == RowCardinality.SINGLE;
						cascadeReachable = true;
					}
				}
			}

			if ( joined && isFetchablePluralAttributeMapping ) {
				joined = switch ( rowCardinality ) {
					case SET -> !isABag;
					case BAG -> false;
					default -> true;
				};
			}

			// Disable the cascade profile for non-cascaded fetchables so sub-fetches aren't eagerly loaded
			final var originalCascadingFetchProfile = !cascadeReachable
					? loadQueryInfluencers.getEnabledCascadingFetchProfile()
					: null;
			try {
				if ( fetchable.incrementFetchDepth() ) {
					fetchDepth++;
				}
				if ( originalCascadingFetchProfile != null ) {
					loadQueryInfluencers.setEnabledCascadingFetchProfile( null );
				}

				// There is no need to check for circular fetches if this is an explicit fetch
				if ( !explicitFetch && !creationState.isResolvingCircularFetch() ) {
					final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
							fetchablePath,
							fetchParent,
							fetchTiming,
							creationState
					);

					if ( biDirectionalFetch != null ) {
						fetches.add( biDirectionalFetch );
						return;
					}
				}

				if ( maximumFetchDepth != null ) {
					if ( fetchDepth == maximumFetchDepth + 1 ) {
						joined = false;
					}
					else if ( fetchDepth > maximumFetchDepth + 1 ) {
						// We can preserve the existing value of joined for basic and embedded values
						if ( fetchable.asBasicValuedModelPart() == null
								&& !( fetchable instanceof EmbeddedAttributeMapping ) ) {
							joined = false;
						}
					}
				}

				if ( joined && isFetchablePluralAttributeMapping ) {
					rowCardinality = isABag ? RowCardinality.BAG : RowCardinality.SET;
				}

				final Fetch fetch = fetchParent.generateFetchableFetch(
						fetchable,
						fetchablePath,
						fetchTiming,
						joined,
						null,
						creationState
				);

				fetches.add( fetch );
			}
			finally {
				if ( fetchable.incrementFetchDepth() ) {
					fetchDepth--;
				}
				if ( entityGraphTraversalState != null && traversalResult != null ) {
					entityGraphTraversalState.backtrack( traversalResult );
				}
				if ( originalCascadingFetchProfile != null ) {
					loadQueryInfluencers.setEnabledCascadingFetchProfile( originalCascadingFetchProfile );
				}
			}
		};
	}

	@Nonnull
	private static NavigablePath getFetchablePath(@Nonnull FetchParent fetchParent, @Nonnull Fetchable fetchable, boolean isKeyFetchable) {
		if ( isKeyFetchable ) {
			final var identifierMapping = getEntityIdentifierMapping( fetchParent );
			if ( identifierMapping != null ) {
				return new EntityIdentifierNavigablePath(
						fetchParent.getNavigablePath(),
						attributeName( identifierMapping )
				);
			}
			else {
				return fetchParent.resolveNavigablePath( fetchable );
			}
		}
		else {
			return fetchParent.resolveNavigablePath( fetchable );
		}
	}

	@Nullable
	private static EntityIdentifierMapping getEntityIdentifierMapping(@Nonnull FetchParent fetchParent) {
		if ( fetchParent instanceof BiDirectionalFetch parentAsBiDirectionalFetch ) {
			return parentAsBiDirectionalFetch.getFetchedMapping() instanceof EntityValuedFetchable entityFetchable
							? entityFetchable.getEntityMappingType().getIdentifierMapping()
							: null;
		}
		else {
			return fetchParent.getReferencedMappingContainer() instanceof EntityValuedModelPart entityModelPart
							? entityModelPart.getEntityMappingType().getIdentifierMapping()
							: null;
		}
	}

	private boolean shouldExplicitFetch(@Nullable Integer maxFetchDepth, @Nonnull Fetchable fetchable, @Nonnull LoaderSqlAstCreationState creationState) {
		/*
			Forcing the value of explicitFetch to true will disable the fetch circularity check and
			for already visited association or collection this will cause a StackOverflow if maxFetchDepth is null, see HHH-15391.
		 */
		if ( maxFetchDepth == null ) {
			if ( fetchable instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				return !creationState.isAssociationKeyVisited(
						toOneAttributeMapping.getForeignKeyDescriptor().getAssociationKey()
				);
			}
			else if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
				return !creationState.isAssociationKeyVisited(
						pluralAttributeMapping.getKeyDescriptor().getAssociationKey()
				);
			}
		}

		return true;
	}

	@Nonnull
	private SelectStatement generateSelect(@Nonnull SubselectFetch subselect, @Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator) {

		// todo (6.0) : we could even convert this to a join by piecing together
		//		parts from the subselect-fetch sql-ast.  e.g. today we do:
		// 			select ...
		// 			from collection_table c
		// 			where c.fk in (
		//      		select o.id
		//				from owner_table o
		//				where <original restriction>
		// 			)
		//  	but instead could do:
		// 			select ...
		// 			from owner_table o
		//				left join collection_table c on c.fk = o.id
		// 			where <original restriction>

		// just like with other load-paths, bag-mappings can potentially be problematic here

		// todo (6.0) : ^^ another interesting idea is to use `partsToSelect` here relative to the owner
		//		- so `loadable` is the owner entity-descriptor and the `partsToSelect` is the collection

		assert loadable instanceof PluralAttributeMapping;
		final var attributeMapping = (PluralAttributeMapping) loadable;

		final var rootNavigablePath = new NavigablePath( loadable.getRootPathName() );
		final var rootQuerySpec = new QuerySpec( true );
		rootQuerySpec.applyRootPathForLocking( rootNavigablePath );

		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBaseGenerator,
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				numberOfKeysToLoad > 1,
				loadQueryInfluencers,
				creationContext
		);

		final var rootTableGroup = buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		// generate and apply the restriction
		applySubSelectRestriction( rootQuerySpec, rootTableGroup, subselect, sqlAstCreationState );

		// NOTE: no need to check - we are explicitly processing a plural-attribute
		applyFiltering( rootQuerySpec, rootTableGroup, attributeMapping, sqlAstCreationState );
		applyOrdering( rootQuerySpec, rootTableGroup, attributeMapping, sqlAstCreationState );

		// register the jdbc-parameters
		// todo (6.0) : analyzing the call paths, it seems like `jdbcParameterConsumer`
		//		never does anything for sub-select-fetch select building.
		//subselect.getLoadingJdbcParameters().forEach( jdbcParameterConsumer );

		return new SelectStatement(
				rootQuerySpec,
				singletonList(
						new CollectionDomainResult(
								rootNavigablePath,
								attributeMapping,
								null,
								rootTableGroup,
								sqlAstCreationState
						)
				)
		);
	}

	@Nonnull
	private SelectStatement generateSelect(
			@Nonnull ToOneAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator) {
		assert loadable instanceof EntityMappingType;
		final var entityMapping = (EntityMappingType) loadable;

		final var rootNavigablePath = new NavigablePath( loadable.getRootPathName() );
		final var rootQuerySpec = new QuerySpec( true );
		rootQuerySpec.applyRootPathForLocking( rootNavigablePath );

		final var sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				sqlAliasBaseGenerator,
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				true,
				loadQueryInfluencers,
				creationContext
		);

		final var rootTableGroup = buildRootTableGroup( rootNavigablePath, rootQuerySpec, sqlAstCreationState );

		final List<DomainResult<?>> domainResults =
				cachedDomainResult == null
						? singletonList(
								loadable.createDomainResult(
										rootNavigablePath,
										rootTableGroup,
										null,
										sqlAstCreationState
								)
						)
						: singletonList( cachedDomainResult );

		applySubSelectRestriction( rootQuerySpec, rootTableGroup, attributeMapping, subselect, sqlAstCreationState );
		applyFiltering( rootQuerySpec, rootTableGroup, entityMapping, sqlAstCreationState );

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private void applySubSelectRestriction(
			@Nonnull QuerySpec querySpec,
			@Nonnull TableGroup rootTableGroup,
			@Nonnull SubselectFetch subselect,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		assert loadable instanceof PluralAttributeMapping;

		final var attributeMapping = (PluralAttributeMapping) loadable;
		final var fkDescriptor = attributeMapping.getKeyDescriptor();

		final Expression fkExpression;
		if ( !fkDescriptor.isEmbedded() ) {
			assert fkDescriptor instanceof SimpleForeignKeyDescriptor;
			final var simpleFkDescriptor = (SimpleForeignKeyDescriptor) fkDescriptor;
			final var tableReference =
					rootTableGroup.resolveTableReference( null, fkDescriptor,
							simpleFkDescriptor.getContainingTableExpression() );
			fkExpression =
					sqlAstCreationState.getSqlExpressionResolver()
							.resolveSqlExpression( tableReference, simpleFkDescriptor );
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( fkDescriptor.getJdbcTypeCount() );
			fkDescriptor.forEachSelectable(
					(columnIndex, selection) -> {
						final var tableReference =
								rootTableGroup.resolveTableReference( null, fkDescriptor,
										selection.getContainingTableExpression() );
						columnReferences.add(
								(ColumnReference)
										sqlAstCreationState.getSqlExpressionResolver()
												.resolveSqlExpression( tableReference, selection )
						);
					}
			);
			fkExpression = new SqlTuple( columnReferences, fkDescriptor );
		}

		querySpec.applyPredicate(
				new InSubQueryPredicate(
						fkExpression,
						generateSubSelect( attributeMapping, subselect, sqlAstCreationState ),
						false
				)
		);
	}

	private void applySubSelectRestriction(
			@Nonnull QuerySpec querySpec,
			@Nonnull TableGroup rootTableGroup,
			@Nonnull ToOneAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		final var fkDescriptor = attributeMapping.getForeignKeyDescriptor();
		final var targetPart = fkDescriptor.getPart( attributeMapping.getSideNature().inverse() );

		querySpec.applyPredicate(
				new InSubQueryPredicate(
						createPartExpression( rootTableGroup, targetPart, sqlAstCreationState ),
						generateSubSelect( attributeMapping, subselect, sqlAstCreationState ),
						false
				)
		);
	}

	@Nonnull
	private Expression createPartExpression(
			@Nonnull TableGroup tableGroup,
			@Nonnull ValuedModelPart modelPart,
			@Nonnull LoaderSqlAstCreationState sqlAstCreationState) {
		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		if ( modelPart.getJdbcTypeCount() == 1 ) {
			final var selectable = modelPart.getSelectable( 0 );
			final var tableReference =
					tableGroup.resolveTableReference( null, modelPart, selectable.getContainingTableExpression() );
			return sqlExpressionResolver.resolveSqlExpression( tableReference, selectable );
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( modelPart.getJdbcTypeCount() );
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final var tableReference =
								tableGroup.resolveTableReference( null, modelPart,
										selection.getContainingTableExpression() );
						columnReferences.add(
								(ColumnReference)
										sqlExpressionResolver.resolveSqlExpression( tableReference, selection )
						);
					}
			);
			return new SqlTuple( columnReferences, modelPart );
		}
	}

	@Nonnull
	private QueryPart generateSubSelect(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nonnull LoaderSqlAstCreationState creationState) {
		final var fkDescriptor = attributeMapping.getKeyDescriptor();
		final var subQuery = new QuerySpec( false );
		final var loadingSqlAst = subselect.getLoadingSqlAst();
		final var ownerTableGroup = subselect.getOwnerTableGroup();

		// transfer the from-clause
		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final var sqlExpressionResolver = creationState.getSqlExpressionResolver();

		fkDescriptor.visitTargetSelectables(
				(valuesPosition, selection) -> {
					// for each column, resolve a SqlSelection and add it to the sub-query select-clause
					final var tableReference =
							ownerTableGroup.resolveTableReference( null, fkDescriptor,
									selection.getContainingTableExpression() );
					subQuery.getSelectClause()
							.addSqlSelection( new SqlSelectionImpl( valuesPosition,
									sqlExpressionResolver.resolveSqlExpression( tableReference, selection ) ) );
				}
		);

		// transfer the restriction
		subQuery.applyPredicate( loadingSqlAst.getWhereClauseRestrictions() );

		return subQuery;
	}

	@Nonnull
	private QueryPart generateSubSelect(
			@Nonnull ToOneAttributeMapping attributeMapping,
			@Nonnull SubselectFetch subselect,
			@Nonnull LoaderSqlAstCreationState creationState) {
		final var fkDescriptor = attributeMapping.getForeignKeyDescriptor();
		final var ownerPart = fkDescriptor.getPart( attributeMapping.getSideNature() );
		final var subQuery = new QuerySpec( false );
		final var loadingSqlAst = subselect.getLoadingSqlAst();
		final var ownerTableGroup = subselect.getOwnerTableGroup();

		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final var sqlExpressionResolver = creationState.getSqlExpressionResolver();
		ownerPart.forEachSelectable(
				(valuesPosition, selection) -> {
					final var tableReference =
							ownerTableGroup.resolveTableReference( null, ownerPart,
									selection.getContainingTableExpression() );
					subQuery.getSelectClause()
							.addSqlSelection( new SqlSelectionImpl( valuesPosition,
									sqlExpressionResolver.resolveSqlExpression( tableReference, selection ) ) );
				}
		);

		subQuery.applyPredicate( loadingSqlAst.getWhereClauseRestrictions() );

		return subQuery;
	}

	private void registerPluralTableGroupParts(@Nonnull FromClauseAccess fromClauseAccess, @Nonnull TableGroup tableGroup) {
		if ( tableGroup instanceof PluralTableGroup pluralTableGroup ) {
			if ( pluralTableGroup.getElementTableGroup() != null ) {
				final var elementTableGroup = pluralTableGroup.getElementTableGroup();
				fromClauseAccess.registerTableGroup( elementTableGroup.getNavigablePath(), elementTableGroup );
			}
			if ( pluralTableGroup.getIndexTableGroup() != null ) {
				final var indexTableGroup = pluralTableGroup.getIndexTableGroup();
				fromClauseAccess.registerTableGroup( indexTableGroup.getNavigablePath(), indexTableGroup );
			}
		}
	}

	/**
	 * Describes the JDBC result set cardinality per entity result object.
	 */
	private enum RowCardinality {
		/**
		 * Means that there is a single JDBC result row per entity result object.
		 */
		SINGLE,
		/**
		 * Means there are multiple JDBC result rows per entity result object,
		 * but the aggregation of rows is not affected the result cardinality.
		 */
		SET,
		/**
		 * Means there are multiple JDBC result rows per entity result object,
		 * but the aggregation of rows is dependent on the result cardinality.
		 */
		BAG
	}
}
