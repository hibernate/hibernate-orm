/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;

import static org.hibernate.engine.internal.TenantIdHelper.MissingRowPolicy.THROW;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.NoBatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesMappingProducer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.internal.model.builder.TableUpdateBuilderSkipped;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationOperationGroupFactory;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

import static org.hibernate.engine.OptimisticLockStyle.DIRTY;
import static org.hibernate.engine.internal.Versioning.isVersionIncrementRequired;
import static org.hibernate.generator.EventType.UPDATE;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
import static org.hibernate.internal.util.collections.ArrayHelper.contains;
import static org.hibernate.internal.util.collections.ArrayHelper.join;
import static org.hibernate.internal.util.collections.ArrayHelper.trim;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.query.sqm.ComparisonOperator.DISTINCT_FROM;
import static org.hibernate.sql.ast.spi.query.predicate.Junction.Nature.DISJUNCTION;

/**
 * Coordinates the updating of an entity.
 *
 * @see #update
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class UpdateCoordinatorStandard extends AbstractMutationCoordinator implements UpdateCoordinator {

	private final MutationOperationGroup staticUpdateGroup;
	@Nullable
	private final BatchKey batchKey;

	@Nullable
	private final MutationOperationGroup versionUpdateGroup;
	@Nullable
	private final BatchKey versionUpdateBatchkey;
	private final boolean hasCustomVersionUpdateSql;

	public UpdateCoordinatorStandard(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		// NOTE: even given dynamic-update and/or dirty optimistic locking
		// there are cases where we need the full static updates.
		staticUpdateGroup = buildStaticUpdateGroup();
		versionUpdateGroup = buildVersionUpdateGroup();
		hasCustomVersionUpdateSql = hasCustomVersionUpdateSql( versionUpdateGroup );
		if ( entityPersister.getUpdateDelegate() != null ) {
			// generated-values delegates execute the mutation themselves and cannot be batched
			batchKey = null;
			versionUpdateBatchkey = null;
		}
		else {
			batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#UPDATE" );
			versionUpdateBatchkey = new BasicBatchKey( entityPersister.getEntityName() + "#UPDATE_VERSION" );
		}
	}

	//Used by Hibernate Reactive to efficiently create new instances of this same class
	@SuppressWarnings("unused")
	protected UpdateCoordinatorStandard(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull MutationOperationGroup staticUpdateGroup,
			@Nonnull BatchKey batchKey,
			@Nonnull MutationOperationGroup versionUpdateGroup,
			@Nonnull BatchKey versionUpdateBatchkey) {
		super( entityPersister, factory );
		this.staticUpdateGroup = staticUpdateGroup;
		this.batchKey = batchKey;
		this.versionUpdateGroup = versionUpdateGroup;
		this.versionUpdateBatchkey = versionUpdateBatchkey;
		this.hasCustomVersionUpdateSql = hasCustomVersionUpdateSql( versionUpdateGroup );
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticUpdateGroup;
	}

	@Nullable
	protected MutationOperationGroup getVersionUpdateGroup() {
		return versionUpdateGroup;
	}

	@Nullable
	protected BatchKey getBatchKey() {
		return batchKey;
	}

	public final boolean isModifiableEntity(@Nullable EntityEntry entry) {
		return entry == null ? entityPersister().isMutable() : entry.isModifiableEntity();
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		if ( versionUpdateGroup == null ) {
			throw new HibernateException( "Cannot force version increment relative to subtype; use the root type" );
		}
		doVersionUpdate( null, id, nextVersion, currentVersion, getLoadedState( id, session ), session );
	}

	private @Nullable Object[] getLoadedState(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		return entityPersister.hasPartitionedSelectionMapping()
				? session.getPersistenceContextInternal()
				.getEntityHolder( session.generateEntityKey( id, entityPersister ) ).getEntityEntry().getLoadedState()
				: null;
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			boolean batching,
			@Nonnull SharedSessionContractImplementor session) {
		if ( versionUpdateGroup == null ) {
			throw new HibernateException( "Cannot force version increment relative to subtype; use the root type" );
		}
		doVersionUpdate( null, id, nextVersion, currentVersion, batching, getLoadedState( id, session ), session );
	}

	@Nullable
	@Override
	public GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] incomingDirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session) {
		TenantIdHelper.validateIdentifierTenant( id, entityPersister(), session );
		final var versionMapping = entityPersister().getVersionMapping();
		final boolean databaseDirtinessCheck =
				incomingOldValues == null
						&& incomingDirtyAttributeIndexes == null
						&& supportsDatabaseDirtinessCheck( versionMapping );
		if ( versionMapping != null ) {
			final var generatedValuesAccess =
					handlePotentialImplicitForcedVersionIncrement(
							entity,
							id,
							values,
							incomingOldValues,
							oldVersion,
							incomingDirtyAttributeIndexes,
							session,
							versionMapping
					);
			if ( generatedValuesAccess != null ) {
				return generatedValuesAccess.get();
			}
		}

		final var entry = session.getPersistenceContextInternal().getEntry( entity );

		// Ensure that an immutable or non-modifiable entity is not being updated unless it is
		// in the process of being deleted.
		if ( entry == null && !entityPersister().isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet" );
		}

		// apply any pre-update in-memory value generation
		final int[] preUpdateGeneratedAttributeIndexes = preUpdateInMemoryValueGeneration( entity, values, session );
		final int[] dirtyAttributeIndexes =
				databaseDirtinessCheck
						? null
						: dirtyAttributeIndexes( incomingDirtyAttributeIndexes, preUpdateGeneratedAttributeIndexes );

		final boolean temporalExcludedUpdate =
				entityPersister().excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection );

		final boolean[] attributeUpdateability;
		final boolean forceDynamicUpdate;
		if ( temporalExcludedUpdate ) {
			attributeUpdateability = getPropertiesToUpdate( dirtyAttributeIndexes, hasDirtyCollection );
			for ( int i = 0; i < attributeUpdateability.length; i++ ) {
				if ( attributeUpdateability[i] && !entityPersister().isPropertyTemporalExcluded( i ) ) {
					attributeUpdateability[i] = false;
				}
			}
			forceDynamicUpdate = true;
		}
		else if ( entityPersister().isDynamicUpdate() && dirtyAttributeIndexes != null ) {
			attributeUpdateability = getPropertiesToUpdate( dirtyAttributeIndexes, hasDirtyCollection );
			forceDynamicUpdate = true;
		}
		else if ( !isModifiableEntity( entry ) ) {
			// either the entity is mapped as immutable or has been marked as read-only within the Session
			attributeUpdateability = getPropertiesToUpdate(
					dirtyAttributeIndexes == null ? EMPTY_INT_ARRAY : dirtyAttributeIndexes,
					hasDirtyCollection
			);
			forceDynamicUpdate = true;
		}
		else if ( dirtyAttributeIndexes != null
				&& entityPersister().hasUninitializedLazyProperties( entity )
				&& hasLazyDirtyFields( entityPersister(), dirtyAttributeIndexes ) ) {
			// we have an entity with dirty lazy attributes.  we need to use dynamic
			// delete and add the dirty, lazy attributes plus the non-lazy attributes
			forceDynamicUpdate = true;
			attributeUpdateability = getPropertiesToUpdate( dirtyAttributeIndexes, hasDirtyCollection );

			final var propertyLaziness = entityPersister().getPropertyLaziness();
			for ( int i = 0; i < propertyLaziness.length; i++ ) {
				// add also all the non-lazy properties because dynamic update is false
				if ( !propertyLaziness[i] ) {
					attributeUpdateability[i] = true;
				}
			}
		}
		else {
			attributeUpdateability = getPropertyUpdateability( entity );
			forceDynamicUpdate = entityPersister().hasUninitializedLazyProperties( entity );
		}

		return performUpdate(
				entity,
				id,
				rowId,
				values,
				oldVersion,
				incomingOldValues,
				hasDirtyCollection,
				session,
				versionMapping,
				dirtyAttributeIndexes,
				attributeUpdateability,
				forceDynamicUpdate || databaseDirtinessCheck,
				databaseDirtinessCheck,
				temporalExcludedUpdate
		);
	}

	@Nullable
	protected GeneratedValues performUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityVersionMapping versionMapping,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull boolean[] attributeUpdateability,
			boolean forceDynamicUpdate,
			boolean databaseDirtinessCheck,
			boolean temporalExcludedUpdate) {

		final AttributeInclusionChecker dirtinessChecker =
				(position, attribute) -> isDirty(
						hasDirtyCollection,
						versionMapping,
						dirtyAttributeIndexes,
						attributeUpdateability,
						position,
						attribute,
						entityPersister()
				);

		final AttributeInclusionChecker lockingChecker =
				(position, attribute) -> includedInLock(
						versionMapping,
						dirtinessChecker,
						position,
						attribute,
						entityPersister()
				);

		final AttributeInclusionChecker inclusionChecker = (position, attribute) -> attributeUpdateability[position];

		final var valuesAnalysis = analyzeUpdateValues(
				entity,
				values,
				oldVersion,
				incomingOldValues,
				dirtyAttributeIndexes,
				createInclusionChecker( attributeUpdateability ),
				lockingChecker,
				dirtinessChecker,
				temporalExcludedUpdate,
				rowId,
				forceDynamicUpdate,
				databaseDirtinessCheck,
				session
		);

		if ( valuesAnalysis.tablesNeedingUpdate.isEmpty()
				&& valuesAnalysis.tablesNeedingDynamicUpdate.isEmpty() ) {
			// nothing to do
			return null;
		}
		else if ( valuesAnalysis.needsDynamicUpdate() ) {
			return doDynamicUpdate(
					entity,
					id,
					rowId,
					values,
					incomingOldValues,
					dirtinessChecker,
					valuesAnalysis,
					session
			);
		}
		else {
			return doStaticUpdate(
					entity,
					id,
					rowId,
					values,
					incomingOldValues,
					valuesAnalysis,
					session
			);
		}
	}

	@Nullable
	protected static int[] dirtyAttributeIndexes(@Nullable int[] incomingDirtyIndexes, @Nonnull int[] preUpdateGeneratedIndexes) {
		if ( preUpdateGeneratedIndexes.length == 0 ) {
			return incomingDirtyIndexes;
		}
		else {
			return incomingDirtyIndexes == null
					? preUpdateGeneratedIndexes
					: join( incomingDirtyIndexes, preUpdateGeneratedIndexes );
		}
	}

	private boolean supportsDatabaseDirtinessCheck(@Nullable EntityVersionMapping versionMapping) {
		final var persister = entityPersister();
		final var updateDelegate = persister.getUpdateDelegate();
		if ( versionMapping != null && !persister.isVersionPropertyGenerated()
				&& updateDelegate != null
				&& updateDelegate.getGeneratedValuesMappingProducer()
						instanceof GeneratedValuesMappingProducer mappingProducer ) {
			// The persister adds the application-generated version to the delegate's results
			// only when it has determined that a database dirtiness check is supported.
			for ( var resultBuilder : mappingProducer.getResultBuilders() ) {
				if ( resultBuilder.getModelPart() == versionMapping ) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isDirty(
			boolean hasDirtyCollection,
			@Nullable EntityVersionMapping versionMapping,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull boolean[] attributeUpdateability,
			int position,
			@Nonnull SingularAttributeMapping attribute,
			@Nonnull EntityPersister persister) {
		if ( !attributeUpdateability[position] ) {
			return false;
		}
		else if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attribute) {
			return isVersionIncrementRequired(
					dirtyAttributeIndexes,
					hasDirtyCollection,
					persister.getPropertyVersionability()
			);
		}
		else if ( dirtyAttributeIndexes == null ) {
			// we do not know, so assume it is
			return true;
		}
		else {
			return contains( dirtyAttributeIndexes, position );
		}
	}

	private static boolean includedInLock(
			@Nullable EntityVersionMapping versionMapping,
			@Nonnull AttributeInclusionChecker dirtinessChecker,
			int position,
			@Nonnull SingularAttributeMapping attribute,
			@Nonnull EntityPersister persister) {
		return switch ( persister.optimisticLockStyle() ) {
			case NONE -> false;
			case VERSION -> versionMapping != null
					&& versionMapping.getVersionAttribute() == attribute;
//						&& updateableAttributeIndexes[position];
			case ALL -> attribute.getAttributeMetadata().isIncludedInOptimisticLocking();
			case DIRTY -> attribute.getAttributeMetadata().isIncludedInOptimisticLocking()
					&& dirtinessChecker.include( position, attribute );
		};
	}

	@Nullable
	protected Supplier<GeneratedValues> handlePotentialImplicitForcedVersionIncrement(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nullable Object[] oldValues,
			@Nullable Object oldVersion,
			@Nullable int[] incomingDirtyAttributeIndexes,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull EntityVersionMapping versionMapping) {
		// Handle a case where the only value being updated is the version.
		// We treat this case specially in `#coordinateUpdate` to leverage
		// `#doVersionUpdate`.
		final Object newVersion;
		if ( hasUpdateGeneratedValues() || hasCustomVersionUpdateSql ) {
			// Can't use the version-only update: either there are fields
			// generated by the UPDATE event that must be included in the
			// statement, or a custom update SQL is defined which would
			// be bypassed by the version-only update path
			return null;
		}
		else if ( incomingDirtyAttributeIndexes != null ) {
			switch ( incomingDirtyAttributeIndexes.length ) {
				case 1:
					final int dirtyAttributeIndex = incomingDirtyAttributeIndexes[0];
					final var versionAttribute = versionMapping.getVersionAttribute();
					final var dirtyAttribute = entityPersister().getAttributeMapping( dirtyAttributeIndex );
					if ( versionAttribute == dirtyAttribute ) {
						// only the version attribute itself is dirty
						newVersion = values[dirtyAttributeIndex];
					}
					else {
						// the dirty field is some other field
						return null;
					}
					break;
				case 0:
					if ( oldVersion != null ) {
						newVersion = values[versionMapping.getVersionAttribute().getStateArrayPosition()];
						if ( versionMapping.areEqual( newVersion, oldVersion, session ) ) {
							return null;
						}
					}
					else {
						return null;
					}
					break;
				default:
					return null;
			}
		}
		else {
			return null;
		}

		// we have just the version being updated - use the special handling
		assert newVersion != null;
		final var generatedValues = doVersionUpdate(
				entity,
				id,
				newVersion,
				oldVersion,
				oldValues == null ? values : oldValues,
				session
		);
		return () -> generatedValues;
	}

	private boolean hasUpdateGeneratedValues() {
		final var entityMetamodel = entityPersister();
		return entityMetamodel.hasUpdateGeneratedProperties()
			|| entityMetamodel.hasPreUpdateGeneratedProperties();
	}

	private static boolean hasCustomVersionUpdateSql(@Nullable MutationOperationGroup versionUpdateGroup) {
		return versionUpdateGroup != null
				&& versionUpdateGroup.getSingleOperation().getTableDetails().getUpdateDetails().getCustomSql() != null;
	}

	private static boolean isValueGenerationOnUpdateInSql(@Nullable Generator generator, @Nonnull Dialect dialect) {
		return generator != null
			&& generator.generatedOnExecution()
			&& generator.generatesOnUpdate()
			&& ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect, EventType.UPDATE );
	}

	/**
	 * Which properties appear in the SQL update?
	 * (Initialized, updateable ones!)
	 */
	@Nonnull
	public boolean[] getPropertyUpdateability(@Nonnull Object entity) {
		return entityPersister().hasUninitializedLazyProperties( entity )
				? entityPersister().getNonLazyPropertyUpdateability()
				: entityPersister().getPropertyUpdateability();
	}

	@Nullable
	protected GeneratedValues doVersionUpdate(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nullable Object oldVersion,
			@Nullable Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session) {
		return doVersionUpdate(
				entity,
				id,
				version,
				oldVersion,
				true,
				loadedState,
				session
		);
	}

	@Nullable
	protected GeneratedValues doVersionUpdate(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nullable Object oldVersion,
			boolean batching,
			@Nullable Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session) {
		final var versionUpdateGroup = castNonNull( this.versionUpdateGroup );

		final var mutatingTableDetails =
				(EntityTableMapping) versionUpdateGroup.getSingleOperation().getTableDetails();

		final var mutationExecutor =
				updateVersionExecutor( session, versionUpdateGroup, false, batching );

		final var versionMapping = entityPersister().getVersionMapping();

		// set the new version
		mutationExecutor.getJdbcValueBindings().bindValue(
				version,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.SET
		);

		bindPartitionColumnValueBindings( loadedState, session, mutationExecutor.getJdbcValueBindings() );
		bindTenantRestriction( session, mutationExecutor.getJdbcValueBindings(), versionUpdateGroup );

		// restrict the key
		mutatingTableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) ->
						mutationExecutor.getJdbcValueBindings().bindValue(
								jdbcValue,
								mutatingTableDetails.getTableName(),
								columnMapping.getColumnName(),
								ParameterUsage.RESTRICT
						),
				session
		);

		// restrict the old-version
		mutationExecutor.getJdbcValueBindings().bindValue(
				oldVersion,
				mutatingTableDetails.getTableName(),
				versionMapping.getSelectionExpression(),
				ParameterUsage.RESTRICT
		);

		try {
			return mutationExecutor.execute(
					entity,
					null,
					tableMapping -> tableMapping.getTableName().equals( entityPersister.getIdentifierTableName() ),
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private int[] preUpdateInMemoryValueGeneration(
			@Nonnull Object object,
			@Nonnull Object[] newValues,
			@Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		if ( !persister.hasPreUpdateGeneratedProperties() ) {
			return EMPTY_INT_ARRAY;
		}

		final var generators = persister.getGenerators();
		if ( generators.length != 0 ) {
			final int[] fieldsPreUpdateNeeded = new int[generators.length];
			int count = 0;
			for ( int i = 0; i < generators.length; i++ ) {
				final var generator = generators[i];
				if ( generator != null
						&& generator.generatesOnUpdate()
						&& generator.generatedBeforeExecution( object, session ) ) {
					newValues[i] = ( (BeforeExecutionGenerator) generator ).generate( session, object, newValues[i], UPDATE );
					entityPersister().setValue( object, i, newValues[i] );
					fieldsPreUpdateNeeded[count++] = i;
				}
			}

			if ( count > 0 ) {
				return trim( fieldsPreUpdateNeeded, count );
			}
		}

		return EMPTY_INT_ARRAY;
	}

	@Nonnull
	public boolean[] getPropertyUpdateability() {
		return entityPersister().getPropertyUpdateability();
	}

	/**
	 * Transform the array of property indexes to an array of booleans for each attribute,
	 * true when the property is dirty
	 */
	@Nonnull
	protected boolean[] getPropertiesToUpdate(@Nullable final int[] dirtyProperties, final boolean hasDirtyCollection) {
		final var persister = entityPersister();
		if ( dirtyProperties == null ) {
			return getPropertyUpdateability();
		}
		else {
			final var updateability = persister.getPropertyUpdateability();
			final var insertability = persister.getPropertyInsertability();
			final var propsToUpdate = new boolean[persister.getNumberOfAttributeMappings()];
			for ( int property: dirtyProperties ) {
				propsToUpdate[property] = includeProperty( insertability, updateability, property );
			}
			if ( persister.isVersioned() ) {
				final var versionAttribute = persister.getVersionMapping().getVersionAttribute();
				if ( versionAttribute.isUpdateable() ) {
					final int versionAttributeIndex = versionAttribute.getStateArrayPosition();
					propsToUpdate[versionAttributeIndex] =
							propsToUpdate[versionAttributeIndex]
							|| isVersionIncrementRequired(
									dirtyProperties,
									hasDirtyCollection,
									persister.getPropertyVersionability()
							);
				}
			}
			return propsToUpdate;
		}
	}

	protected boolean includeProperty(@Nonnull boolean[] insertability, @Nonnull boolean[] updateability, int property) {
		return updateability[property];
	}

	@Nonnull
	protected UpdateValuesAnalysisImpl analyzeUpdateValues(
			@Nullable Object entity,
			@Nullable Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] oldValues,
			@Nullable int[] dirtyAttributeIndexes,
			@Nonnull AttributeInclusionChecker inclusionChecker,
			@Nonnull AttributeInclusionChecker lockingChecker,
			@Nonnull AttributeInclusionChecker dirtinessChecker,
			boolean restrictToTemporalExcluded,
			@Nullable Object rowId,
			boolean forceDynamicUpdate,
			boolean databaseDirtinessCheck,
			@Nullable SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		final var attributeMappings = persister.getAttributeMappings();

		// NOTE:
		// 		* `dirtyAttributeIndexes == null` means we had no snapshot and couldn't
		// 			get one using select-before-update; never the case for #merge
		//		* `oldValues == null` just means we had no snapshot to begin with - we might
		//			have used select-before-update to get the dirtyAttributeIndexes (again,
		//			never the case for #merge)
		final var analysis = new UpdateValuesAnalysisImpl(
				values,
				oldValues,
				dirtyAttributeIndexes,
				dirtinessChecker,
				rowId,
				forceDynamicUpdate,
				databaseDirtinessCheck
		);

		for ( int attributeIndex = 0; attributeIndex < attributeMappings.size(); attributeIndex++ ) {
			final var attributeMapping = attributeMappings.get( attributeIndex );
			analysis.startingAttribute( attributeMapping );

			try {
				if ( attributeMapping.getJdbcTypeCount() > 0
						&& attributeMapping instanceof SingularAttributeMapping singularAttributeMapping ) {
					processAttribute(
							entity,
							analysis,
							attributeIndex,
							singularAttributeMapping,
							oldVersion,
							oldValues,
							inclusionChecker,
							lockingChecker,
							restrictToTemporalExcluded,
							session
					);

					// In this case we check for exactly DirtynessStatus.DIRTY so to not log warnings when the user didn't get it wrong:
					if ( castNonNull( analysis.currentAttributeAnalysis ).getDirtynessStatus() == AttributeAnalysis.DirtynessStatus.DIRTY ) {
						if ( !includeProperty( persister.getPropertyInsertability(), persister.getPropertyUpdateability(), attributeIndex ) ) {
							CORE_LOGGER.ignoreImmutablePropertyModification( attributeMapping.getAttributeName(), persister.getEntityName() );
						}
					}
				}
			}
			finally {
				analysis.finishedAttribute( attributeMapping );
			}
		}

		return analysis;
	}

	private void processAttribute(
			@Nullable Object entity,
			@Nonnull UpdateValuesAnalysisImpl analysis,
			int attributeIndex,
			@Nonnull SingularAttributeMapping attributeMapping,
			@Nullable Object oldVersion,
			@Nullable Object[] oldValues,
			@Nonnull AttributeInclusionChecker inclusionChecker,
			@Nonnull AttributeInclusionChecker lockingChecker,
			boolean restrictToTemporalExcluded,
			@Nullable SharedSessionContractImplementor session) {

		final var generator =
				restrictToTemporalExcluded
						&& !entityPersister().isPropertyTemporalExcluded( attributeIndex )
				? null
				: attributeMapping.getGenerator();
		final boolean generatesOnUpdate =
				generator != null
						&& generator.generatesOnUpdate();
		final boolean needsDynamicUpdate =
				generatesOnUpdate
						&& session != null
						&& generator.generatedBeforeExecution( entity, session )
						// Only force dynamic update when the generator can switch to on-execution mode.
						&& generator.generatedOnExecution();
		final boolean generatedOnExecution =
				generatesOnUpdate
						&& ( session == null
							? generator.generatedOnExecution()
							: generator.generatedOnExecution( entity, session )
						);
		final boolean generatedInSql =
				generatedOnExecution
						&& generator instanceof OnExecutionGenerator onExecutionGenerator
						&& hasValueGenerationOnExecution( onExecutionGenerator, dialect, EventType.UPDATE );
		if ( generatedInSql
				&& !needsDynamicUpdate
				&& !( (OnExecutionGenerator) generator ).writePropertyValue( EventType.UPDATE ) ) {
			analysis.registerValueGeneratedInSqlNoWrite();
		}

		if ( needsDynamicUpdate || generatedInSql || inclusionChecker.include( attributeIndex, attributeMapping ) ) {
			final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				processSet( analysis, attributeMapping.getSelectable( i ), needsDynamicUpdate );
			}
		}

		if ( lockingChecker.include( attributeIndex, attributeMapping ) ) {
			processLock( analysis, attributeMapping, session,
					attributeLockValue( attributeIndex, attributeMapping, oldVersion, oldValues ) );
		}
	}

	@Nullable
	private Object attributeLockValue(
			int attributeIndex,
			@Nonnull SingularAttributeMapping attributeMapping,
			@Nullable Object oldVersion,
			@Nullable Object[] oldValues) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null
			&& versionMapping.getVersionAttribute() == attributeMapping ) {
			return oldVersion;
		}
		else {
			return oldValues == null ? null : oldValues[attributeIndex];
		}
	}

	private void processSet(@Nonnull UpdateValuesAnalysisImpl analysis, @Nullable SelectableMapping selectable, boolean needsDynamicUpdate) {
		if ( selectable != null && !selectable.isFormula() && isColumnIncludedInSet( selectable ) ) {
			final var tableMapping = physicalTableMappingForMutation( entityPersister(), selectable );
			analysis.registerColumnSet( tableMapping, selectable.getSelectionExpression(), selectable.getWriteExpression() );
			if ( needsDynamicUpdate ) {
				analysis.getTablesNeedingDynamicUpdate().add( tableMapping );
			}
		}
	}

	protected boolean isColumnIncludedInSet(@Nonnull SelectableMapping selectable) {
		return selectable.isUpdateable();
	}

	@Nonnull
	protected AttributeInclusionChecker createInclusionChecker(@Nonnull boolean[] attributeUpdateability) {
		return (position, attribute) -> attributeUpdateability[position];
	}

	private void processLock(
			@Nonnull UpdateValuesAnalysisImpl analysis,
			@Nonnull SingularAttributeMapping attributeMapping,
			@Nullable SharedSessionContractImplementor session,
			@Nullable Object attributeLockValue) {
		attributeMapping.decompose(
				attributeLockValue,
				0,
				analysis,
				null,
				(valueIndex, updateAnalysis, noop, jdbcValue, columnMapping) -> {
					if ( !columnMapping.isFormula() ) {
						updateAnalysis.registerColumnOptLock(
								physicalTableMappingForMutation( entityPersister(), columnMapping ),
								columnMapping.getSelectionExpression(),
								jdbcValue
						);
					}
				},
				session
		);
	}

	@Nullable
	protected GeneratedValues doStaticUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object[] oldValues,
			@Nonnull UpdateValuesAnalysisImpl valuesAnalysis,
			@Nonnull SharedSessionContractImplementor session) {
		checkTenantIdBeforeUpdate( id, staticUpdateGroup, valuesAnalysis.tablesNeedingUpdate::contains, session );

		final var mutationExecutor = executor( session, staticUpdateGroup, false );

		decomposeForUpdate(
				entity,
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				staticUpdateGroup,
//				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).isDirty(),
				(position, attribute) -> AttributeAnalysis.DirtynessStatus.CONSIDER_LIKE_DIRTY,
				session
		);
		// no snapshot when called from StatelessSession.update()
		bindPartitionColumnValueBindings( oldValues == null ? values : oldValues,
				session, mutationExecutor.getJdbcValueBindings() );
		bindTenantRestriction( session, mutationExecutor.getJdbcValueBindings(), staticUpdateGroup );

		try {
			return mutationExecutor.execute(
					entity,
					valuesAnalysis,
					valuesAnalysis.tablesNeedingUpdate::contains,
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	protected void decomposeForUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nonnull UpdateValuesAnalysisImpl valuesAnalysis,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull MutationOperationGroup jdbcOperationGroup,
			@Nonnull DirtinessChecker dirtinessChecker,
			@Nonnull SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		// apply values
		for ( int position = 0; position < jdbcOperationGroup.getNumberOfOperations(); position++ ) {
			final var operation = jdbcOperationGroup.getOperation( position );
			final var tableMapping = (EntityTableMapping) operation.getTableDetails();
			if ( valuesAnalysis.tablesNeedingUpdate.contains( tableMapping ) ) {
				for ( int attributeIndex : tableMapping.getAttributeIndexes() ) {
					decomposeAttributeForUpdate(
							entity,
							values,
							valuesAnalysis,
							dirtinessChecker,
							session,
							jdbcValueBindings,
							tableMapping,
							attributeIndex
					);
				}
			}
		}

		if ( valuesAnalysis.databaseDirtinessCheck ) {
			bindDatabaseDirtinessCheckValues( values, session, jdbcValueBindings );
		}

		// apply keys
		for ( int position = 0; position < jdbcOperationGroup.getNumberOfOperations(); position++ ) {
			final var operation = jdbcOperationGroup.getOperation( position );
			final var tableMapping = (EntityTableMapping) operation.getTableDetails();
			breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, tableMapping );
		}
	}

	private void bindDatabaseDirtinessCheckValues(
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		final var persister = entityPersister();
		final var identifierTable = persister.getIdentifierTableMapping();
		final var versionAttribute = persister.getVersionMapping().getVersionAttribute();
		final boolean[] updateability = persister.getPropertyUpdateability();
		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int i = 0; i < updateability.length; i++ ) {
			if ( updateability[i] && versionability[i] ) {
				final var attribute = persister.getAttributeMapping( i );
				if ( attribute != versionAttribute ) {
					((SingularAttributeMapping) attribute).decompose(
							values[i],
							0,
							jdbcValueBindings,
							identifierTable,
							(valueIndex, bindings, table, jdbcValue, selectable) -> {
								if ( !selectable.isFormula() && selectable.isUpdateable() ) {
									bindings.bindValue(
											jdbcValue,
											table.getTableName(),
											selectable.getSelectionExpression(),
											ParameterUsage.RESTRICT
									);
								}
							},
							session
					);
				}
			}
		}
	}

	private void decomposeAttributeForUpdate(
			@Nonnull Object entity,
			@Nonnull Object[] values,
			@Nonnull UpdateValuesAnalysisImpl valuesAnalysis,
			@Nonnull DirtinessChecker dirtinessChecker,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMapping tableMapping,
			int attributeIndex) {
		final var attributeMapping = entityPersister().getAttributeMappings().get( attributeIndex );
		if ( attributeMapping instanceof SingularAttributeMapping ) {
			final var attributeAnalysisRef = valuesAnalysis.attributeAnalyses.get( attributeIndex );
			if ( !attributeAnalysisRef.isSkipped() ) {
				final var attributeAnalysis = (IncludedAttributeAnalysis) attributeAnalysisRef;

				if ( attributeAnalysis.includeInSet() ) {
					// apply the new values
					if ( includeInSet( dirtinessChecker, attributeIndex, attributeMapping, attributeAnalysis ) ) {
						decomposeAttributeMapping(
								session,
								jdbcValueBindings,
								tableMapping,
								attributeMapping,
								values[attributeIndex],
								entity
						);
					}
				}

				// apply any optimistic locking
				if ( attributeAnalysis.includeInLocking() ) {
					optimisticLock( session, jdbcValueBindings, tableMapping, attributeAnalysis );
				}
			}
		}
	}

	private static void optimisticLock(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMapping tableMapping,
			@Nonnull IncludedAttributeAnalysis attributeAnalysis) {
		attributeAnalysis.columnLockingAnalyses.forEach( columnLockingAnalysis -> {
			if ( columnLockingAnalysis.getLockValue() != null ) {
				jdbcValueBindings.bindValue(
						columnLockingAnalysis.getLockValue(),
						tableMapping.getTableName(),
						columnLockingAnalysis.getReadExpression(),
						ParameterUsage.RESTRICT
				);
			}
		} );
	}

	private void decomposeAttributeMapping(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMapping tableMapping,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull Object values,
			@Nonnull Object entity) {
		final var generator = attributeMapping.getGenerator();
		final OnExecutionGenerator onExecutionGenerator;
		final String[] columnValues;
		final boolean[] columnInclusions;
		final boolean bindAllValues;
		if ( generator instanceof OnExecutionGenerator executionGenerator
				&& generator.generatedOnExecution( entity, session )
				&& generator.generatesOnUpdate() ) {
			onExecutionGenerator = executionGenerator;
			columnValues = onExecutionGenerator.getReferencedColumnValues( dialect(), EventType.UPDATE );
			columnInclusions = onExecutionGenerator.getColumnInclusions( dialect(), EventType.UPDATE );
			bindAllValues = onExecutionGenerator.writePropertyValue( EventType.UPDATE ) && columnValues == null;
		}
		else {
			onExecutionGenerator = null;
			columnValues = null;
			columnInclusions = null;
			bindAllValues = false;
		}

		attributeMapping.decompose(
				values,
				0,
				jdbcValueBindings,
				tableMapping,
				(valueIndex, bindings, table, jdbcValue, jdbcMapping) -> {
					if ( !jdbcMapping.isFormula()
							&& isColumnIncludedInSet( jdbcMapping )
							&& shouldBindValue( onExecutionGenerator, columnValues, columnInclusions, bindAllValues, valueIndex ) ) {
						bindings.bindValue(
								jdbcValue,
								table.getTableName(),
								jdbcMapping.getSelectionExpression(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private static boolean shouldBindValue(
			@Nullable OnExecutionGenerator onExecutionGenerator,
			@Nullable String[] columnValues,
			@Nullable boolean[] columnInclusions,
			boolean bindAllValues,
			int valueIndex) {
		if ( onExecutionGenerator == null ) {
			return true;
		}
		else if ( columnInclusions != null && !columnInclusions[valueIndex] ) {
			return false;
		}
		else {
			return bindAllValues
				|| columnValues != null && "?".equals( columnValues[valueIndex] );
		}
	}

	private boolean includeInSet(
			@Nullable DirtinessChecker dirtinessChecker,
			int attributeIndex,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull IncludedAttributeAnalysis attributeAnalysis) {
		if ( attributeAnalysis.isValueGeneratedInSqlNoWrite() ) {
			// we applied `#getDatabaseGeneratedReferencedColumnValue` earlier
			return false;
		}
		else if ( entityPersister().isVersioned()
				&& entityPersister().getVersionMapping().getVersionAttribute() == attributeMapping) {
			return true;
		}
		else if ( entityPersister().isDynamicUpdate() && dirtinessChecker != null ) {
			return attributeAnalysis.includeInSet()
				&& dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
		}
		else {
			return true;
		}
	}

	@Nullable
	protected GeneratedValues doDynamicUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object[] oldValues,
			@Nonnull AttributeInclusionChecker dirtinessChecker,
			@Nonnull UpdateValuesAnalysisImpl valuesAnalysis,
			@Nonnull SharedSessionContractImplementor session) {
		// Create the JDBC operation descriptors
		final var dynamicUpdateGroup = generateDynamicUpdateGroup(
				entity,
				id,
				rowId,
				oldValues,
				valuesAnalysis,
				session
		);

		// and then execute them
		final TableInclusionChecker inclusionChecker = tableMapping ->
				tableMapping.isOptional() && !valuesAnalysis.tablesWithNonNullValues.contains( tableMapping )
						? valuesAnalysis.dirtyAttributeIndexes == null || valuesAnalysis.dirtyAttributeIndexes.length > 0
						: valuesAnalysis.tablesNeedingUpdate.contains( tableMapping );
		checkTenantIdBeforeUpdate( id, dynamicUpdateGroup, inclusionChecker, session );

		final var mutationExecutor = executor( session, dynamicUpdateGroup, true );

		decomposeForUpdate(
				entity,
				id,
				rowId,
				values,
				valuesAnalysis,
				mutationExecutor,
				dynamicUpdateGroup,
				(attributeIndex, attribute) ->
						dirtinessChecker.include( attributeIndex, (SingularAttributeMapping) attribute )
								? AttributeAnalysis.DirtynessStatus.CONSIDER_LIKE_DIRTY
								: AttributeAnalysis.DirtynessStatus.NOT_DIRTY,
				session
		);
		// no snapshot when called from StatelessSession.update()
		bindPartitionColumnValueBindings( oldValues == null ? values : oldValues,
				session, mutationExecutor.getJdbcValueBindings() );
		bindTenantRestriction( session, mutationExecutor.getJdbcValueBindings(), dynamicUpdateGroup );

		try {
			return mutationExecutor.execute(
					entity,
					valuesAnalysis,
					inclusionChecker,
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Nonnull
	private MutationExecutor executor(
			@Nonnull SharedSessionContractImplementor session, @Nonnull MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	private void checkTenantIdBeforeUpdate(
			@Nonnull Object id, @Nonnull MutationOperationGroup group, @Nonnull TableInclusionChecker inclusionChecker,
			@Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		// Stateless mutations already check stored ownership before reaching this coordinator.
		if ( session instanceof SessionImplementor && TenantIdHelper.needsMultiTableUpdateCheck( persister, session ) ) {
			final String tenantTable = persister.physicalTableNameForMutation(
					TenantIdHelper.tenantIdAttribute( persister ).getSelectable( 0 ) );
			boolean updatesOtherTable = false;
			for ( int i = 0; i < group.getNumberOfOperations(); i++ ) {
				final var operation = group.getOperation( i );
				if ( inclusionChecker.include( operation.getTableDetails() ) ) {
					// The legacy executor executes and checks the identifier-table update first.
					if ( operation.getTableDetails().isIdentifierTable() && TenantIdHelper.checksTenantId( persister, operation ) ) {
						return;
					}
					updatesOtherTable |= !tenantTable.equals( operation.getTableDetails().getTableName() );
				}
			}
			if ( updatesOtherTable ) {
				session.getJdbcCoordinator().executeBatch();
				TenantIdHelper.checkStoredTenantOwnership( id, persister, session, THROW );
			}
		}
	}

	@Nonnull
	private MutationExecutor updateVersionExecutor(
			@Nonnull SharedSessionContractImplementor session, @Nonnull MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveUpdateVersionBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	@Nonnull
	private MutationExecutor updateVersionExecutor(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull MutationOperationGroup group,
			boolean dynamicUpdate,
			boolean batching) {
		return batching
				? updateVersionExecutor( session, group, dynamicUpdate )
				: mutationExecutorService.createExecutor( NoBatchKeyAccess.INSTANCE, group, session );

	}

	@Nonnull
	protected BatchKeyAccess resolveUpdateVersionBatchKeyAccess(boolean dynamicUpdate, @Nonnull SharedSessionContractImplementor session) {
		if ( !dynamicUpdate
				&& session.getTransactionCoordinator() != null
				&& session.getTransactionCoordinator().isTransactionActive() ) {
			return this::getVersionUpdateBatchkey;
		}
		else {
			return NoBatchKeyAccess.INSTANCE;
		}
	}

	//Used by Hibernate Reactive
	@Nullable
	protected BatchKey getVersionUpdateBatchkey(){
		return versionUpdateBatchkey;
	}

	@Nonnull
	protected MutationOperationGroup generateDynamicUpdateGroup(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] oldValues,
			@Nonnull UpdateValuesAnalysisImpl valuesAnalysis,
			@Nonnull SharedSessionContractImplementor session) {
		final var updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			final var tableReference = new MutatingTableReference( tableMapping );
			final var tableUpdateBuilder =
					valuesAnalysis.tablesNeedingUpdate.contains( tableReference.getTableMapping() )
							? createTableUpdateBuilder( tableMapping )
							// this table does not need updating
							: new TableUpdateBuilderSkipped( tableReference );
			updateGroupBuilder.addTableDetailsBuilder( tableUpdateBuilder );
		} );

		applyTableUpdateDetails(
				entity,
				rowId,
				updateGroupBuilder,
				oldValues,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).getDirtynessStatus(),
				session
		);

		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	@Nonnull
	private TableMutationBuilder<?> createTableUpdateBuilder(@Nonnull EntityTableMapping tableMapping) {
		final var delegate =
				tableMapping.isIdentifierTable()
						? entityPersister().getUpdateDelegate()
						: null;
		return delegate != null
				? delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), factory() )
				: newTableUpdateBuilder( tableMapping );
	}

	@Nonnull
	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(@Nonnull EntityTableMapping tableMapping) {
		return new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );
	}

	private void applyTableUpdateDetails(
			@Nullable Object entity,
			@Nullable Object rowId,
			@Nonnull MutationGroupBuilder updateGroupBuilder,
			@Nullable Object[] oldValues,
			@Nonnull UpdateValuesAnalysisImpl updateValuesAnalysis,
			@Nonnull DirtinessChecker dirtinessChecker,
			@Nullable SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		final var versionMapping = persister.getVersionMapping();
		final var attributeMappings = persister.getAttributeMappings();
		final boolean[] versionability = persister.getPropertyVersionability();
		final var optimisticLockStyle = persister.optimisticLockStyle();

		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableUpdateBuilder = (TableUpdateBuilder<?>) builder;
			if ( updateValuesAnalysis.databaseDirtinessCheck && tableMapping.isIdentifierTable() ) {
				// MySQL evaluates assignments from left to right, so the version CASE must
				// read the columns before any of their new values are assigned.
				addDatabaseDirtinessCheckedVersionAssignment( versionMapping, tableUpdateBuilder );
			}

			for ( final int attributeIndex : tableMapping.getAttributeIndexes() ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				final var attributeAnalysis = updateValuesAnalysis.attributeAnalyses.get( attributeIndex );

				if ( attributeAnalysis.includeInSet() ) {
					assert updateValuesAnalysis.tablesNeedingUpdate.contains( tableMapping )
						|| updateValuesAnalysis.tablesNeedingDynamicUpdate.contains( tableMapping );
					applyAttributeUpdateDetails(
							entity,
							updateGroupBuilder,
							dirtinessChecker,
							versionMapping,
							updateValuesAnalysis.databaseDirtinessCheck,
							attributeIndex,
							attributeMapping,
							tableUpdateBuilder,
							session
					);
				}

				if ( attributeAnalysis.includeInLocking() ) {
					final boolean includeRestriction = includeInRestriction(
							oldValues,
							dirtinessChecker,
							versionMapping,
							versionability,
							optimisticLockStyle,
							attributeIndex,
							attributeMapping,
							attributeAnalysis
					);

					if ( includeRestriction ) {
						applyAttributeLockingDetails(
								oldValues,
								session,
								attributeIndex,
								attributeMapping,
								(TableUpdateBuilder<?>) builder
						);
					}
				}
			}
		} );

		updateGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableUpdateBuilder = (TableUpdateBuilder<?>) builder;
			applyKeyRestriction( rowId, persister, tableUpdateBuilder, tableMapping );
			applyPartitionKeyRestriction( tableUpdateBuilder );
			applyTenantRestriction( tableUpdateBuilder );
		} );
	}

	private static void applyAttributeLockingDetails(
			@Nullable Object[] oldValues,
			@Nullable SharedSessionContractImplementor session,
			int attributeIndex,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull TableUpdateBuilder<?> tableUpdateBuilder) {
		if ( oldValues == null ) {
			tableUpdateBuilder.addOptimisticLockRestrictions( attributeMapping );
		}
		else if ( tableUpdateBuilder.getOptimisticLockBindings() != null ) {
			attributeMapping.decompose(
					oldValues[attributeIndex],
					tableUpdateBuilder.getOptimisticLockBindings(),
					session
			);
		}
	}

	private static boolean includeInRestriction(
			@Nullable Object[] oldValues,
			@Nullable DirtinessChecker dirtinessChecker,
			@Nullable EntityVersionMapping versionMapping,
			@Nonnull boolean[] versionability,
			@Nonnull OptimisticLockStyle optimisticLockStyle,
			int attributeIndex,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull AttributeAnalysis attributeAnalysis) {

		if ( optimisticLockStyle == OptimisticLockStyle.VERSION
				&& versionMapping != null
				&& attributeMapping == versionMapping.getVersionAttribute() ) {
			return true;
		}
		else if ( oldValues == null ) {
			return false;
		}
		else if ( optimisticLockStyle == OptimisticLockStyle.ALL ) {
			return versionability[attributeIndex];
		}
		else if ( optimisticLockStyle == DIRTY ) {
			if ( dirtinessChecker == null ) {
				// this should indicate creation of the "static" update group.
				return false;
			}
			else {
				return versionability[attributeIndex]
					&& attributeAnalysis.includeInLocking()
					&& dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
			}
		}
		else {
			return false;
		}
	}

	private void applyAttributeUpdateDetails(
			@Nullable Object entity,
			@Nonnull MutationGroupBuilder updateGroupBuilder,
			@Nullable DirtinessChecker dirtinessChecker,
			@Nullable EntityVersionMapping versionMapping,
			boolean databaseDirtinessCheck,
			int attributeIndex,
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull TableUpdateBuilder<?> tableUpdateBuilder,
			@Nullable SharedSessionContractImplementor session) {
		final var generator = attributeMapping.getGenerator();
		if ( generator instanceof OnExecutionGenerator onExecutionGenerator
				&& hasValueGenerationOnExecution( entity, session, onExecutionGenerator, EventType.UPDATE ) ) {
			handleValueGeneration( attributeMapping, updateGroupBuilder, onExecutionGenerator, EventType.UPDATE );
		}
		else if ( versionMapping != null
				&& versionMapping.getVersionAttribute() == attributeMapping) {
			if ( !databaseDirtinessCheck ) {
				tableUpdateBuilder.addValueColumn( versionMapping.getVersionAttribute() );
			}
		}
		else {
			final boolean includeInSet = !entityPersister().isDynamicUpdate()
					|| dirtinessChecker == null
					|| dirtinessChecker.isDirty( attributeIndex, attributeMapping ).isDirty();
			if ( includeInSet ) {
				forEachUpdatable( attributeMapping, tableUpdateBuilder );
			}
		}
	}

	private void addDatabaseDirtinessCheckedVersionAssignment(
			@Nonnull EntityVersionMapping versionMapping,
			@Nonnull TableUpdateBuilder<?> tableUpdateBuilder) {
		final var mutatingTable = tableUpdateBuilder.getMutatingTable();
		final var versionAttribute = versionMapping.getVersionAttribute();
		final var versionColumn = new ColumnReference( mutatingTable, versionAttribute );
		final var changedPredicate = new Junction( DISJUNCTION );
		final List<ColumnValueParameter> parameters = new ArrayList<>();

		final var persister = entityPersister();
		final boolean[] updateability = persister.getPropertyUpdateability();
		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int i = 0; i < updateability.length; i++ ) {
			if ( updateability[i] && versionability[i] ) {
				final var attribute = persister.getAttributeMapping( i );
				if ( attribute != versionAttribute ) {
					final var singularAttribute = (SingularAttributeMapping) attribute;
					for ( int selectableIndex = 0; selectableIndex < singularAttribute.getJdbcTypeCount(); selectableIndex++ ) {
						final var selectable = singularAttribute.getSelectable( selectableIndex );
						if ( !selectable.isFormula() && selectable.isUpdateable() ) {
							final var column = new ColumnReference( mutatingTable, selectable );
							final var parameter = new ColumnValueParameter( column, ParameterUsage.RESTRICT );
							parameters.add( parameter );
							changedPredicate.add( new ComparisonPredicate( column, DISTINCT_FROM, parameter ) );
						}
					}
				}
			}
		}
		if ( changedPredicate.isEmpty() ) {
			// Keep a parameterized CASE shape even when the entity has no updateable
			// versionable columns; a column is never distinct from itself.
			changedPredicate.add( new ComparisonPredicate( versionColumn, DISTINCT_FROM, versionColumn ) );
		}

		final var nextVersion = new ColumnValueParameter( versionColumn, ParameterUsage.SET );
		parameters.add( nextVersion );
		final var versionExpression = new CaseSearchedExpression( versionMapping );
		versionExpression.when( changedPredicate, nextVersion );
		versionExpression.otherwise( versionColumn );
		tableUpdateBuilder.addColumnAssignment( new ColumnValueBinding( versionColumn,
				new ColumnWriteFragment( parameters, versionAttribute, versionExpression ) ) );
	}

	protected void forEachUpdatable(@Nonnull AttributeMapping attributeMapping, @Nonnull TableUpdateBuilder<?> tableUpdateBuilder) {
		attributeMapping.forEachUpdatable( tableUpdateBuilder );
	}

	/**
	 * Contains the aggregated analysis of the update values to determine
	 * what SQL UPDATE statement(s) should be used to update the entity
	 * and to drive parameter binding
	 */
	protected class UpdateValuesAnalysisImpl implements UpdateValuesAnalysis {
		@Nullable
		private final Object[] values;
		@Nullable
		private final int[] dirtyAttributeIndexes;
		private final AttributeInclusionChecker dirtinessChecker;
		private final boolean databaseDirtinessCheck;

		private final TableSet tablesNeedingUpdate = new TableSet();
		private final TableSet tablesNeedingDynamicUpdate = new TableSet();
		private final TableSet tablesWithNonNullValues = new TableSet();
		private final TableSet tablesWithPreviousNonNullValues = new TableSet();

		private final List<AttributeAnalysis> attributeAnalyses = new ArrayList<>();

		// transient values as we perform the analysis
		@Nullable
		private AttributeAnalysisImplementor currentAttributeAnalysis;
		private boolean dirtyChecked = false;
		private boolean nullChecked = false;

		public UpdateValuesAnalysisImpl(
				@Nullable Object[] values,
				@Nullable Object[] oldValues,
				@Nullable int[] dirtyAttributeIndexes,
				@Nonnull AttributeInclusionChecker dirtinessChecker,
				@Nullable Object rowId,
				boolean forceDynamicUpdate,
				boolean databaseDirtinessCheck) {
			this.values = values;
			this.dirtyAttributeIndexes = dirtyAttributeIndexes;
			this.dirtinessChecker = dirtinessChecker;
			this.databaseDirtinessCheck = databaseDirtinessCheck;

			entityPersister().forEachMutableTable( (tableMapping) -> {
				if ( values == null ) {
					tablesWithNonNullValues.add( tableMapping );
				}
				else {
					for ( final int attributeIndex : tableMapping.getAttributeIndexes() ) {
						if ( values[attributeIndex] != null ) {
							tablesWithNonNullValues.add( tableMapping );
							break;
						}
					}
				}

				if ( dirtyAttributeIndexes == null && tableMapping.hasColumns() ) {
					tablesNeedingUpdate.add( tableMapping );
				}

				if ( oldValues == null ) {
					tablesWithPreviousNonNullValues.add( tableMapping );
				}
				else {
					for ( final int attributeIndex : tableMapping.getAttributeIndexes() ) {
						if ( oldValues[attributeIndex] != null ) {
							tablesWithPreviousNonNullValues.add( tableMapping );
							break;
						}
					}
				}

				if ( tableMapping.getUpdateDetails().getCustomSql() == null ) {
					// we should only dynamically update tables w/o custom update sql
					if ( forceDynamicUpdate ) {
						tablesNeedingDynamicUpdate.add( tableMapping );
					}
					else if ( dirtyAttributeIndexes != null ) {
						if ( entityPersister().isDynamicUpdate()
								|| entityPersister().optimisticLockStyle() == DIRTY ) {
							tablesNeedingDynamicUpdate.add( tableMapping );
						}
						else if ( rowId == null && needsRowId( entityPersister(), tableMapping ) ) {
							tablesNeedingDynamicUpdate.add( tableMapping );
						}
					}
				}
			} );
		}

		@Nullable
		@Override
		public Object[] getValues() {
			return values;
		}

		@Nonnull
		@Override
		public TableSet getTablesNeedingUpdate() {
			return tablesNeedingUpdate;
		}

		@Nonnull
		@Override
		public TableSet getTablesWithNonNullValues() {
			return tablesWithNonNullValues;
		}

		@Nonnull
		@Override
		public TableSet getTablesWithPreviousNonNullValues() {
			return tablesWithPreviousNonNullValues;
		}

		@Nonnull
		@Override
		public List<AttributeAnalysis> getAttributeAnalyses() {
			return attributeAnalyses;
		}

		/**
		 * Basically, can the ({@linkplain UpdateCoordinatorStandard#staticUpdateGroup static update group}
		 * be used or is a dynamic update needed.
		 */
		public boolean needsDynamicUpdate() {
			return !tablesNeedingDynamicUpdate.isEmpty();
		}

		@Nonnull
		public TableSet getTablesNeedingDynamicUpdate() {
			return tablesNeedingDynamicUpdate;
		}

		/**
		 * Callback at start of processing an attribute
		 */
		public void startingAttribute(@Nonnull AttributeMapping attribute) {
			if ( attribute.getJdbcTypeCount() < 1
					|| !( attribute instanceof SingularAttributeMapping singularAttributeMapping ) ) {
				currentAttributeAnalysis = new SkippedAttributeAnalysis( attribute );
			}
			else {
				currentAttributeAnalysis = new IncludedAttributeAnalysis( singularAttributeMapping );
				if ( dirtyAttributeIndexes == null
						|| contains( dirtyAttributeIndexes, attribute.getStateArrayPosition() ) ) {
					currentAttributeAnalysis.markDirty( dirtyAttributeIndexes != null );
				}
			}

			attributeAnalyses.add( currentAttributeAnalysis );
		}

		public void finishedAttribute(@Nonnull AttributeMapping attribute) {
			assert currentAttributeAnalysis.getAttribute() == attribute;
			currentAttributeAnalysis = null;
			dirtyChecked = false;
			nullChecked = false;
		}

		/**
		 * Callback to register the setting of a column value
		 */
		public void registerColumnSet(@Nonnull EntityTableMapping table, @Nonnull String readExpression, @Nullable String writeExpression) {
			final var includedAttributeAnalysis = (IncludedAttributeAnalysis) castNonNull( currentAttributeAnalysis );
			includedAttributeAnalysis.columnValueAnalyses.add( new ColumnSetAnalysis( readExpression, writeExpression ) );

			if ( !dirtyChecked ) {
				final var attribute = includedAttributeAnalysis.attribute;
				if ( dirtinessChecker.include( attribute.getStateArrayPosition(), attribute ) ) {
					tablesNeedingUpdate.add( table );
				}

				dirtyChecked = true;
			}

			if ( values != null && !nullChecked ) {
				final int attributePosition = includedAttributeAnalysis.getAttribute().getStateArrayPosition();
				if ( values[attributePosition] != null ) {
					tablesWithNonNullValues.add( table );
				}
				nullChecked = true;
			}
		}

		public void registerColumnOptLock(@Nonnull EntityTableMapping table, @Nonnull String readExpression, @Nullable Object lockValue) {
			final var attributeAnalysis = (IncludedAttributeAnalysis) castNonNull( currentAttributeAnalysis );
			attributeAnalysis.columnLockingAnalyses.add( new ColumnLockingAnalysis( readExpression, lockValue ) );

			if ( dirtyAttributeIndexes != null && lockValue == null ) {
				// we need to use `IS NULL` as opposed to `= ?` w/ NULL
				tablesNeedingDynamicUpdate.add( table );
			}
		}

		public void registerValueGeneratedInSqlNoWrite() {
			final var attributeAnalysis = (IncludedAttributeAnalysis) castNonNull( currentAttributeAnalysis );
			attributeAnalysis.setValueGeneratedInSqlNoWrite( true );
		}
	}

	/**
	 * Local extension to AttributeAnalysis
	 */
	private interface AttributeAnalysisImplementor extends AttributeAnalysis {
		/**
		 * @param asCertain set to true when we're sure, false when we merely need to treat the attribute
		 * as dirty but couldn't actually run the comparison.
		 * Once it's marked at least once "with certainty", there is no option to revert to a lower state.
		 */
		void markDirty(boolean asCertain);
	}

	/**
	 * Local AttributeAnalysis implementation for use when the attribute is
	 * to be completely skipped.  Avoids having to define the collections
	 * needed to fully implement AttributeAnalysis.
	 *
	 * @see IncludedAttributeAnalysis
	 */
	private static class SkippedAttributeAnalysis implements AttributeAnalysisImplementor {
		private final AttributeMapping attributeMapping;

		public SkippedAttributeAnalysis(@Nonnull AttributeMapping attributeMapping) {
			this.attributeMapping = attributeMapping;
		}

		@Nonnull
		@Override
		public AttributeMapping getAttribute() {
			return attributeMapping;
		}

		@Override
		public boolean includeInSet() {
			return false;
		}

		@Override
		public boolean includeInLocking() {
			return false;
		}

		@Nonnull
		@Override
		public DirtynessStatus getDirtynessStatus() {
			return DirtynessStatus.NOT_DIRTY;
		}

		@Override
		public void markDirty(boolean certainty) {
		}

		@Nonnull
		@Override
		public String toString() {
			return String.format(
					Locale.ROOT,
					"SkippedAttributeAnalysis(`%s`)",
					attributeMapping.getNavigableRole().getFullPath()
			);
		}
	}

	/**
	 * Local AttributeAnalysis implementation
	 */
	private static class IncludedAttributeAnalysis implements AttributeAnalysisImplementor {
		private final SingularAttributeMapping attribute;

		private final List<ColumnSetAnalysis> columnValueAnalyses;
		private final List<ColumnLockingAnalysis> columnLockingAnalyses;

		private DirtynessStatus dirty = DirtynessStatus.NOT_DIRTY;
		private boolean valueGeneratedInSqlNoWrite;

		public IncludedAttributeAnalysis(@Nonnull SingularAttributeMapping attribute) {
			this.attribute = attribute;

			this.columnValueAnalyses = arrayList( attribute.getJdbcTypeCount() );
			this.columnLockingAnalyses = arrayList( attribute.getJdbcTypeCount() );
		}

		@Nonnull
		@Override
		public SingularAttributeMapping getAttribute() {
			return attribute;
		}

		@Override
		public boolean includeInSet() {
			return !columnValueAnalyses.isEmpty();
		}

		@Override
		public boolean includeInLocking() {
			return !columnLockingAnalyses.isEmpty();
		}

		@Nonnull
		@Override
		public DirtynessStatus getDirtynessStatus() {
			return dirty;
		}

		@Override
		public void markDirty(boolean certain) {
			if ( certain ) {
				this.dirty = DirtynessStatus.DIRTY;
			}
			else if ( this.dirty == DirtynessStatus.NOT_DIRTY ) {
				this.dirty = DirtynessStatus.CONSIDER_LIKE_DIRTY;
			}
		}

		public boolean isValueGeneratedInSqlNoWrite() {
			return valueGeneratedInSqlNoWrite;
		}

		public void setValueGeneratedInSqlNoWrite(boolean valueGeneratedInSqlNoWrite) {
			this.valueGeneratedInSqlNoWrite = valueGeneratedInSqlNoWrite;
		}

		@Nonnull
		@Override
		public String toString() {
			return String.format(
					Locale.ROOT,
					"IncludedAttributeAnalysis(`%s`)",
					attribute.getNavigableRole().getFullPath()
			);
		}
	}

	private static class ColumnSetAnalysis {
		private final String readExpression;
		@Nullable
		private final String writeExpression;

		public ColumnSetAnalysis(@Nonnull String readExpression, @Nullable String writeExpression) {
			this.readExpression = readExpression;
			this.writeExpression = writeExpression;
		}

		@Nonnull
		@SuppressWarnings("unused")
		public String getReadExpression() {
			return readExpression;
		}

		@Nullable
		@SuppressWarnings("unused")
		public String getWriteExpression() {
			return writeExpression;
		}
	}

	private static class ColumnLockingAnalysis {
		private final String readExpression;
		@Nullable
		private final Object lockValue;

		public ColumnLockingAnalysis(@Nonnull String readExpression, @Nullable Object lockValue) {
			assert readExpression != null;
			assert !readExpression.equals( "?" );

			this.readExpression = readExpression;
			this.lockValue = lockValue;
		}

		@Nonnull
		public String getReadExpression() {
			return readExpression;
		}

		@Nullable
		public Object getLockValue() {
			return lockValue;
		}
	}

	@Nonnull
	protected MutationOperationGroup buildStaticUpdateGroup() {
		final var persister = entityPersister();
		final var valuesAnalysis = analyzeUpdateValues(
				null,
				null,
				null,
				null,
				null,
				(index, attribute) -> includeInStaticUpdate( index, attribute, persister.getPropertyUpdateability() ),
				(index,attribute) ->
						switch ( persister.optimisticLockStyle() ) {
							case ALL -> true;
							case VERSION -> {
								final var versionMapping = persister.getVersionMapping();
								yield versionMapping != null && attribute == versionMapping.getVersionAttribute();
							}
							default -> false;
						},
				(index,attribute) -> true,
				false,
				"", // pass anything here to generate the row id restriction if possible
				false,
				false,
				null
		);

		final var updateGroupBuilder = new MutationGroupBuilder( MutationType.UPDATE, persister );

		persister.forEachMutableTable( (tableMapping) -> {
			// NOTE: TableUpdateBuilderStandard handles custom SQL update mappings
			updateGroupBuilder.addTableDetailsBuilder( createTableUpdateBuilder( tableMapping ) );
		} );

		// next, iterate each attribute and build the SET and WHERE clauses
		applyTableUpdateDetails(
				null,
				// row-id
				"", // pass anything here to generate the row id restriction if possible
				// the "collector"
				updateGroupBuilder,
				// oldValues
				null,
				valuesAnalysis,
				(position, attribute) -> valuesAnalysis.getAttributeAnalyses().get( position ).getDirtynessStatus(),
				// session
				null
		);

		// build the mutation-group (SQL AST) and convert it into a jdbc-operations (SQL String, etc) group
		return createOperationGroup( valuesAnalysis, updateGroupBuilder.buildMutationGroup() );
	}

	protected boolean includeInStaticUpdate(
			int index,
			@Nonnull AttributeMapping attribute,
			@Nonnull boolean[] propertyUpdateability) {
		return isValueGenerationOnUpdateInSql( attribute.getGenerator(), dialect() )
			|| propertyUpdateability[index];
	}

	@Nullable
	private MutationOperationGroup buildVersionUpdateGroup() {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping == null ) {
			return null;
		}
		else {
			final var identifierTableMapping = entityPersister().getIdentifierTableMapping();
			final AbstractTableUpdateBuilder<JdbcMutationOperation> updateBuilder =
					identifierTableMapping.getUpdateDetails().getCustomSql() == null
							? newTableUpdateBuilder( identifierTableMapping )
							// A custom update expects all mapped values, not just the version.
							: new TableUpdateBuilderStandard<>( entityPersister(),
									new MutatingTableReference( identifierTableMapping ),
									new TableMapping.MutationDetails(
											MutationType.UPDATE, new Expectation.RowCount(), null, false ),
									null, factory() );

			updateBuilder.setSqlComment( "forced version increment for " + entityPersister().getRolePath() );

			updateBuilder.addValueColumn( versionMapping );

			updateBuilder.addKeyRestrictionsLeniently( identifierTableMapping.getKeyMapping() );

			applyVersionOptimisticLocking( updateBuilder );
			applyPartitionKeyRestriction( updateBuilder );
			applyTenantRestriction( updateBuilder );

			//noinspection resource
			final var jdbcMutation = factory()
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( factory(), updateBuilder.buildMutation() ) )
					.translate( null, MutationQueryOptions.INSTANCE );

			return MutationOperationGroupFactory.singleOperation( MutationType.UPDATE, entityPersister(), jdbcMutation );
		}
	}

	@FunctionalInterface
	protected interface DirtinessChecker {
		@Nonnull
		AttributeAnalysis.DirtynessStatus isDirty(int position, @Nonnull AttributeMapping attribute);
	}

	public boolean hasLazyDirtyFields(@Nonnull EntityPersister persister,  @Nonnull int[] dirtyFields) {
		final var propertyLaziness = persister.getPropertyLaziness();
		for ( int dirtyField : dirtyFields ) {
			if ( propertyLaziness[dirtyField] ) {
				return true;
			}
		}
		return false;
	}

	@Nonnull
	public EntityTableMapping physicalTableMappingForMutation(
			@Nonnull EntityPersister persister, @Nonnull SelectableMapping selectableMapping) {
		final String tableNameForMutation = persister.physicalTableNameForMutation( selectableMapping );
		for ( var tableMapping : persister.getTableMappings() ) {
			if ( tableNameForMutation.equals( tableMapping.getTableName() ) ) {
				return tableMapping;
			}
		}

		throw new IllegalArgumentException( "Unable to resolve TableMapping for selectable - " + selectableMapping );
	}

	@Nonnull
	@Override
	public String toString() {
		return "UpdateCoordinatorStandard(" + entityPersister().getEntityName() + ")";
	}
}
