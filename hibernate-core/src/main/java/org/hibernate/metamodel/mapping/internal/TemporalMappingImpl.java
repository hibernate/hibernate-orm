package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import org.hibernate.mapping.Column;
import org.hibernate.dialect.temporal.spi.TemporalRestrictionRequest;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Stateful;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.LegacyAuxiliaryMutationSupport;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.spi.query.from.LazyTableGroup;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.exec.internal.TemporalJdbcParameter;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.hibernate.metamodel.mapping.internal.CurrentState.ownerRevision;
import static org.hibernate.boot.model.internal.TemporalHelper.ROW_END;
import static org.hibernate.boot.model.internal.TemporalHelper.ROW_START;
import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN_OR_EQUAL;

/**
 * Temporal mapping implementation.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public class TemporalMappingImpl implements TemporalMapping, LegacyAuxiliaryMutationSupport {
	private final String tableName;
	private final SelectableMapping startingColumnMapping;
	private final SelectableMapping endingColumnMapping;
	private final JdbcMapping jdbcMapping;
	@Nullable private final String currentTimestampFunctionName;
	@Nullable private final Expression currentTimestampExpression;
	private final TemporalTableStrategy temporalTableStrategy;

	public TemporalMappingImpl(
			Stateful bootMapping,
			String tableName,
			MappingModelCreationProcess creationProcess) {
		this.tableName = tableName;

		final var creationContext = creationProcess.getCreationContext();
		temporalTableStrategy =
				creationContext.getSessionFactoryOptions()
						.getTemporalTableStrategy();

		final var startingColumn = bootMapping.getAuxiliaryColumn( ROW_START );
		final var endingColumn = bootMapping.getAuxiliaryColumn( ROW_END );
		final var startingValue = (BasicValue) startingColumn.getValue();
		final var endingValue = (BasicValue) endingColumn.getValue();

		final var startingResolution = startingValue.resolve();
		final var endingResolution = endingValue.resolve();

		jdbcMapping = startingResolution.getJdbcMapping();
		if ( jdbcMapping != endingResolution.getJdbcMapping() ) {
			throw new IllegalStateException( "Temporal starting and ending columns must use the same JDBC mapping" );
		}

		startingColumnMapping = startingColumnMapping( tableName, startingColumn, creationContext );
		endingColumnMapping = endingColumnMapping( tableName, endingColumn, creationContext );

		final var dialect = creationContext.getDialect();
		if ( creationContext.getSessionFactory()
				.getChangesetCoordinator()
				.useServerTimestamp( dialect ) ) {
			currentTimestampFunctionName = dialect.getCurrentTemporalSupport().currentTimestamp();
			currentTimestampExpression =
					new SelfRenderingSqlFragmentExpression( currentTimestampFunctionName, jdbcMapping );
		}
		else {
			currentTimestampFunctionName = null;
			currentTimestampExpression = null;
		}
	}

	@Nonnull
	private SelectableMapping endingColumnMapping(
			String tableName,
			Column endingColumn,
			RuntimeModelCreationContext creationContext) {
		return SelectableMappingImpl.from(
				tableName,
				endingColumn,
				null,
				null,
				jdbcMapping,
				true,
				true,
				false,
				false,
				creationContext
		);
	}

	@Nonnull
	private SelectableMapping startingColumnMapping(
			String tableName,
			Column startingColumn,
			RuntimeModelCreationContext creationContext) {
		return SelectableMappingImpl.from(
				tableName,
				startingColumn,
				null,
				null,
				jdbcMapping,
				true,
				false,
				false,
				false,
				creationContext
		);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return tableName;
	}

	@Nonnull
	@Override
	public SelectableMapping getStartingColumnMapping() {
		return startingColumnMapping;
	}

	@Nonnull
	@Override
	public SelectableMapping getEndingColumnMapping() {
		return endingColumnMapping;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	private Predicate createCurrentRestriction(TableReference tableReference) {
		return createCurrentRestriction( tableReference, null );
	}

	private Predicate createCurrentRestriction(TableReference tableReference, @Nullable SqlExpressionResolver expressionResolver) {
		final var endingColumn = resolveColumn( tableReference, expressionResolver, endingColumnMapping );
		return new NullnessPredicate( endingColumn, false, jdbcMapping );
	}

	private Predicate createRestriction(TableReference tableReference, Object temporalValue) {
		return createRestriction( tableReference, null, temporalValue );
	}

	private Predicate createRestriction(
			TableReference tableReference,
			@Nullable SqlExpressionResolver expressionResolver,
			Object temporalValue) {
		final Expression startingTemporalValue;
		final Expression endingTemporalValue;
		if ( currentTimestampExpression == null || temporalValue != null ) {
			startingTemporalValue = new TemporalJdbcParameter( startingColumnMapping );
			endingTemporalValue = new TemporalJdbcParameter( endingColumnMapping );
		}
		else {
			startingTemporalValue = currentTimestampExpression;
			endingTemporalValue = currentTimestampExpression;
		}
		return createRestriction( tableReference, expressionResolver, startingTemporalValue, endingTemporalValue );
	}

	private Predicate createRestriction(
			TableReference tableReference,
			@Nullable SqlExpressionResolver expressionResolver,
			Expression startingTemporalValue,
			Expression endingTemporalValue) {
		final var startingColumn = resolveColumn( tableReference, expressionResolver, startingColumnMapping );
		final var endingColumn = resolveColumn( tableReference, expressionResolver, endingColumnMapping );

		final var startingPredicate = new ComparisonPredicate( startingColumn, LESS_THAN_OR_EQUAL, startingTemporalValue );
		final var endingNullPredicate = new NullnessPredicate( endingColumn, false, jdbcMapping );
		final var endingAfterPredicate = new ComparisonPredicate( endingColumn, GREATER_THAN, endingTemporalValue );

		final var endingPredicate = new Junction( Junction.Nature.DISJUNCTION );
		endingPredicate.add( endingNullPredicate );
		endingPredicate.add( endingAfterPredicate );

		final var predicate = new Junction( Junction.Nature.CONJUNCTION );
		predicate.add( startingPredicate );
		predicate.add( endingPredicate );

		return predicate;
	}

	@Nonnull
	@Override
	public ColumnValueBinding createStartingValueBinding(@Nonnull ColumnReference startingColumnReference) {
		return createTemporalValueBinding( startingColumnReference, startingColumnMapping );
	}

	@Nonnull
	@Override
	public ColumnValueBinding createEndingValueBinding(@Nonnull ColumnReference endingColumnReference) {
		return createTemporalValueBinding( endingColumnReference, endingColumnMapping );
	}

	private ColumnValueBinding createTemporalValueBinding(
			ColumnReference endingColumnReference, SelectableMapping columnMapping) {
		return new ColumnValueBinding( endingColumnReference,
				currentTimestampFunctionName != null
						? new ColumnWriteFragment( currentTimestampFunctionName, emptyList(), columnMapping )
						: new ColumnWriteFragment( "?",
								new ColumnValueParameter( endingColumnReference ),
								columnMapping ) );
	}

	@Nonnull
	@Override
	public ColumnValueBinding createNullEndingValueBinding(@Nonnull ColumnReference endingColumnReference) {
		return new ColumnValueBinding( endingColumnReference,
				new ColumnWriteFragment( null, emptyList(), endingColumnMapping ) );
	}

	private Expression resolveColumn(
			TableReference tableReference,
			@Nullable SqlExpressionResolver expressionResolver,
			SelectableMapping selectableMapping) {
		return expressionResolver != null
				? expressionResolver.resolveSqlExpression( tableReference, selectableMapping )
				: new ColumnReference( tableReference, selectableMapping );
	}

	@Override
	public void addToInsertGroup(@Nonnull MutationGroupBuilder insertGroupBuilder, @Nonnull EntityPersister persister) {
		if ( temporalTableStrategy == TemporalTableStrategy.SINGLE_TABLE ) {
			final TableInsertBuilder insertBuilder =
					insertGroupBuilder.getTableDetailsBuilder( persister.getIdentifierTableName() );
			final var mutatingTable = insertBuilder.getMutatingTable();

			final var startingColumn = new ColumnReference( mutatingTable, getStartingColumnMapping() );
			insertBuilder.addValueColumn( createStartingValueBinding( startingColumn ) );

			final var endingColumn = new ColumnReference( mutatingTable, getEndingColumnMapping() );
			insertBuilder.addValueColumn( createNullEndingValueBinding( endingColumn ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityMappingType,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull LazyTableGroup lazyTableGroup,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState) {
		final var ownerRevision = ownerRevision( this, navigablePath, creationState );
		if ( ownerRevision != null ) {
			// read as of the revision of the audited snapshot that owns the association
			final var tableReference = lazyTableGroup.resolveTableReference( navigablePath, getTableName() );
			predicateConsumer.accept( createRestriction( tableReference, creationState.getSqlExpressionResolver(),
					ownerRevision, ownerRevision ) );
		}
		else if ( hasRowRestriction( creationState.getLoadQueryInfluencers() ) ) {
			final var tableReference = lazyTableGroup.resolveTableReference( navigablePath, getTableName() );
			final var temporalInstant = temporalInstant( creationState.getLoadQueryInfluencers() );
			final var resolver = creationState.getSqlExpressionResolver();
			final var temporalPredicate =
					temporalInstant == null
							? createCurrentRestriction( tableReference, resolver )
							: createRestriction( tableReference, resolver, temporalInstant );
			predicateConsumer.accept( temporalPredicate );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( hasRowRestriction( influencers ) ) {
			final var instant = temporalInstant( influencers );
			final var primaryTableReference =
					tableGroup.resolveTableReference( getTableName() );
			predicateConsumer.accept( instant == null
					? createCurrentRestriction( primaryTableReference )
					: createRestriction( primaryTableReference, instant ) );
		}
		else if ( !tableGroup.isInitialized() && requiresTargetTableForKeyRead( influencers ) ) {
			// Native temporal SQL applies the changeset to the table reference itself.
			tableGroup.getPrimaryTableReference();
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull PluralAttributeMapping associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( hasRowRestriction( influencers ) ) {
			final var instant = temporalInstant( influencers );
			final var tableReference = tableGroup.resolveTableReference( getTableName() );
			predicateConsumer.accept( instant == null
					? createCurrentRestriction( tableReference )
					: createRestriction( tableReference, instant ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull TableGroupJoin tableGroupJoin,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		if ( hasRowRestriction( loadQueryInfluencers ) ) {
			final var temporalInstant = temporalInstant( loadQueryInfluencers );
			final var tableReference = tableGroupJoin.getJoinedGroup().resolveTableReference( getTableName() );
			tableGroupJoin.applyPredicate( temporalInstant == null
					? createCurrentRestriction( tableReference )
					: createRestriction( tableReference, temporalInstant ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull Supplier<Consumer<Predicate>> predicateCollector,
			@Nonnull SqlAstCreationState creationState,
			@Nonnull TableGroup tableGroup,
			@Nonnull NamedTableReference rootTableReference,
			@Nonnull EntityMappingType entityMappingType) {
		if ( hasRowRestriction( creationState.getLoadQueryInfluencers() ) ) {
			final var tableReference =
					tableGroup.resolveTableReference( getTableName() );
			final var temporalInstant = temporalInstant( creationState.getLoadQueryInfluencers() );
			final var resolver = creationState.getSqlExpressionResolver();
			predicateCollector.get()
					.accept( temporalInstant == null
							? createCurrentRestriction( tableReference, resolver )
							: createRestriction( tableReference, resolver, temporalInstant ) );
			if ( tableReference != rootTableReference && creationState.supportsEntityNameUsage() ) {
				creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
						entityMappingType.getRootEntityDescriptor().getEntityName() );
			}
		}
	}

	@Override
	public void applyAssociationRestrictions(
			@Nonnull EntityMappingType associatedEntityDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		if ( ownerRevision != null && readsHistoryAtOwnerRevision( influencers ) ) {
			// read as of the revision of the audited snapshot that owns the association
			ownerRestrictions.accept( createRevisionRestriction( tableGroup, ownerRevision ) );
		}
		else {
			applyPredicate( associatedEntityDescriptor, rowRestrictions, tableGroup, aliases, influencers );
		}
	}

	@Override
	public void applyAssociationRestrictions(
			@Nonnull PluralAttributeMapping collectionDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		if ( ownerRevision != null && readsHistoryAtOwnerRevision( influencers ) ) {
			// read as of the revision of the audited snapshot that owns the collection
			ownerRestrictions.accept( createRevisionRestriction( tableGroup, ownerRevision ) );
		}
		else {
			applyPredicate( collectionDescriptor, rowRestrictions, tableGroup, aliases, influencers );
		}
	}

	/**
	 * Restricts a temporal entity joined as the element of a collection to its state
	 * as of the given revision.
	 */
	@Nonnull
	public Predicate createRevisionRestriction(@Nonnull TableGroup tableGroup, @Nonnull Expression revision) {
		return createRestriction( tableGroup.resolveTableReference( getTableName() ), null, revision, revision );
	}

	/**
	 * Whether the dialect reads history with a restriction, which can be bounded by a
	 * revision column, rather than with {@code AS OF}, which cannot.
	 */
	@Override
	public boolean readsHistoryAtOwnerRevision(@Nonnull LoadQueryInfluencers influencers) {
		final var sessionFactory = influencers.getSessionFactory();
		return sessionFactory.getJdbcServices().getDialect().getTemporalTableSupport()
				.useTemporalRestriction( new TemporalRestrictionRequest(
						temporalTableStrategy,
						true,
						sessionFactory.getChangesetCoordinator().isIdentifierTypeInstant()
				) );
	}

	/**
	 * The instant at which temporal state is read. A temporal entity has no
	 * revisions of its own, so an all-revisions query reads its current state.
	 */
	private static @Nullable Object temporalInstant(LoadQueryInfluencers influencers) {
		return influencers.isAllRevisions() ? null : influencers.getTemporalIdentifier();
	}

	@Override
	public boolean hasRowRestriction(@Nonnull LoadQueryInfluencers influencers) {
		return influencers.getSessionFactory().getJdbcServices().getDialect().getTemporalTableSupport()
				.useTemporalRestriction( temporalRestrictionRequest( influencers ) );
	}

	private static TemporalRestrictionRequest temporalRestrictionRequest(LoadQueryInfluencers influencers) {
		final var sessionFactory = influencers.getSessionFactory();
		return new TemporalRestrictionRequest(
				sessionFactory.getSessionFactoryOptions().getTemporalTableStrategy(),
				temporalInstant( influencers ) != null,
				sessionFactory.getChangesetCoordinator().isIdentifierTypeInstant()
		);
	}

	@Override
	public boolean useAuxiliaryTable(@Nonnull LoadQueryInfluencers influencers) {
		return temporalTableStrategy == TemporalTableStrategy.HISTORY_TABLE
			&& ( temporalInstant( influencers ) != null || influencers.readsHistoryTables() );
	}

	@Override
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return temporalInstant( influencers ) != null;
	}
}
